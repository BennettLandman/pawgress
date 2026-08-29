package com.balandman.liftlog.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * All app state lives in one small JSON file in the app's private storage.
 *
 * A gym log is a few thousand rows over years, so a database would be more
 * machinery than the problem deserves — and the whole file is cheap to rewrite
 * on every change.
 *
 * The file holds a map of profiles keyed by Google account, plus which one is
 * active. Everything the UI sees is a view onto the active profile.
 */
class LiftRepository(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _profiles = MutableStateFlow<Map<String, Profile>>(emptyMap())
    private val _activeKey = MutableStateFlow(Profile.LOCAL_KEY)

    init {
        load()
    }

    // ------------------------------------------------------------ derived views

    val active: StateFlow<Profile> =
        combine(_profiles, _activeKey) { profiles, key -> profiles[key] ?: blank(key) }
            .stateIn(scope, SharingStarted.Eagerly, current())

    val machines: StateFlow<List<Machine>> =
        active.map { it.machines }.stateIn(scope, SharingStarted.Eagerly, current().machines)

    val log: StateFlow<List<LogEntry>> =
        active.map { it.log }.stateIn(scope, SharingStarted.Eagerly, current().log)

    val sync: StateFlow<SyncState> =
        combine(active, _profiles) { profile, profiles -> profile.toSyncState(profiles.size) }
            .stateIn(scope, SharingStarted.Eagerly, current().toSyncState(_profiles.value.size))

    // ------------------------------------------------------------ direct reads
    // Callers that act on a decision (the sync manager, mostly) read these rather
    // than the flows above, because a derived flow updates a beat later and a
    // sync must never act on the profile that was active a moment ago.

    /** The active profile, always current. */
    fun current(): Profile = _profiles.value[_activeKey.value] ?: blank(_activeKey.value)

    fun visibleMachines(): List<Machine> =
        current().machines.filter { it.visible }.sortedBy { it.sortOrder }

    fun unsyncedEntries(): List<LogEntry> =
        current().log.filter { !it.synced }.sortedBy { it.loggedAt }

    fun pendingDeletions(): List<String> = current().pendingDeletions

    fun machine(id: String): Machine? = current().machines.firstOrNull { it.id == id }

    private fun previousWeightBefore(machineId: String, excludingEntryId: String): LogEntry? =
        current().log
            .filter { it.machineId == machineId && it.id != excludingEntryId }
            .maxByOrNull { it.loggedAt }

    // --------------------------------------------------------------- profiles

    /**
     * Point the app at [email]'s profile, creating it if this account is new here.
     *
     * The very first account to sign in adopts whatever was set up while signed
     * out, so nobody loses the machines they configured before connecting. Every
     * account after that starts from the default catalog with an empty history.
     */
    fun activateAccount(email: String): Profile {
        val profiles = _profiles.value
        val existing = profiles[email]

        if (existing != null) {
            val reconnected = existing.copy(connected = true)
            _profiles.value = profiles + (email to reconnected)
            _activeKey.value = email
            persist()
            return reconnected
        }

        val activeKey = _activeKey.value
        val activeProfile = profiles[activeKey]
        val onlyLocalExists = profiles.keys.all { it == Profile.LOCAL_KEY }
        val adopt = activeKey == Profile.LOCAL_KEY && activeProfile != null && onlyLocalExists

        val adopted = if (adopt && activeProfile != null) {
            activeProfile.copy(key = email, accountEmail = email, connected = true)
        } else {
            blank(email).copy(accountEmail = email, connected = true)
        }

        _profiles.value =
            (if (adopt) profiles - Profile.LOCAL_KEY else profiles) + (email to adopted)
        _activeKey.value = email
        persist()
        return adopted
    }

    /** Stops syncing but keeps the profile, its grid and its history intact. */
    fun disconnect() {
        mutateActive { profile ->
            profile.copy(
                connected = false,
                spreadsheetId = null,
                spreadsheetUrl = null,
                lastSyncAt = null,
                lastError = null,
                pendingDeletions = emptyList(),
                log = profile.log.map { it.copy(synced = false) },
            )
        }
    }

    // ------------------------------------------------------------------ write

    /** Record a lift. Returns the new entry so the caller can kick off a sync. */
    fun logLift(machineId: String, weight: Int): LogEntry? {
        val machine = machine(machineId) ?: return null
        val clamped = Weights.clamp(weight)
        val now = System.currentTimeMillis()

        // One entry per machine per gym day: re-logging replaces today's entry
        // rather than stacking up duplicates.
        val existingToday = current().log.firstOrNull {
            it.machineId == machineId && GymDay.isToday(it.loggedAt)
        }

        val entry = LogEntry(
            id = existingToday?.id ?: UUID.randomUUID().toString(),
            machineId = machineId,
            machineName = machine.name,
            weight = clamped,
            loggedAt = now,
            synced = false,
        )

        mutateActive { profile ->
            // A stale row already in the sheet gets queued for removal, so the
            // corrected value does not end up sitting next to the wrong one.
            val deletions =
                if (existingToday != null && existingToday.synced) {
                    profile.pendingDeletions + existingToday.id
                } else {
                    profile.pendingDeletions
                }

            profile.copy(
                log = profile.log.filterNot { it.id == entry.id } + entry,
                machines = profile.machines.map {
                    if (it.id == machineId) it.copy(lastWeight = clamped, lastLoggedAt = now) else it
                },
                pendingDeletions = deletions,
            )
        }
        return entry
    }

    /** Undo today's entry for a machine, restoring the previously shown weight. */
    fun undoToday(machineId: String) {
        val entry = current().log.firstOrNull {
            it.machineId == machineId && GymDay.isToday(it.loggedAt)
        } ?: return

        val restored = previousWeightBefore(machineId, entry.id)

        mutateActive { profile ->
            profile.copy(
                log = profile.log.filterNot { it.id == entry.id },
                machines = profile.machines.map {
                    if (it.id == machineId) {
                        it.copy(lastWeight = restored?.weight, lastLoggedAt = restored?.loggedAt)
                    } else {
                        it
                    }
                },
                pendingDeletions =
                    if (entry.synced) profile.pendingDeletions + entry.id
                    else profile.pendingDeletions,
            )
        }
    }

    fun setVisible(machineId: String, visible: Boolean) = mutateActive { profile ->
        profile.copy(
            machines = profile.machines.map {
                if (it.id == machineId) it.copy(visible = visible) else it
            }
        )
    }

    fun setAllVisible(visible: Boolean) = mutateActive { profile ->
        profile.copy(machines = profile.machines.map { it.copy(visible = visible) })
    }

    fun rename(machineId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        mutateActive { profile ->
            profile.copy(
                machines = profile.machines.map {
                    if (it.id == machineId) it.copy(name = trimmed) else it
                }
            )
        }
    }

    fun setIcon(machineId: String, iconKey: String) = mutateActive { profile ->
        profile.copy(
            machines = profile.machines.map {
                if (it.id == machineId) it.copy(iconKey = iconKey) else it
            }
        )
    }

    fun addCustomMachine(name: String, iconKey: String, group: MachineGroup): Machine? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val machine = Machine(
            id = "custom_" + UUID.randomUUID().toString().take(8),
            name = trimmed,
            iconKey = iconKey,
            group = group,
            visible = true,
            custom = true,
            sortOrder = (current().machines.maxOfOrNull { it.sortOrder } ?: -1) + 1,
        )
        mutateActive { profile -> profile.copy(machines = profile.machines + machine) }
        return machine
    }

    /** Only custom machines can be deleted; built-ins are hidden instead. */
    fun deleteCustomMachine(machineId: String) {
        if (machine(machineId)?.custom != true) return
        mutateActive { profile ->
            profile.copy(machines = profile.machines.filterNot { it.id == machineId })
        }
    }

    fun recordSyncSuccess(spreadsheetId: String, spreadsheetUrl: String) = mutateActive { profile ->
        profile.copy(
            spreadsheetId = spreadsheetId,
            spreadsheetUrl = spreadsheetUrl,
            lastSyncAt = System.currentTimeMillis(),
            lastError = null,
        )
    }

    fun recordSyncError(message: String) = mutateActive { profile ->
        profile.copy(lastError = message)
    }

    fun markSynced(entryIds: Collection<String>) {
        if (entryIds.isEmpty()) return
        val ids = entryIds.toSet()
        mutateActive { profile ->
            profile.copy(log = profile.log.map { if (it.id in ids) it.copy(synced = true) else it })
        }
    }

    fun clearPendingDeletions(entryIds: Collection<String>) {
        if (entryIds.isEmpty()) return
        val ids = entryIds.toSet()
        mutateActive { profile ->
            profile.copy(pendingDeletions = profile.pendingDeletions.filterNot { it in ids })
        }
    }

    /**
     * Used when the spreadsheet had to be recreated: nothing previously pushed
     * exists any more, so the whole history gets rebuilt on the next sync.
     */
    fun markAllUnsynced() = mutateActive { profile ->
        profile.copy(
            log = profile.log.map { it.copy(synced = false) },
            pendingDeletions = emptyList(),
        )
    }

    // ------------------------------------------------------------ persistence

    private fun mutateActive(transform: (Profile) -> Profile) {
        val key = _activeKey.value
        val existing = _profiles.value[key] ?: blank(key)
        _profiles.value = _profiles.value + (key to transform(existing))
        persist()
    }

    private fun blank(key: String) = Profile(
        key = key,
        accountEmail = if (key == Profile.LOCAL_KEY) null else key,
        machines = MachineCatalog.defaults(),
    )

    private fun persist() {
        val snapshot = toJson().toString()
        scope.launch {
            writeMutex.withLock {
                try {
                    val tmp = File(file.parentFile, "$FILE_NAME.tmp")
                    tmp.writeText(snapshot)
                    if (!tmp.renameTo(file)) {
                        // renameTo can fail if the destination exists on some devices
                        file.writeText(snapshot)
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Could not save app state", e)
                }
            }
        }
    }

    private fun load() {
        if (!file.exists()) {
            seedLocal()
            return
        }
        try {
            val root = JSONObject(file.readText())

            // A version 1 file held a single profile at the root.
            if (!root.has("profiles")) {
                val legacy = root.toLegacyProfile()
                _profiles.value = mapOf(legacy.key to legacy)
                _activeKey.value = legacy.key
                return
            }

            val stored = root.optJSONObject("profiles")
            val loaded = mutableMapOf<String, Profile>()
            stored?.keys()?.forEach { key ->
                stored.optJSONObject(key)?.toProfile(key)?.let { loaded[key] = it }
            }

            if (loaded.isEmpty()) {
                seedLocal()
                return
            }

            _profiles.value = loaded
            val wanted = root.optString("activeKey")
            _activeKey.value = if (wanted in loaded.keys) wanted else loaded.keys.first()
        } catch (e: Exception) {
            Log.e(TAG, "Saved state was unreadable; starting from the default catalog", e)
            seedLocal()
        }
    }

    private fun seedLocal() {
        val local = blank(Profile.LOCAL_KEY)
        _profiles.value = mapOf(Profile.LOCAL_KEY to local)
        _activeKey.value = Profile.LOCAL_KEY
    }

    private fun toJson(): JSONObject {
        val profiles = JSONObject()
        _profiles.value.forEach { (key, profile) -> profiles.put(key, profile.toJson()) }
        return JSONObject().apply {
            put("version", 2)
            put("activeKey", _activeKey.value)
            put("profiles", profiles)
        }
    }

    companion object {
        private const val TAG = "LiftRepository"
        private const val FILE_NAME = "liftlog.json"
    }
}

// ----------------------------------------------------------- JSON conversions

private fun Profile.toSyncState(profileCount: Int) = SyncState(
    accountEmail = if (connected) accountEmail else null,
    profileEmail = accountEmail,
    profileCount = profileCount,
    spreadsheetId = spreadsheetId,
    spreadsheetUrl = spreadsheetUrl,
    lastSyncAt = lastSyncAt,
    lastError = lastError,
)

private fun Profile.toJson(): JSONObject = JSONObject().apply {
    put("key", key)
    if (accountEmail != null) put("accountEmail", accountEmail)
    put("connected", connected)
    if (spreadsheetId != null) put("spreadsheetId", spreadsheetId)
    if (spreadsheetUrl != null) put("spreadsheetUrl", spreadsheetUrl)
    if (lastSyncAt != null) put("lastSyncAt", lastSyncAt)
    if (lastError != null) put("lastError", lastError)

    val machinesArray = JSONArray()
    machines.forEach { machinesArray.put(it.toJson()) }
    put("machines", machinesArray)

    val logArray = JSONArray()
    log.forEach { logArray.put(it.toJson()) }
    put("log", logArray)

    val deletions = JSONArray()
    pendingDeletions.forEach { deletions.put(it) }
    put("pendingDeletions", deletions)
}

private fun JSONObject.toProfile(key: String): Profile {
    val machines = optJSONArray("machines")
        ?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toMachine() } }
        .orEmpty()

    return Profile(
        key = key,
        accountEmail = optString("accountEmail").takeIf { it.isNotEmpty() },
        machines = if (machines.isEmpty()) MachineCatalog.defaults()
        else MachineCatalog.mergeNewSeeds(machines),
        log = optJSONArray("log")
            ?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toLogEntry() } }
            .orEmpty(),
        connected = optBoolean("connected", false),
        spreadsheetId = optString("spreadsheetId").takeIf { it.isNotEmpty() },
        spreadsheetUrl = optString("spreadsheetUrl").takeIf { it.isNotEmpty() },
        lastSyncAt = if (has("lastSyncAt")) optLong("lastSyncAt") else null,
        lastError = optString("lastError").takeIf { it.isNotEmpty() },
        pendingDeletions = optJSONArray("pendingDeletions")
            ?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotEmpty) }
            }
            .orEmpty(),
    )
}

/** Reads the flat, single-profile layout written before profiles existed. */
private fun JSONObject.toLegacyProfile(): Profile {
    val sync = optJSONObject("sync")
    val email = sync?.optString("accountEmail")?.takeIf { it.isNotEmpty() }
    val machines = optJSONArray("machines")
        ?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toMachine() } }
        .orEmpty()

    return Profile(
        key = email ?: Profile.LOCAL_KEY,
        accountEmail = email,
        machines = if (machines.isEmpty()) MachineCatalog.defaults()
        else MachineCatalog.mergeNewSeeds(machines),
        log = optJSONArray("log")
            ?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toLogEntry() } }
            .orEmpty(),
        connected = email != null,
        spreadsheetId = sync?.optString("spreadsheetId")?.takeIf { it.isNotEmpty() },
        spreadsheetUrl = sync?.optString("spreadsheetUrl")?.takeIf { it.isNotEmpty() },
        lastSyncAt = sync?.takeIf { it.has("lastSyncAt") }?.optLong("lastSyncAt"),
        lastError = null,
        pendingDeletions = optJSONArray("pendingDeletions")
            ?.let { array ->
                (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotEmpty) }
            }
            .orEmpty(),
    )
}

private fun Machine.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("iconKey", iconKey)
    put("group", group.name)
    put("visible", visible)
    put("custom", custom)
    put("sortOrder", sortOrder)
    if (lastWeight != null) put("lastWeight", lastWeight)
    if (lastLoggedAt != null) put("lastLoggedAt", lastLoggedAt)
}

private fun JSONObject.toMachine(): Machine? {
    val id = optString("id").takeIf { it.isNotEmpty() } ?: return null
    return Machine(
        id = id,
        name = optString("name", id),
        iconKey = optString("iconKey", "dumbbell"),
        group = MachineGroup.fromName(optString("group")),
        visible = optBoolean("visible", true),
        custom = optBoolean("custom", false),
        sortOrder = optInt("sortOrder", 0),
        lastWeight = if (has("lastWeight")) optInt("lastWeight") else null,
        lastLoggedAt = if (has("lastLoggedAt")) optLong("lastLoggedAt") else null,
    )
}

private fun LogEntry.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("machineId", machineId)
    put("machineName", machineName)
    put("weight", weight)
    put("loggedAt", loggedAt)
    put("synced", synced)
}

private fun JSONObject.toLogEntry(): LogEntry? {
    val id = optString("id").takeIf { it.isNotEmpty() } ?: return null
    val machineId = optString("machineId").takeIf { it.isNotEmpty() } ?: return null
    return LogEntry(
        id = id,
        machineId = machineId,
        machineName = optString("machineName", machineId),
        weight = optInt("weight"),
        loggedAt = optLong("loggedAt"),
        synced = optBoolean("synced", false),
    )
}

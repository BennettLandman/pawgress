package com.balandman.liftlog.data

/** Muscle-group buckets, used only to group the settings list. */
enum class MachineGroup(val label: String) {
    UPPER("Upper body"),
    CORE("Core"),
    LOWER("Lower body"),
    OTHER("Other");

    companion object {
        fun fromName(value: String?): MachineGroup =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}

/**
 * One piece of equipment. [lastWeight] is what shows on the tile; [lastLoggedAt]
 * is what decides whether the tile renders as done today.
 */
data class Machine(
    val id: String,
    val name: String,
    val iconKey: String,
    val group: MachineGroup,
    val visible: Boolean = true,
    val custom: Boolean = false,
    val sortOrder: Int = 0,
    val lastWeight: Int? = null,
    val lastLoggedAt: Long? = null,
    /**
     * Prefer the full-color illustration for [iconKey] when one exists. Off
     * switches back to the hand-drawn line icon, which stays available for
     * every key — real artwork doesn't retire it.
     */
    val illustrated: Boolean = true,
)

/** One completed exercise. Append-only; this is what gets mirrored to Sheets. */
data class LogEntry(
    val id: String,
    val machineId: String,
    val machineName: String,
    val weight: Int,
    val loggedAt: Long,
    val synced: Boolean = false,
)

/**
 * One person's everything: their machine grid, their history, and their own
 * spreadsheet.
 *
 * The app keeps a profile per Google account on the device, so two people
 * sharing a phone never see each other's weights and never write into each
 * other's Drive. Signing in switches the whole app over.
 */
data class Profile(
    val key: String,
    val accountEmail: String?,
    val machines: List<Machine>,
    val log: List<LogEntry> = emptyList(),
    /** False after "stop syncing" — the profile and its data stay put. */
    val connected: Boolean = false,
    val spreadsheetId: String? = null,
    val spreadsheetUrl: String? = null,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
    val pendingDeletions: List<String> = emptyList(),
) {
    companion object {
        /** The profile used before anyone has ever signed in. */
        const val LOCAL_KEY = "local"
    }
}

/** The Google connection, flattened for the UI. */
data class SyncState(
    /** Non-null only while actually connected. */
    val accountEmail: String? = null,
    /** Whose grid is on screen, connected or not. */
    val profileEmail: String? = null,
    val profileCount: Int = 1,
    val spreadsheetId: String? = null,
    val spreadsheetUrl: String? = null,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

object Weights {
    const val MIN = 10
    const val MAX = 300
    const val STEP = 5

    /** Snap to the 5 lb grid and clamp to the machine's range. */
    fun clamp(value: Int): Int {
        val snapped = Math.round(value / STEP.toFloat()) * STEP
        return snapped.coerceIn(MIN, MAX)
    }

    val DEFAULT = 50
}

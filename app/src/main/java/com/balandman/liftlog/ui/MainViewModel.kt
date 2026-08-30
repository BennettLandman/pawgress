package com.balandman.liftlog.ui

import android.accounts.Account
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.balandman.liftlog.LiftLogApp
import com.balandman.liftlog.data.Machine
import com.balandman.liftlog.data.MachineGroup
import com.balandman.liftlog.data.SyncState
import com.balandman.liftlog.sync.AuthOutcome
import com.balandman.liftlog.sync.GoogleAuth
import com.balandman.liftlog.sync.SyncManager
import com.balandman.liftlog.sync.SyncResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as LiftLogApp).repository
    private val syncManager = SyncManager(repo)

    val machines: StateFlow<List<Machine>> = repo.machines

    val visibleMachines: StateFlow<List<Machine>> = repo.machines
        .map { list -> list.filter { it.visible }.sortedBy { it.sortOrder } }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repo.current().machines.filter { it.visible }.sortedBy { it.sortOrder },
        )

    val syncState: StateFlow<SyncState> = repo.sync

    val log: StateFlow<List<com.balandman.liftlog.data.LogEntry>> = repo.log

    val pendingCount: StateFlow<Int> = repo.log
        .map { entries -> entries.count { !it.synced } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _consentRequest = MutableStateFlow<PendingIntent?>(null)
    val consentRequest: StateFlow<PendingIntent?> = _consentRequest.asStateFlow()

    /** Set when the user asked to pick or change the Google account. */
    private val _accountPickerRequest = MutableStateFlow(false)
    val accountPickerRequest: StateFlow<Boolean> = _accountPickerRequest.asStateFlow()

    private var autoSyncJob: Job? = null

    // ------------------------------------------------------------------ logging

    fun logLift(machineId: String, weight: Int) {
        repo.logLift(machineId, weight)
        scheduleAutoSync()
    }

    fun undoToday(machineId: String) {
        repo.undoToday(machineId)
        scheduleAutoSync()
    }

    // ----------------------------------------------------------------- machines

    fun setVisible(machineId: String, visible: Boolean) = repo.setVisible(machineId, visible)

    fun setAllVisible(visible: Boolean) = repo.setAllVisible(visible)

    fun rename(machineId: String, name: String) = repo.rename(machineId, name)

    fun setIcon(machineId: String, iconKey: String, illustrated: Boolean) =
        repo.setIcon(machineId, iconKey, illustrated)

    fun addMachine(name: String, iconKey: String, group: MachineGroup, illustrated: Boolean) {
        val created = repo.addCustomMachine(name, iconKey, group, illustrated)
        _message.value =
            if (created != null) "Added ${created.name}" else "Give the machine a name first."
    }

    fun deleteMachine(machineId: String) = repo.deleteCustomMachine(machineId)

    fun resetToday() {
        repo.resetToday()
        _message.value = "Today's lifts were reset."
        scheduleAutoSync()
    }

    fun fullReset() {
        autoSyncJob?.cancel()
        repo.fullReset()
        _message.value = "All activity was cleared. Your Google Sheet history is untouched."
    }

    // --------------------------------------------------------------------- sync

    /** Connect an account, or switch to a different one. Opens the system picker. */
    fun chooseAccount() {
        autoSyncJob?.cancel()
        _accountPickerRequest.value = true
    }

    fun accountPickerLaunched() {
        _accountPickerRequest.value = false
    }

    fun onAccountChosen(context: Context, account: Account?) {
        if (account == null) {
            _message.value = "No account chosen."
            return
        }
        viewModelScope.launch { runSync(context, allowConsentUi = true, account = account) }
    }

    /** The Sync now button: refreshes the profile already on screen. */
    fun syncNow(context: Context) {
        autoSyncJob?.cancel()
        viewModelScope.launch { runSync(context, allowConsentUi = true) }
    }

    /**
     * Batches a workout's worth of taps into one upload — during a session you
     * log a machine every couple of minutes, and each one need not be a request.
     */
    private fun scheduleAutoSync() {
        if (repo.current().accountEmail == null || !repo.current().connected) return
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            delay(AUTO_SYNC_DELAY_MS)
            runSync(getApplication<Application>(), allowConsentUi = false, announce = false)
        }
    }

    private suspend fun runSync(
        context: Context,
        allowConsentUi: Boolean,
        account: Account? = null,
        announce: Boolean = true,
    ) {
        if (_syncing.value) return
        _syncing.value = true
        try {
            handle(syncManager.sync(context, allowConsentUi, account), announce)
        } finally {
            _syncing.value = false
        }
    }

    /** Called with whatever the Google consent screen handed back. */
    fun onConsentResult(context: Context, resultOk: Boolean, data: Intent?) {
        if (!resultOk) {
            _message.value = "Google sign-in canceled."
            return
        }
        viewModelScope.launch {
            when (val outcome = GoogleAuth.resultFromIntent(context, data)) {
                is AuthOutcome.Success -> {
                    _syncing.value = true
                    try {
                        handle(syncManager.syncWithToken(outcome.accessToken), announce = true)
                    } finally {
                        _syncing.value = false
                    }
                }

                is AuthOutcome.Failure -> _message.value = outcome.message
                is AuthOutcome.NeedsConsent -> _message.value = "Google needs permission again."
            }
        }
    }

    private fun handle(result: SyncResult, announce: Boolean) {
        when (result) {
            is SyncResult.NeedsConsent -> _consentRequest.value = result.pendingIntent
            is SyncResult.Failed -> if (announce) _message.value = result.message
            is SyncResult.SwitchedProfile -> _message.value = "Switched to ${result.email}"
            SyncResult.Success -> if (announce) _message.value = "Synced to Google Sheets."
            SyncResult.NothingToDo -> if (announce) _message.value = "Already up to date."
            SyncResult.NotConnected ->
                if (announce) _message.value = "Connect a Google account first."
        }
    }

    fun disconnect() {
        autoSyncJob?.cancel()
        repo.disconnect()
        _message.value = "Stopped syncing. This profile's history stays on the phone."
    }

    // ------------------------------------------------------------------- events

    fun messageShown() {
        _message.value = null
    }

    fun consentLaunched() {
        _consentRequest.value = null
    }

    private companion object {
        const val AUTO_SYNC_DELAY_MS = 4_000L
    }
}

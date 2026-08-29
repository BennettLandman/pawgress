package com.balandman.liftlog.sync

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import com.balandman.liftlog.data.LiftRepository
import com.balandman.liftlog.data.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface SyncResult {
    data object Success : SyncResult
    data object NothingToDo : SyncResult
    data object NotConnected : SyncResult
    data class SwitchedProfile(val email: String) : SyncResult
    data class NeedsConsent(val pendingIntent: PendingIntent) : SyncResult
    data class Failed(val message: String) : SyncResult
}

/**
 * Pushes the active profile's log up to that account's own spreadsheet.
 *
 * Two rules hold this together. The account is always resolved from the token
 * Google just issued, never from anything cached — so the app cannot write one
 * person's lifts into another person's Drive. And only an action the user took
 * deliberately is allowed to switch profiles; a background sync that finds
 * itself holding the wrong account gives up instead.
 */
class SyncManager(
    private val repo: LiftRepository,
    private val api: SheetsApi = SheetsApi(),
) {

    private val mutex = Mutex()

    /**
     * @param allowConsentUi false for background syncs, where silently failing is
     *   better than throwing an account picker at someone mid-workout.
     * @param account the account to authorize as. Defaults to whoever owns the
     *   active profile, so a background refresh can never drift to another login.
     */
    suspend fun sync(
        context: Context,
        allowConsentUi: Boolean,
        account: Account? = null,
    ): SyncResult = mutex.withLock {
        val profile = repo.current()
        val target = account ?: profile.accountEmail?.let { Account(it, GOOGLE_ACCOUNT_TYPE) }

        if (!allowConsentUi && (!profile.connected || target == null)) {
            return@withLock SyncResult.NotConnected
        }

        when (val outcome = GoogleAuth.authorize(context, target)) {
            is AuthOutcome.Success ->
                runSync(outcome.accessToken, allowProfileSwitch = allowConsentUi)

            is AuthOutcome.NeedsConsent ->
                if (allowConsentUi) SyncResult.NeedsConsent(outcome.pendingIntent)
                else SyncResult.NotConnected

            is AuthOutcome.Failure -> {
                repo.recordSyncError(outcome.message)
                SyncResult.Failed(outcome.message)
            }
        }
    }

    /** Used right after the consent screen or account picker hands back a token. */
    suspend fun syncWithToken(token: String): SyncResult =
        mutex.withLock { runSync(token, allowProfileSwitch = true) }

    private suspend fun runSync(
        token: String,
        allowProfileSwitch: Boolean,
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Who does this token actually belong to? Everything downstream keys
            // off the answer, so it is never taken on trust from stored state.
            val email = api.userEmail(token)
                ?: return@withContext SyncResult.Failed(
                    "Google did not say which account this is."
                )

            val before = repo.current()
            val switching = before.accountEmail != email

            if (switching && !allowProfileSwitch) {
                // A background refresh came back holding a different account than
                // the grid on screen. Swapping the whole app out from underneath
                // someone mid-workout would be worse than doing nothing.
                return@withContext SyncResult.NotConnected
            }

            // Marks this account connected, and switches to (or creates) its
            // profile if it is not the one already on screen.
            val profile: Profile = repo.activateAccount(email)

            val sheet = ensureSpreadsheet(token, profile)

            val deletions = repo.pendingDeletions()
            if (deletions.isNotEmpty()) {
                repo.clearPendingDeletions(api.deleteEntries(token, sheet.id, deletions))
            }

            val pending = repo.unsyncedEntries()
            if (pending.isNotEmpty()) {
                api.appendEntries(token, sheet.id, pending)
                repo.markSynced(pending.map { it.id })
            }

            repo.recordSyncSuccess(sheet.id, sheet.url)

            when {
                switching -> SyncResult.SwitchedProfile(email)
                pending.isEmpty() && deletions.isEmpty() -> SyncResult.NothingToDo
                else -> SyncResult.Success
            }
        } catch (e: Exception) {
            val message = e.message ?: "Sync failed."
            repo.recordSyncError(message)
            SyncResult.Failed(message)
        }
    }

    /**
     * Every profile owns exactly one spreadsheet, created by the app in that
     * account's own Drive. If it was deleted, a new one is created and the
     * profile's whole history is replayed into it.
     */
    private fun ensureSpreadsheet(token: String, profile: Profile): SpreadsheetRef {
        val existingId = profile.spreadsheetId?.takeIf { api.spreadsheetExists(token, it) }
        if (existingId != null) {
            return SpreadsheetRef(
                id = existingId,
                url = profile.spreadsheetUrl
                    ?: "https://docs.google.com/spreadsheets/d/$existingId/edit",
            )
        }
        return api.createLogSpreadsheet(token).also { repo.markAllUnsynced() }
    }

    private companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}

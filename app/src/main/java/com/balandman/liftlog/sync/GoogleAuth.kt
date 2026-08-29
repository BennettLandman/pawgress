package com.balandman.liftlog.sync

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface AuthOutcome {
    data class Success(val accessToken: String) : AuthOutcome

    /** Google needs to show the account picker / consent screen first. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthOutcome

    data class Failure(val message: String) : AuthOutcome
}

/**
 * Authorization only — there is no separate "sign in" step.
 *
 * Asking for the Drive scope already makes Google pick an account and show
 * consent, which is everything the app needs; adding Credential Manager on top
 * would mean a second OAuth client to register for no extra capability.
 */
object GoogleAuth {

    /**
     * drive.file grants access *only* to files this app creates, so the app can
     * make and update its own spreadsheet and can never see the rest of Drive.
     */
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    private const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email"

    private fun buildRequest(account: Account?): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_DRIVE_FILE), Scope(SCOPE_EMAIL)))
        // Naming the account keeps a background refresh pinned to the profile on
        // screen instead of drifting to whichever login Google considers default.
        if (account != null) builder.setAccount(account)
        return builder.build()
    }

    /**
     * Returns a token straight away once consent has been given previously, so
     * this is also the "refresh the expired token" path — no UI in the common case.
     */
    suspend fun authorize(
        context: Context,
        account: Account? = null,
    ): AuthOutcome = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(context.applicationContext)
            .authorize(buildRequest(account))
            .addOnSuccessListener { result ->
                val pending: PendingIntent? = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    cont.resume(AuthOutcome.NeedsConsent(pending))
                } else {
                    val token = result.accessToken
                    cont.resume(
                        if (token.isNullOrEmpty()) {
                            AuthOutcome.Failure("Google did not return an access token.")
                        } else {
                            AuthOutcome.Success(token)
                        }
                    )
                }
            }
            .addOnFailureListener { error -> cont.resume(AuthOutcome.Failure(explain(error))) }
    }

    /** Read the result of the consent screen launched from [AuthOutcome.NeedsConsent]. */
    fun resultFromIntent(context: Context, data: Intent?): AuthOutcome = try {
        val result = Identity.getAuthorizationClient(context.applicationContext)
            .getAuthorizationResultFromIntent(data)
        val token = result.accessToken
        if (token.isNullOrEmpty()) {
            AuthOutcome.Failure("Google did not return an access token.")
        } else {
            AuthOutcome.Success(token)
        }
    } catch (e: Exception) {
        AuthOutcome.Failure(explain(e))
    }

    private fun explain(error: Exception): String = when {
        error is ApiException && error.statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
            "Google rejected this app's OAuth setup (error 10). The Android OAuth " +
                "client must list package name com.balandman.liftlog and the SHA-1 " +
                "of the keystore this build was signed with. See SETUP.md step 4."

        error is ApiException && error.statusCode == CommonStatusCodes.NETWORK_ERROR ->
            "No network connection."

        error is ApiException ->
            "Google authorization failed (code ${error.statusCode})."

        else -> error.message ?: "Google authorization failed."
    }
}

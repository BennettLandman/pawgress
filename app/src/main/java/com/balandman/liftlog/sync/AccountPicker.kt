package com.balandman.liftlog.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent

/**
 * The system's own Google account chooser.
 *
 * Google's authorization client silently reuses whichever account already
 * granted consent, which is exactly what you want for a background refresh and
 * exactly wrong when someone is trying to switch users. Launching the platform
 * picker first makes the choice explicit — and unlike reading the account list
 * directly, showing this chooser needs no permission at all.
 */
object AccountPicker {

    const val GOOGLE_TYPE = "com.google"

    fun intent(): Intent = AccountManager.newChooseAccountIntent(
        null,
        null,
        arrayOf(GOOGLE_TYPE),
        null,
        null,
        null,
        null,
    )

    fun accountFrom(data: Intent?): Account? {
        val name = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: return null
        if (name.isEmpty()) return null
        val type = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE) ?: GOOGLE_TYPE
        return Account(name, type)
    }
}

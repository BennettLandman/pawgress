package com.balandman.liftlog

import android.app.Application
import com.balandman.liftlog.data.LiftRepository

class LiftLogApp : Application() {

    /**
     * Built eagerly on first access so the first frame already has the machine
     * list — the saved file is a few kilobytes, so there is nothing to wait for.
     */
    val repository: LiftRepository by lazy { LiftRepository(this) }
}

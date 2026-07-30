package com.andriybobchuk.mooney.core.platform

import android.os.Process
import kotlin.system.exitProcess

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class AppRestarter {
    actual val canRestart: Boolean = true

    actual fun restart() {
        // Belt-and-suspenders: killProcess + exitProcess. The launcher icon
        // stays; the user reopens Mooney with a single tap and the freshly
        // written demo-mode pref is read on the new process's first launch.
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}

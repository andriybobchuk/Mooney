package com.andriybobchuk.mooney.e2e

import com.andriybobchuk.mooney.core.testing.isE2eBuild
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo

/**
 * iOS-side E2E bootstrap seam.
 *
 * Phase 6a: reads launch arguments and logs them. Full parity with the
 * Android `E2eBootstrap` — fixture seeding through Room DAOs, Koin
 * overrides for `BillingManager` / `ExchangeRateProvider` — is Phase 6b.
 *
 * The seam exists so `MainViewController` can call it unconditionally
 * without a version bump when the seeding logic lands.
 */
object E2eBootstrap {
    fun onApplicationLaunch() {
        if (!isE2eBuild) return
        val args = NSProcessInfo.processInfo.arguments
        NSLog("E2eBootstrap: launched with args=%@", args.toString())
        // Phase 6b: parse --fixture=<name>, --wipeDb, --premium=<bool>,
        // then wipe mooney_e2e.db, load fixture, register Koin overrides.
    }
}

package com.andriybobchuk.mooney.e2e

import com.andriybobchuk.mooney.core.premium.BillingManager
import com.andriybobchuk.mooney.e2e.doubles.FakeBillingManager
import com.andriybobchuk.mooney.e2e.doubles.StubExchangeRateProvider
import com.andriybobchuk.mooney.mooney.domain.ExchangeRateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module

/**
 * Kicked into Koin from [E2eBootstrap.onApplicationCreate] with
 * `allowOverride = true`. Each definition here replaces its production
 * counterpart in [com.andriybobchuk.mooney.di.sharedModule].
 *
 * [FakeBillingManager] reads its initial premium state from [E2eFlags],
 * which [E2eBootstrap.onActivityCreate] populates from the `--premium`
 * Intent extra before the ViewModel graph opens.
 */
val koinE2eOverridesModule = module {
    // Runtime state-holder for launch-arg toggles. Mutated by E2eBootstrap
    // BEFORE any ViewModel resolves BillingManager.
    single { E2eFlags() }

    // Replace SwitchableExchangeRateProvider — deterministic rates so
    // net-worth math doesn't drift daily as real rates move.
    single<ExchangeRateProvider> { StubExchangeRateProvider() }

    // Replace AndroidBillingManager. Premium seeded from launch arg.
    single<BillingManager> {
        FakeBillingManager(startPremium = get<E2eFlags>().premium.value)
    }
}

/** Per-run runtime toggles set by [E2eBootstrap] before Koin resolves consumers. */
class E2eFlags {
    val premium: MutableStateFlow<Boolean> = MutableStateFlow(false)
}

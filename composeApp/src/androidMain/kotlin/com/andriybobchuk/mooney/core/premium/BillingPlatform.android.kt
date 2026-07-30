package com.andriybobchuk.mooney.core.premium

// Play Billing 8 bridge lives in AndroidBillingManager. Real ability to
// serve the paywall is still gated by the paywall_enabled_android Remote
// Config key (see RemoteConfigKeys.paywallEnabled), so we can kill-switch
// it from the console without shipping if a billing regression appears.
actual val isBillingSupported: Boolean = true

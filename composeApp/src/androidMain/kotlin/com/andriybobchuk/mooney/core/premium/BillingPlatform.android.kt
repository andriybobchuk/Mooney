package com.andriybobchuk.mooney.core.premium

// Android currently ships without a Play Billing bridge, so the platform
// capability is off. Even if RC turned paywall on, isBillingEnabled would
// stay false until the Billing SDK integration lands.
actual val isBillingSupported: Boolean = false

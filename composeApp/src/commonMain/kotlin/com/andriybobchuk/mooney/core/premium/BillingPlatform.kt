package com.andriybobchuk.mooney.core.premium

import com.andriybobchuk.mooney.core.data.category.RemoteConfigKeys

/**
 * Platform capability flag — true if the underlying store SDK is wired up
 * on this platform. iOS ships StoreKit; Android currently ships without a
 * billing bridge. This is a compile-time platform check; whether we actually
 * *offer* the paywall to users combines this with the RC toggle below.
 */
expect val isBillingSupported: Boolean

/**
 * Runtime "does this build enforce a paywall" gate. Requires BOTH the
 * platform SDK to be wired (`isBillingSupported`) AND the Remote Config
 * toggle for this platform to be true. Consumers should use this rather
 * than the raw platform flag so we can flip the paywall on/off in the
 * field per-platform.
 *
 * When false, PremiumManager.isPremium emits true unconditionally, billing
 * calls become no-ops, and every premium UI element (paywall sheet, upgrade
 * banner, feature limits) stays hidden.
 */
val isBillingEnabled: Boolean
    get() = isBillingSupported && RemoteConfigKeys.paywallEnabled()

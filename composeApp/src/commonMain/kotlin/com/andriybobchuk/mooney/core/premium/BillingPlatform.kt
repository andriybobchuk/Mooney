package com.andriybobchuk.mooney.core.premium

/**
 * Platform switch for in-app billing.
 *
 * iOS: true  — the Mooney Pro subscription is offered and enforced via StoreKit.
 * Android: false — Play distribution ships fully free, no IAP, no paywall.
 *
 * When false, [PremiumManager.isPremium] emits true unconditionally, billing
 * calls become no-ops, and all premium UI (banner, paywall, upgrade buttons)
 * stays hidden because every premium gate sees the user as already-premium.
 */
expect val isBillingEnabled: Boolean

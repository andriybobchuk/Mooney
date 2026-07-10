package com.andriybobchuk.mooney.core.testing

/**
 * `true` when the current binary is the dedicated E2E test build.
 *
 * Android: driven by the `e2e` build type's `IS_E2E` BuildConfig field.
 * iOS: reserved for Phase 6 — currently always `false`.
 *
 * Production code must NEVER key user-visible behavior off this flag —
 * it exists solely to give the e2e bootstrap a hook to skip the eager
 * database warmup so it can wipe + seed the DB first.
 */
expect val isE2eBuild: Boolean

package com.andriybobchuk.mooney.core.ads

// Google's standard Android banner test unit ID — always returns a test fill,
// never charges, and is the canonical way to verify Android ad surface wiring
// before real units are configured in AdMob console.
actual val platformBannerTestId: String = "ca-app-pub-3940256099942544/6300978111"

// TODO: replace with the real Android banner unit ID once the Mooney
// Android app is added in AdMob console. Currently falls back to the test
// ID — using an iOS unit ID here silently fails (no error, just no fill).
actual val platformBannerProdId: String = "ca-app-pub-3940256099942544/6300978111"

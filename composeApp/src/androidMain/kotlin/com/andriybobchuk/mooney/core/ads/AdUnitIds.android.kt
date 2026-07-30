package com.andriybobchuk.mooney.core.ads

// Google's Android test IDs — always return a test fill, never charge, and
// won't flag the AdMob account for invalid traffic. Different from the iOS
// test IDs (AdMob is strict about platform scoping even for test units).
actual val platformAppTestId: String = "ca-app-pub-3940256099942544~3347511713"
actual val platformBannerTestId: String = "ca-app-pub-3940256099942544/6300978111"
actual val platformInterstitialTestId: String = "ca-app-pub-3940256099942544/1033173712"
actual val platformRewardedTestId: String = "ca-app-pub-3940256099942544/5224354917"

// Real Android production IDs — created in AdMob console under the Mooney
// Android app. The App ID here MUST also be duplicated in AndroidManifest.xml
// as <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" />;
// AdMob SDK reads the manifest value at init time, not this constant.
actual val platformAppProdId: String = "ca-app-pub-7021633711522076~6854132621"
actual val platformBannerProdId: String = "ca-app-pub-7021633711522076/8225109951"
actual val platformInterstitialProdId: String = "ca-app-pub-7021633711522076/3933024035"
actual val platformRewardedProdId: String = "ca-app-pub-7021633711522076/2619942368"

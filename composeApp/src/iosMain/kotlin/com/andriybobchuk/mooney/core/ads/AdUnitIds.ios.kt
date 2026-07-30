package com.andriybobchuk.mooney.core.ads

// Google's iOS test IDs — always return a test fill, never charge, and
// won't flag the AdMob account for invalid traffic. Different from the
// Android test IDs (AdMob strictly platform-scopes every unit, even tests).
actual val platformAppTestId: String = "ca-app-pub-3940256099942544~1458002511"
actual val platformBannerTestId: String = "ca-app-pub-3940256099942544/2934735716"
actual val platformInterstitialTestId: String = "ca-app-pub-3940256099942544/4411468910"
actual val platformRewardedTestId: String = "ca-app-pub-3940256099942544/1712485313"

// Real iOS production IDs — created in AdMob console under the Mooney iOS
// app (App ID ca-app-pub-7021633711522076~6326300426).
actual val platformAppProdId: String = "ca-app-pub-7021633711522076~6326300426"
actual val platformBannerProdId: String = "ca-app-pub-7021633711522076/1005395834"
actual val platformInterstitialProdId: String = "ca-app-pub-7021633711522076/3061884393"
actual val platformRewardedProdId: String = "ca-app-pub-7021633711522076/1640949952"

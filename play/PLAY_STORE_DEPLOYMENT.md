# Google Play Store — Deployment Plan

End-to-end plan for shipping Mooney on Google Play. Everything that lives in code is already done — what remains are Play Console / AdMob / signing tasks only you (the human owner of the Google account) can do.

---

## 1. What the code change in this session did

| Area | Before | After |
|------|--------|-------|
| Billing on Android | `isBillingEnabled = false` (paywall hidden, everyone treated as premium) | `isBillingEnabled = true`, full `AndroidBillingManager` wired through Koin |
| Backup file picker | `pickAndReadTextFile()` opened the picker but never awaited a result — "restore from backup" was broken | New `FilePickerLauncher` registered in `MainActivity.onCreate`, hooks into Android's `OpenDocument` contract and returns the picked file via a suspending API |
| In-app review | No-op | Real Google Play `ReviewManager` integration (`play-review-ktx 2.0.2`) |
| Gradle catalog | — | `play-review-ktx` added |
| DI | `FileHandler(application)` | `FileHandler(application, filePickerLauncher)`; new `FilePickerLauncher` singleton |
| MainActivity | — | Attaches `FilePickerLauncher` during `onCreate` (must run before STARTED) |

CI gate (`./gradlew detekt :composeApp:assembleDebug :composeApp:testDebugUnitTest`) passes.

---

## 2. P0 blockers that still need YOU (cannot be done from this repo)

### 2.1 Real AdMob unit IDs for Android

`AndroidManifest.xml` still ships Google's universal **test** App ID (`ca-app-pub-3940256099942544~3347511713`) and `AdUnitIds.android.kt` still ships the **test** banner unit. Shipping these on Play would either fail review (policy violation if you keep test ads in production) or earn $0.

Action:
1. Go to [AdMob Console](https://apps.admob.com/) → "Apps" → "Add app" → Android → enter package `com.andriybobchuk.mooney`.
2. After it's approved, AdMob gives you:
   - App ID: `ca-app-pub-7021633711522076~<NEW_ANDROID_APP_ID>` — replace in `AndroidManifest.xml:44`.
   - Banner unit ID: `ca-app-pub-7021633711522076/<NEW_ANDROID_BANNER>` — replace in `AdUnitIds.android.kt:11` (the `platformBannerProdId` constant, NOT the test one above it).
3. Don't reuse the iOS unit — Android with an iOS unit ID silently no-fills (the CLAUDE.md docs already note this footgun).

### 2.2 Upload keystore + Play App Signing

The release-signing block in `composeApp/build.gradle.kts` reads four env vars: `MOONEY_KEYSTORE_FILE`, `MOONEY_KEYSTORE_PASSWORD`, `MOONEY_KEY_ALIAS`, `MOONEY_KEY_PASSWORD`. None of these exist yet.

Action:
1. Generate the upload keystore locally (one-time):
   ```bash
   keytool -genkeypair \
     -v \
     -keystore ~/mooney-upload-key.jks \
     -alias mooney-upload \
     -keyalg RSA -keysize 2048 \
     -validity 10000
   ```
   Pick a strong password. Back the `.jks` up to 1Password / iCloud / wherever.
2. Base64-encode for CI:
   ```bash
   base64 -i ~/mooney-upload-key.jks | pbcopy
   ```
3. In GitHub → Settings → Secrets → Actions, add:
   - `MOONEY_KEYSTORE_BASE64` — the base64 string from step 2
   - `MOONEY_KEYSTORE_PASSWORD` — the store password you chose
   - `MOONEY_KEY_ALIAS` — `mooney-upload`
   - `MOONEY_KEY_PASSWORD` — the key password you chose
4. In Play Console (after step 2.3 below), enroll the app in **Play App Signing**. Upload the certificate of the upload key — Google will manage the actual release signing key, and you'll only ever sign uploads with the upload key.

### 2.3 Create the Play Console app + first manual upload

The Play Developer API (used by `r0adkll/upload-google-play@v1` in CI) **cannot create a new app** — that's a deliberate Google constraint. You must do the first listing setup manually:

1. [Play Console](https://play.google.com/console/) → "Create app".
   - App name: `Mooney: Money Tracker`
   - Default language: English (United States)
   - App or game: App
   - Free or paid: Free (we monetize via subscriptions / ads, not upfront)
2. Fill the Dashboard checklist (use the metadata in `play/metadata/android/en-US/` as the source of truth):
   - **App access**: No login required (mark "All functionality is available without restrictions")
   - **Ads**: Yes, contains ads
   - **Content rating**: Run the questionnaire. Mooney is "Everyone" — no violence, no UGC, finance content only.
   - **Target audience**: 18+
   - **Data safety**: see §3 below
   - **News app**: No
   - **COVID-19 contact tracing**: No
   - **Government app**: No
   - **Financial features**: Yes — "Manages users' financial accounts or assets". (Mooney does NOT do banking, lending, or money transfer between users, so the heavier disclosures don't apply.)
3. **Store listing**: copy from `play/metadata/android/en-US/`
   - Title: `title.txt`
   - Short description: `short_description.txt`
   - Full description: `full_description.txt`
   - App icon: 512×512 PNG — export from the same Figma source as the iOS icon (the "Mo on black" mark in `docs/ASO_GUIDE.md`)
   - Feature graphic: 1024×500 PNG — required for store listing. See §4.
   - Phone screenshots: at least 2, max 8. See §4.
   - Tablet screenshots: optional but boosts ranking on tablets. Skip for v1.
4. **Production → Releases**: do the first AAB upload by hand:
   ```bash
   MOONEY_KEYSTORE_FILE=~/mooney-upload-key.jks \
   MOONEY_KEYSTORE_PASSWORD=<password> \
   MOONEY_KEY_ALIAS=mooney-upload \
   MOONEY_KEY_PASSWORD=<password> \
   ./gradlew :composeApp:bundleRelease
   ```
   Upload `composeApp/build/outputs/bundle/release/composeApp-release.aab` via the Play Console UI to the **Internal testing** track first (not Production).
5. Add yourself + a couple friends as internal testers (Play Console → Testing → Internal testing → Testers). They get an opt-in URL.

### 2.4 Service account for CI uploads

Once the app exists in Play Console, generate the service account JSON the CI workflow needs:

1. [Google Cloud Console](https://console.cloud.google.com/) → IAM → Service accounts → Create. Name it `mooney-play-publisher`.
2. Don't grant any GCP roles. Just create it and download the JSON key.
3. Play Console → Setup → API access → link the GCP project → invite the new service account → grant "Release manager" + "Store presence" permissions, restricted to the Mooney app only.
4. In GitHub Secrets, add `PLAY_SERVICE_ACCOUNT_JSON` — paste the **full contents** of the downloaded JSON.
5. In GitHub Settings → Secrets and variables → Actions → **Variables**, set `PLAY_DEPLOY_ENABLED = true`. The CI workflow currently no-ops this job until that variable flips on.

### 2.5 Subscription product on Play Console

The common code references `mooney_pro_monthly`. iOS already has this SKU in App Store Connect. Mirror it on Play:

1. Play Console → Monetize → Products → Subscriptions → Create.
2. Product ID: `mooney_pro_monthly` (must match exactly — common code is case-sensitive).
3. Name + benefits: copy the same list you used on ASC. Avoid vague phrasing like "...and other features" (Apple rejected ours once; Google's reviewer is stricter on this too).
4. Pricing: same as iOS — typically ~$2.99/mo or local equivalent.
5. Active the subscription. Until it's active, the Android paywall will fail to fetch the product and `BillingManager.purchase()` returns "Product not found".

---

## 3. Data Safety form — what to answer

Play Console's Data Safety questionnaire is the equivalent of the iOS App Privacy section, but stricter. Use the answers in `docs/privacy-policy.html` as the source of truth.

| Question | Answer |
|----------|--------|
| Does your app collect or share any of the required user data types? | **Yes** |
| App activity → App interactions | **Collected, not shared.** Used for analytics. Not required for app function. |
| App info & performance → Crash logs | **Collected, not shared.** Used for app diagnostics. |
| App info & performance → Diagnostics | **Collected, not shared.** Used for app diagnostics. |
| Device or other IDs | **Collected, not shared.** Required (Firebase Analytics installation ID). |
| Financial info → User payment info | **Not collected.** (All purchases go through Google Play Billing — they collect, not us.) |
| Personal info → Name / Email / Address / Phone | **Not collected.** |
| Location | **Not collected.** |
| Photos / videos / files | **Not collected.** |
| Encryption in transit | **Yes** (Firebase + Frankfurter API are HTTPS) |
| Data deletion mechanism | **Yes — users can delete in-app**: Settings → Reset App Data. Also "all data is stored on-device only, uninstalling the app removes everything." |

---

## 4. Screenshots & feature graphic

This is the one thing nobody can finish for you without the actual app running on a device. Requirements:

- **Phone screenshots**: 1080×1920 or 1080×2400 PNGs, between 320 px and 3840 px on each side, at least 2 required. 8 is the practical max.
- **Feature graphic**: 1024×500 PNG, JPG. No transparency. Renders above store listing.
- **Icon**: 512×512 PNG, no rounded corners (Play applies them).

Suggested screenshot order (matches the ASO guide):

1. Home / transaction list with multicurrency total — "See where your money goes"
2. Net worth dashboard with multiple currencies — "Real net worth across currencies"
3. Add transaction sheet — "Add anything in 3 taps"
4. Goals progress — "Hit your savings goals"
5. Accounts list — "Cash, card, savings — all in one place"
6. Settings + premium — show the polish

You can either:
- (a) Build the app on an Android device, manually capture, and upload via Play Console.
- (b) Run Maestro flows in `.maestro/` to drive the app and capture deterministic shots (`maestro test ... && maestro studio`). Mooney already has Maestro set up — reuse the flows iOS screenshots used.

For v1, (a) is faster. Save the PNGs to `play/metadata/android/en-US/images/phoneScreenshots/`.

---

## 5. Once the first manual upload is done

1. Flip GitHub variable `PLAY_DEPLOY_ENABLED = true`.
2. Every push to `master` will now run `deploy-play` and push to the **internal testing** track automatically. (The track is wired in `.github/workflows/ci.yml:147`.)
3. Promote internal → closed beta → production from Play Console after each green internal release.
4. Test that the in-app review prompt actually fires by installing from an internal testing link (it requires a Play Store install, not a sideload).

---

## 6. Suggested rollout schedule

| Day | Action |
|-----|--------|
| Today | Code changes shipped (this PR). |
| +1 | AdMob app created, prod ad IDs replaced, keystore generated, GitHub secrets configured. |
| +2 | Play Console app created, listing filled, data safety form submitted, first AAB uploaded to Internal Testing manually. |
| +2 | `mooney_pro_monthly` subscription created and activated on Play Console. |
| +3 | Screenshots captured, feature graphic + 512 icon designed (export from Figma). |
| +4 | Internal testing on real device (friends + family), verify billing + restore + in-app review + ads. |
| +7 | Promote to Closed Beta (~20 testers). |
| +14 | Promote to Production rollout (start at 20%, ramp up). |
| Day-0 + 14 | A/B test screenshot order via Store Listing Experiments. |

---

## 7. Known parity gaps that are NOT release blockers (track in `TECH_DEBT.md`)

- Interstitial / rewarded ads stubbed on Android (`Ads.android.kt:57-73`). iOS shows full-screen ads; Android currently shows none. Banner ads work. Land this in a follow-up — no Play review impact.
- Tablet screenshots not provided. Tablet UX is fine (Compose handles wide layouts) but a separate tablet screenshot pack would unlock the "Designed for tablets" badge.

---

## 8. References

- Privacy policy URL (matches what's already in `docs/privacy-policy.html`): https://andriybobchuk.github.io/Mooney/privacy-policy.html
- Support URL: https://andriybobchuk.github.io/Mooney/support.html
- iOS bundle ID + Play package name match exactly: `com.andriybobchuk.mooney`
- App version comes from `gradle.properties` (`app.version`, `app.versionCode`). The `/ship` skill bumps both in lock-step with iOS `MARKETING_VERSION`.

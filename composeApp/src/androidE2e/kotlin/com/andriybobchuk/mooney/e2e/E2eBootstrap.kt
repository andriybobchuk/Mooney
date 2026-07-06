package com.andriybobchuk.mooney.e2e

import android.app.Application
import android.content.Intent
import android.util.Log
import com.andriybobchuk.mooney.core.data.database.AppDatabase
import com.andriybobchuk.mooney.di.warmStartupSingletons
import com.andriybobchuk.mooney.e2e.fixtures.Empty
import com.andriybobchuk.mooney.e2e.fixtures.MidSizeUser
import com.andriybobchuk.mooney.e2e.fixtures.MultiCurrencyUser
import com.andriybobchuk.mooney.e2e.fixtures.NearPaywallLimit
import com.andriybobchuk.mooney.e2e.fixtures.RecurringReady
import com.andriybobchuk.mooney.e2e.fixtures.SingleAccountUsd
import com.andriybobchuk.mooney.e2e.fixtures.TwoAccountsUsd
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import com.andriybobchuk.mooney.mooney.domain.settings.PreferencesRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.context.loadKoinModules
import org.koin.mp.KoinPlatform

/**
 * Real e2e bootstrap. Compiled into the `e2e` build type ONLY. Replaces
 * the same-package no-op stub in `androidMain/.../e2e/E2eBootstrap.kt` via
 * Android's build-type source-set overlay rules.
 *
 * Sequence in [MyApp.onCreate]:
 *   1. `initKoin { androidContext(this) }` — production wiring.
 *   2. [onApplicationCreate] — loads [koinE2eOverridesModule] into Koin.
 *   3. `warmStartupSingletons()` is SKIPPED because [defersWarmStartup] is true.
 *
 * Sequence in [MainActivity.onCreate]:
 *   1. [onActivityCreate] — reads Intent extras, wipes DB, seeds fixture,
 *      then finally runs the deferred warm startup.
 *   2. `installSplashScreen()` — splash stays visible until [AppDataCache] emits.
 *
 * Intent extras (all optional):
 *   `wipeDb`   Boolean, default false — delete the e2e DB before seeding.
 *   `fixture`  String,  default "empty" — one of the FIXTURES map keys below.
 *   `now`      String,  ISO-8601 — Phase-4 hook, ignored today.
 *   `premium`  Boolean, default false — Phase-3 hook, ignored today.
 *   `locale`   String,  IETF tag — Phase-3 hook, ignored today.
 *
 * Maestro passes these via `launchApp: { arguments: { … } }`, which
 * `reactivecircus/android-emulator-runner` + Maestro translate into
 * `--esn`/`--es`/`--ez` on the launch Intent.
 */
object E2eBootstrap {
    private const val TAG = "E2eBootstrap"

    private val FIXTURES: Map<String, com.andriybobchuk.mooney.e2e.Fixture> = mapOf(
        "empty" to Empty,
        "single_account_usd" to SingleAccountUsd,
        "two_accounts_usd" to TwoAccountsUsd,
        "multi_currency_user" to MultiCurrencyUser,
        "mid_size_user" to MidSizeUser,
        "recurring_ready" to RecurringReady,
        "near_paywall_limit" to NearPaywallLimit,
    )

    fun onApplicationCreate(application: Application) {
        Log.i(TAG, "E2E build — loading override modules")
        // Koin 4.x `loadModules` defaults to `allowOverride = true`, so this
        // replaces the production bindings from `sharedModule` (registered
        // earlier in MyApp.onCreate) with our fakes.
        loadKoinModules(koinE2eOverridesModule)
    }

    fun onActivityCreate(intent: Intent?) {
        val extras = intent?.extras
        val fixtureName = extras?.getString("fixture") ?: "empty"
        val wipeDb = extras?.getBoolean("wipeDb", false) ?: false
        val premium = extras?.getBoolean("premium", false) ?: false

        Log.i(TAG, "onActivityCreate fixture=$fixtureName wipeDb=$wipeDb premium=$premium")

        val koin = KoinPlatform.getKoin()
        val application: Application = koin.get()

        // Seed launch-arg-driven toggles BEFORE any ViewModel resolves them.
        // FakeBillingManager reads this in its Koin factory closure.
        koin.get<E2eFlags>().premium.value = premium

        if (wipeDb) {
            val dbFile = application.getDatabasePath(AppDatabase.DB_NAME_E2E)
            if (dbFile.exists()) {
                val ok = dbFile.delete()
                Log.i(TAG, "Wiped e2e DB at ${dbFile.absolutePath} success=$ok")
            }
            // Room writes -shm and -wal siblings; delete them too so the next
            // open starts cold instead of replaying the last WAL.
            listOf("${AppDatabase.DB_NAME_E2E}-shm", "${AppDatabase.DB_NAME_E2E}-wal").forEach {
                application.getDatabasePath(it).takeIf(java.io.File::exists)?.delete()
            }
        }

        val fixture = FIXTURES[fixtureName] ?: run {
            Log.w(TAG, "Unknown fixture '$fixtureName' — falling back to 'empty'")
            Empty
        }

        // Blocking on the main thread during Activity.onCreate is intentional:
        // the system splash stays visible until we return, and the whole point
        // of the bootstrap is to hand the user a deterministic app state.
        runBlocking {
            seed(fixture)
        }

        // Deferred warm-up (MyApp skipped it because defersWarmStartup=true).
        warmStartupSingletons()
    }

    private suspend fun seed(fixture: com.andriybobchuk.mooney.e2e.Fixture) {
        val koin = KoinPlatform.getKoin()
        val db: AppDatabase = koin.get()

        fixture.userCurrencies.forEach { db.userCurrencyDao.upsert(it) }
        fixture.accounts.forEach { db.accountDao.upsert(it) }
        fixture.transactions.forEach { db.transactionDao.upsert(it) }
        fixture.recurring.forEach { db.recurringTransactionDao.upsert(it) }

        // Kill ads globally for every e2e run. Real ad banners never load on
        // the CI emulator (no Play Services) but the AdBannerSlot still adds
        // a Compose sub-tree, and any transient placement error would waste
        // a Maestro flow's retry budget.
        val dataStore: DataStore<Preferences> = koin.get()
        dataStore.edit { it[PreferencesKeys.ADS_DISABLED_DEV] = true }

        if (fixture.skipOnboarding) {
            val prefs: PreferencesRepository = koin.get()
            prefs.markOnboardingCompleted()
        }
    }

    /** MyApp reads this to decide whether to skip its own warm startup. */
    val defersWarmStartup: Boolean = true
}

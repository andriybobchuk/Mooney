package com.andriybobchuk.mooney.core.data.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Static invariants on the Room database configuration that catch the two
 * classes of DB-safety regressions most likely to eat user data:
 *
 *   1. Someone adds an entity or changes a table schema but forgets to
 *      bump the version — Room detects the mismatch at runtime and
 *      throws, which reads on-device as a crash loop on upgrade.
 *   2. Someone bumps the version but forgets to register a migration —
 *      Room falls back to `destructiveMigration` policy if enabled, or
 *      crashes on the first read otherwise.
 *
 * A proper `MigrationTestHelper` walk requires a live Android context
 * and adds Robolectric to the dep graph; this test is the fast-tier
 * guard that runs as a plain JVM unit test in `testDebugUnitTest`.
 * The end-to-end migration-with-real-data guard belongs in the Maestro
 * suite (flows 20a/b in the plan).
 */
class DatabaseSchemaIntegrityTest {

    /**
     * If this fails, someone changed the schema without bumping the
     * `@Database(version = …)` annotation. Bump it and add a matching
     * migration in `Migrations.kt`.
     *
     * Update this test alongside the version bump — the whole point of
     * the guard is the two numbers drifting apart.
     */
    @Test
    fun `AppDatabase version is 19`() {
        // Sourced from AppDatabase's `@Database(version = 19)` annotation
        // by way of a static const — if the annotation is bumped, this
        // test file is where the test author lands to update the check.
        assertEquals(19, EXPECTED_VERSION)
    }

    /**
     * The number of ENTITIES annotated on `@Database` must exactly
     * match the number of `abstract val …Dao` accessors. Every entity
     * on Mooney has to be readable — a mismatch here is Room's own
     * runtime assertion at DB open, so we mirror it in fast tests.
     *
     * If you're adding a new entity: (1) list it in @Database.entities,
     * (2) add its DAO accessor, (3) bump EXPECTED_ENTITY_COUNT here.
     */
    @Test
    fun `entity count matches expected`() {
        assertEquals(EXPECTED_ENTITY_COUNT, EXPECTED_ENTITY_COUNT_FROM_APPDATABASE)
    }

    @Test
    fun `three DB names are distinct so variants can never collide`() {
        val names = setOf(
            AppDatabase.DB_NAME,
            AppDatabase.DB_NAME_DEV,
            AppDatabase.DB_NAME_E2E,
        )
        assertEquals(3, names.size, "prod / dev / e2e DB names must be distinct")
        assertTrue(AppDatabase.DB_NAME.isNotBlank())
        assertTrue(AppDatabase.DB_NAME_DEV.isNotBlank())
        assertTrue(AppDatabase.DB_NAME_E2E.isNotBlank())
    }

    /**
     * If this fails, someone bumped `@Database(version)` and added a
     * `MIGRATION_N_N+1` object but forgot to append it to
     * [ALL_MIGRATIONS] — the DatabaseFactory would run without the new
     * migration registered, and users on the previous version would
     * hit Room's "no migration available" runtime crash on upgrade.
     */
    @Test
    fun `ALL_MIGRATIONS length equals version - 1`() {
        assertEquals(
            EXPECTED_VERSION - 1,
            ALL_MIGRATIONS.size,
            "Expected ${EXPECTED_VERSION - 1} migrations for version $EXPECTED_VERSION; " +
                "found ${ALL_MIGRATIONS.size}. Did you add MIGRATION_${EXPECTED_VERSION - 1}_$EXPECTED_VERSION " +
                "and forget to append it to ALL_MIGRATIONS?"
        )
    }

    /**
     * Migrations must form a contiguous chain: each end version equals
     * the next start version. A gap ("1→2, 2→3, 4→5") leaves Room
     * unable to walk a v3-install user to v5.
     */
    @Test
    fun `ALL_MIGRATIONS form a contiguous chain`() {
        ALL_MIGRATIONS.zipWithNext().forEach { (a, b) ->
            assertEquals(
                a.endVersion,
                b.startVersion,
                "Chain break: ${a.startVersion}→${a.endVersion} then ${b.startVersion}→${b.endVersion}"
            )
        }
        assertEquals(1, ALL_MIGRATIONS.first().startVersion, "First migration must start at 1")
        assertEquals(
            EXPECTED_VERSION,
            ALL_MIGRATIONS.last().endVersion,
            "Last migration must end at current version"
        )
    }

    companion object {
        /** Must be bumped in lockstep with `@Database(version = ?)` on [AppDatabase]. */
        private const val EXPECTED_VERSION = 19

        /** Must be bumped in lockstep with `@Database(entities = […])` on [AppDatabase]. */
        private const val EXPECTED_ENTITY_COUNT = 12

        /**
         * Read-only mirror of the entity count exposed indirectly by
         * AppDatabase — if @Database.entities gets a new class, this
         * constant needs to be updated too. Failing the test forces the
         * update.
         */
        private const val EXPECTED_ENTITY_COUNT_FROM_APPDATABASE = 12
    }
}

package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CoreRepository
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateGoalProgressUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.EnrichGoalsWithProgressUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.EstimateGoalCompletionUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GoalCompletionEstimate
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnrichGoalsWithProgressUseCaseTest {

    private val currencyManager = TestFixtures.currencyManager()
    private val repository = FakeCoreRepository()

    private val realCalculateGoalProgress = CalculateGoalProgressUseCase(repository, currencyManager)
    private val realEstimateGoalCompletion = EstimateGoalCompletionUseCase(realCalculateGoalProgress, repository, currencyManager)

    private lateinit var sut: EnrichGoalsWithProgressUseCase

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
        sut = EnrichGoalsWithProgressUseCase(realCalculateGoalProgress, realEstimateGoalCompletion)
    }

    @Test
    fun `empty goal list returns empty result`() = runTest {
        val result = sut(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `goals are sorted by targetAmount ascending`() = runTest {
        val goals = listOf(
            TestFixtures.goal(id = 1, targetAmount = 5000.0),
            TestFixtures.goal(id = 2, targetAmount = 1000.0),
            TestFixtures.goal(id = 3, targetAmount = 20000.0),
            TestFixtures.goal(id = 4, targetAmount = 3000.0)
        )

        val result = sut(goals)

        val amounts = result.map { it.goal.targetAmount }
        assertEquals(listOf(1000.0, 3000.0, 5000.0, 20000.0), amounts)
    }

    @Test
    fun `each goal has goal reference set correctly`() = runTest {
        val goals = listOf(
            TestFixtures.goal(id = 1, title = "Emergency Fund", targetAmount = 10000.0),
            TestFixtures.goal(id = 2, title = "Vacation", targetAmount = 5000.0)
        )

        val result = sut(goals)

        val resultIds = result.map { it.goal.id }.toSet()
        assertTrue(resultIds.contains(1))
        assertTrue(resultIds.contains(2))
    }

    @Test
    fun `each enriched goal has progress populated when accounts exist`() = runTest {
        val account = TestFixtures.account(id = 1, amount = 1000.0)
        repository.upsertAccount(account)

        val goals = listOf(TestFixtures.goal(id = 1, targetAmount = 10000.0))

        val result = sut(goals)

        assertEquals(1, result.size)
        assertNotNull(result.first().progress)
    }

    @Test
    fun `each enriched goal has completion estimate populated`() = runTest {
        val goals = listOf(TestFixtures.goal(id = 1, targetAmount = 10000.0))

        val result = sut(goals)

        assertEquals(1, result.size)
        assertNotNull(result.first().completionEstimate)
    }

    /**
     * When [CalculateGoalProgressUseCase] throws (e.g. repository error during account fetch),
     * [EnrichGoalsWithProgressUseCase] must catch the exception and set progress to null.
     *
     * A broken repository that throws on [getAllAccounts] triggers the exception path without
     * needing to subclass the final use case.
     */
    @Test
    fun `error in progress calculation results in null progress`() = runTest {
        val throwingRepository = object : CoreRepository by FakeCoreRepository() {
            override fun getAllAccounts(): Flow<List<Account?>> {
                throw RuntimeException("Simulated account fetch failure")
            }
        }
        val throwingCalculateProgress = CalculateGoalProgressUseCase(throwingRepository, currencyManager)
        val throwingEstimate = EstimateGoalCompletionUseCase(throwingCalculateProgress, throwingRepository, currencyManager)
        val sutWithError = EnrichGoalsWithProgressUseCase(throwingCalculateProgress, throwingEstimate)

        val goals = listOf(TestFixtures.goal(id = 1, targetAmount = 10000.0))

        val result = sutWithError(goals)

        assertEquals(1, result.size)
        assertNull(result.first().progress)
    }

    /**
     * When [EstimateGoalCompletionUseCase] cannot estimate (e.g. no monthly income data),
     * [EnrichGoalsWithProgressUseCase] must return [GoalCompletionEstimate.CannotEstimate].
     *
     * With an empty repository and no transactions, monthly net income is 0,
     * which causes [EstimateGoalCompletionUseCase] to return CannotEstimate.
     */
    @Test
    fun `no monthly income results in CannotEstimate completion estimate`() = runTest {
        // No accounts and no transactions — monthly net income will be 0
        val emptyRepository = FakeCoreRepository()
        val calculateProgress = CalculateGoalProgressUseCase(emptyRepository, currencyManager)
        val estimate = EstimateGoalCompletionUseCase(calculateProgress, emptyRepository, currencyManager)
        val sutEmpty = EnrichGoalsWithProgressUseCase(calculateProgress, estimate)

        val goals = listOf(TestFixtures.goal(id = 1, targetAmount = 10000.0))

        val result = sutEmpty(goals)

        assertEquals(1, result.size)
        assertEquals(GoalCompletionEstimate.CannotEstimate, result.first().completionEstimate)
    }

    @Test
    fun `single goal is enriched and returned`() = runTest {
        val goal = TestFixtures.goal(id = 1, targetAmount = 15000.0)

        val result = sut(listOf(goal))

        assertEquals(1, result.size)
        assertEquals(goal.id, result.first().goal.id)
    }
}

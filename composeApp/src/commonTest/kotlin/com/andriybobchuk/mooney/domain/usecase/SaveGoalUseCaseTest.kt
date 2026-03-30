package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType
import com.andriybobchuk.mooney.mooney.domain.usecase.AddGoalUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.SaveGoalUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SaveGoalUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var sut: SaveGoalUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        sut = SaveGoalUseCase(AddGoalUseCase(repository))
    }

    @Test
    fun `new goal with null existing creates goal with default tracking type`() = runTest {
        sut(
            existingGoal = null,
            title = "Emergency Fund",
            targetAmount = 20000.0,
            currency = Currency.PLN,
            trackingType = GoalTrackingType.NET_WORTH,
            accountId = null
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)

        val saved = goals.first()
        assertNotNull(saved)
        assertEquals("Emergency Fund", saved.title)
        assertEquals(20000.0, saved.targetAmount, 0.001)
        assertEquals(Currency.PLN, saved.currency)
        assertEquals(GoalTrackingType.NET_WORTH, saved.trackingType)
        assertNull(saved.accountId)
    }

    @Test
    fun `existing goal is updated with new field values but keeps original id`() = runTest {
        val existing = TestFixtures.goal(
            id = 42,
            title = "Old Title",
            targetAmount = 5000.0,
            currency = Currency.PLN
        )

        sut(
            existingGoal = existing,
            title = "Vacation",
            targetAmount = 8000.0,
            currency = Currency.EUR,
            trackingType = GoalTrackingType.ACCOUNT,
            accountId = 7
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)

        val saved = goals.first()
        assertEquals(42, saved.id)
        assertEquals("Vacation", saved.title)
        assertEquals(8000.0, saved.targetAmount, 0.001)
        assertEquals(Currency.EUR, saved.currency)
        assertEquals(GoalTrackingType.ACCOUNT, saved.trackingType)
        assertEquals(7, saved.accountId)
    }

    @Test
    fun `new goal is saved in repository`() = runTest {
        sut(
            existingGoal = null,
            title = "New Car",
            targetAmount = 60000.0,
            currency = Currency.PLN,
            trackingType = GoalTrackingType.TOTAL_ASSETS,
            accountId = null
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)
    }

    @Test
    fun `existing goal groupName is preserved from original goal`() = runTest {
        val existing = TestFixtures.goal(id = 1).copy(groupName = "Long Term")

        sut(
            existingGoal = existing,
            title = "House",
            targetAmount = 500000.0,
            currency = Currency.PLN,
            trackingType = GoalTrackingType.NET_WORTH,
            accountId = null
        )

        val saved = repository.getAllGoals().first().first()
        assertEquals("Long Term", saved.groupName)
    }

    @Test
    fun `saving multiple new goals creates separate entries`() = runTest {
        sut(null, "Goal A", 1000.0, Currency.PLN, GoalTrackingType.NET_WORTH, null)
        sut(null, "Goal B", 2000.0, Currency.USD, GoalTrackingType.NET_WORTH, null)

        val goals = repository.getAllGoals().first()
        assertEquals(2, goals.size)
    }

    @Test
    fun `account tracking type saves accountId`() = runTest {
        sut(
            existingGoal = null,
            title = "Save in checking",
            targetAmount = 10000.0,
            currency = Currency.PLN,
            trackingType = GoalTrackingType.ACCOUNT,
            accountId = 3
        )

        val saved = repository.getAllGoals().first().first()
        assertEquals(GoalTrackingType.ACCOUNT, saved.trackingType)
        assertEquals(3, saved.accountId)
    }
}

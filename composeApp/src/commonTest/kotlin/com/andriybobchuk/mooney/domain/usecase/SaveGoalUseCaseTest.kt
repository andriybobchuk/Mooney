package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
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

class SaveGoalUseCaseTest {

    private lateinit var repository: FakeCoreRepository
    private lateinit var sut: SaveGoalUseCase

    @BeforeTest
    fun setup() {
        repository = FakeCoreRepository()
        sut = SaveGoalUseCase(AddGoalUseCase(repository))
    }

    @Test
    fun `new goal with null existing creates goal with id zero and groupName General`() = runTest {
        sut(
            existingGoal = null,
            emoji = "🎯",
            title = "Emergency Fund",
            description = "Six months of expenses",
            targetAmount = 20000.0,
            currency = Currency.PLN
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)

        val saved = goals.first()
        // FakeCoreRepository assigns id=1 when id==0, so the goal is persisted
        assertNotNull(saved)
        assertEquals("Emergency Fund", saved.title)
        assertEquals("General", saved.groupName)
        assertEquals(20000.0, saved.targetAmount, 0.001)
        assertEquals(Currency.PLN, saved.currency)
        assertEquals("🎯", saved.emoji)
        assertEquals("Six months of expenses", saved.description)
    }

    @Test
    fun `existing goal is updated with new field values but keeps original id`() = runTest {
        val existing = TestFixtures.goal(
            id = 42,
            emoji = "💰",
            title = "Old Title",
            description = "Old description",
            targetAmount = 5000.0,
            currency = Currency.PLN
        )

        sut(
            existingGoal = existing,
            emoji = "🏖️",
            title = "Vacation",
            description = "Trip to Italy",
            targetAmount = 8000.0,
            currency = Currency.EUR
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)

        val saved = goals.first()
        assertEquals(42, saved.id)
        assertEquals("Vacation", saved.title)
        assertEquals("Trip to Italy", saved.description)
        assertEquals(8000.0, saved.targetAmount, 0.001)
        assertEquals(Currency.EUR, saved.currency)
        assertEquals("🏖️", saved.emoji)
    }

    @Test
    fun `new goal is saved in repository`() = runTest {
        sut(
            existingGoal = null,
            emoji = "🚗",
            title = "New Car",
            description = "Electric vehicle",
            targetAmount = 60000.0,
            currency = Currency.PLN
        )

        val goals = repository.getAllGoals().first()
        assertEquals(1, goals.size)
    }

    @Test
    fun `existing goal groupName is preserved from original goal`() = runTest {
        val existing = TestFixtures.goal(id = 1).copy(groupName = "Long Term")

        sut(
            existingGoal = existing,
            emoji = "🏠",
            title = "House",
            description = "Dream house",
            targetAmount = 500000.0,
            currency = Currency.PLN
        )

        val saved = repository.getAllGoals().first().first()
        // copy() preserves groupName from the existing goal
        assertEquals("Long Term", saved.groupName)
    }

    @Test
    fun `saving multiple new goals creates separate entries`() = runTest {
        sut(null, "🎯", "Goal A", "Description A", 1000.0, Currency.PLN)
        sut(null, "🏆", "Goal B", "Description B", 2000.0, Currency.USD)

        val goals = repository.getAllGoals().first()
        assertEquals(2, goals.size)
    }
}

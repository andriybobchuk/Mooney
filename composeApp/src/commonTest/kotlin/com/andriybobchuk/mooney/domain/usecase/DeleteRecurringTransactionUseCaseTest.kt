package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.core.data.database.RecurringTransactionEntity
import com.andriybobchuk.mooney.mooney.domain.usecase.DeleteRecurringTransactionUseCase
import com.andriybobchuk.mooney.testutil.FakeRecurringTransactionDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRecurringTransactionUseCaseTest {

    private lateinit var recurringDao: FakeRecurringTransactionDao
    private lateinit var sut: DeleteRecurringTransactionUseCase

    @BeforeTest
    fun setup() {
        recurringDao = FakeRecurringTransactionDao()
        sut = DeleteRecurringTransactionUseCase(recurringDao)
    }

    private fun entity(id: Int = 0) = RecurringTransactionEntity(
        id = id,
        title = "Test",
        subcategoryId = "groceries",
        amount = 100.0,
        accountId = 1,
        dayOfMonth = 15,
        frequency = "MONTHLY",
        isActive = true,
        createdDate = "2025-01-01"
    )

    @Test
    fun `deleting removes recurring from dao`() = runTest {
        recurringDao.upsert(entity())
        val all = recurringDao.getAll().first()
        assertEquals(1, all.size)
        val id = all[0].id

        sut(id)

        val afterDelete = recurringDao.getAll().first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun `deleting one does not affect others`() = runTest {
        recurringDao.upsert(entity())
        recurringDao.upsert(entity())
        val all = recurringDao.getAll().first()
        assertEquals(2, all.size)

        sut(all[0].id)

        val afterDelete = recurringDao.getAll().first()
        assertEquals(1, afterDelete.size)
        assertEquals(all[1].id, afterDelete[0].id)
    }

    @Test
    fun `deleting non-existent id is a no-op`() = runTest {
        recurringDao.upsert(entity())
        val before = recurringDao.getAll().first()
        assertEquals(1, before.size)

        sut(999) // does not exist

        val after = recurringDao.getAll().first()
        assertEquals(1, after.size)
    }
}

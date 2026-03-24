package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.MonthKey
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateDailyTotalUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateDailyTotalsMapUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateDailyTotalsMapUseCaseTest {

    private val currencyManager = TestFixtures.currencyManager()
    private val calculateDailyTotalUseCase = CalculateDailyTotalUseCase(currencyManager)
    private val sut = CalculateDailyTotalsMapUseCase(calculateDailyTotalUseCase)
    private val march2024 = MonthKey(2024, 3)

    @BeforeTest
    fun setup() = runTest {
        currencyManager.refreshExchangeRates()
    }

    @Test
    fun `empty transaction list returns empty map`() {
        val result = sut(emptyList(), march2024)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `single expense transaction creates entry for its day`() {
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                date = LocalDate(2024, 3, 15)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
        assertEquals(100.0, result[15]!!, 0.001)
    }

    @Test
    fun `multiple expense transactions on same day are summed`() {
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                date = LocalDate(2024, 3, 10)
            ),
            TestFixtures.transaction(
                subcategory = TestFixtures.rent,
                amount = 200.0,
                date = LocalDate(2024, 3, 10)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
        assertEquals(300.0, result[10]!!, 0.001)
    }

    @Test
    fun `transactions on different days create separate map entries`() {
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 50.0,
                date = LocalDate(2024, 3, 5)
            ),
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 80.0,
                date = LocalDate(2024, 3, 12)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(2, result.size)
        assertEquals(50.0, result[5]!!, 0.001)
        assertEquals(80.0, result[12]!!, 0.001)
    }

    @Test
    fun `income transactions are excluded from daily totals`() {
        // CalculateDailyTotalUseCase only counts EXPENSE type transactions
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.salary,
                amount = 5000.0,
                date = LocalDate(2024, 3, 1)
            )
        )

        val result = sut(transactions, march2024)

        // The map entry for day 1 exists (grouped by day) but the total should be 0.0
        // because CalculateDailyTotalUseCase filters out income
        val dayTotal = result[1] ?: 0.0
        assertEquals(0.0, dayTotal, 0.001)
    }

    @Test
    fun `tax transactions are excluded from daily totals`() {
        // CalculateDailyTotalUseCase excludes tax transactions (ZUS/PIT) even though they are EXPENSE type
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.zus,
                amount = 500.0,
                date = LocalDate(2024, 3, 20)
            )
        )

        val result = sut(transactions, march2024)

        val dayTotal = result[20] ?: 0.0
        assertEquals(0.0, dayTotal, 0.001)
    }

    @Test
    fun `expense and tax transactions on same day - only expense counted`() {
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 150.0,
                date = LocalDate(2024, 3, 8)
            ),
            TestFixtures.transaction(
                subcategory = TestFixtures.zus,
                amount = 500.0,
                date = LocalDate(2024, 3, 8)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
        assertEquals(150.0, result[8]!!, 0.001)
    }

    @Test
    fun `expense income and tax transactions on same day - only non-tax expense counted`() {
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 200.0,
                date = LocalDate(2024, 3, 15)
            ),
            TestFixtures.transaction(
                subcategory = TestFixtures.salary,
                amount = 5000.0,
                date = LocalDate(2024, 3, 15)
            ),
            TestFixtures.transaction(
                subcategory = TestFixtures.pit,
                amount = 800.0,
                date = LocalDate(2024, 3, 15)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(1, result.size)
        assertEquals(200.0, result[15]!!, 0.001)
    }

    @Test
    fun `USD expense transaction is converted to PLN`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                account = usdAccount,
                date = LocalDate(2024, 3, 5)
            )
        )

        val result = sut(transactions, march2024)

        assertEquals(400.0, result[5]!!, 0.001)
    }

    @Test
    fun `map keys correspond to day-of-month values`() {
        val transactions = listOf(
            TestFixtures.transaction(date = LocalDate(2024, 3, 1)),
            TestFixtures.transaction(date = LocalDate(2024, 3, 15)),
            TestFixtures.transaction(date = LocalDate(2024, 3, 31))
        )

        val result = sut(transactions, march2024)

        assertTrue(result.containsKey(1))
        assertTrue(result.containsKey(15))
        assertTrue(result.containsKey(31))
    }
}

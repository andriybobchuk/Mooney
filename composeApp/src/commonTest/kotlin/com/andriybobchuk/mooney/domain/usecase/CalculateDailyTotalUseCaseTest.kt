package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.data.GlobalConfig
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateDailyTotalUseCase
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CalculateDailyTotalUseCaseTest {

    // Rates: PLN=1.0, USD=0.25, EUR=0.22, UAH=10.0
    // convert(amount, from, to) = amount / fromRate * toRate
    // baseCurrency = PLN (GlobalConfig.baseCurrency)

    private val currencyManager = TestFixtures.currencyManager()
    private lateinit var sut: CalculateDailyTotalUseCase

    private val targetDate = LocalDate(2024, 3, 15)
    private val otherDate = LocalDate(2024, 3, 20)

    @BeforeTest
    fun setup() = runTest {
        GlobalConfig.baseCurrency = Currency.PLN
        currencyManager.refreshExchangeRates()
        sut = CalculateDailyTotalUseCase(currencyManager)
    }

    @Test
    fun `filters by exact date and sums expense transactions`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 50.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(150.0, result, 0.001)
    }

    @Test
    fun `excludes transactions on different dates`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.rent, amount = 1200.0, date = otherDate)
        )

        val result = sut(transactions, targetDate)

        // Only the targetDate transaction counts
        assertEquals(200.0, result, 0.001)
    }

    @Test
    fun `only expense non-tax transactions are summed`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 300.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 500.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 800.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        // ZUS and PIT are excluded
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun `income transactions on same date are excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.salary, amount = 5000.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        // Salary not counted
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `ZUS on target date is excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.zus, amount = 600.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `PIT on target date is excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.pit, amount = 1000.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `foreign currency expense is converted to base PLN`() {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = com.andriybobchuk.mooney.mooney.domain.Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0, account = usdAccount, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(400.0, result, 0.001)
    }

    @Test
    fun `UAH expense is converted to PLN correctly`() {
        // 50 UAH to PLN: 50 / 10.0 * 1.0 = 5 PLN
        val uahAccount = TestFixtures.account(currency = com.andriybobchuk.mooney.mooney.domain.Currency.UAH)
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.utilities, amount = 50.0, account = uahAccount, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun `empty list returns zero`() {
        val result = sut(emptyList(), targetDate)

        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `mixed dates only picks matching date`() {
        val dates = listOf(
            LocalDate(2024, 3, 10),
            LocalDate(2024, 3, 14),
            targetDate,
            LocalDate(2024, 3, 16),
            LocalDate(2024, 3, 31)
        )
        val transactions = dates.mapIndexed { index, date ->
            TestFixtures.transaction(
                id = index,
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                date = date
            )
        }

        val result = sut(transactions, targetDate)

        // Only the single targetDate transaction counts
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `multiple PLN expenses on same day summed correctly`() {
        val transactions = listOf(
            TestFixtures.transaction(id = 1, subcategory = TestFixtures.groceries, amount = 80.0, date = targetDate),
            TestFixtures.transaction(id = 2, subcategory = TestFixtures.utilities, amount = 60.0, date = targetDate),
            TestFixtures.transaction(id = 3, subcategory = TestFixtures.rent, amount = 1200.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(1340.0, result, 0.001)
    }

    @Test
    fun `mixed foreign and PLN expenses on target date`() {
        // 200 PLN + 100 USD (= 400 PLN) = 600 PLN
        val plnAccount = TestFixtures.account(id = 1, currency = com.andriybobchuk.mooney.mooney.domain.Currency.PLN)
        val usdAccount = TestFixtures.account(id = 2, currency = com.andriybobchuk.mooney.mooney.domain.Currency.USD)
        val transactions = listOf(
            TestFixtures.transaction(id = 1, subcategory = TestFixtures.groceries, amount = 200.0, account = plnAccount, date = targetDate),
            TestFixtures.transaction(id = 2, subcategory = TestFixtures.utilities, amount = 100.0, account = usdAccount, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(600.0, result, 0.001)
    }

    @Test
    fun `transfer transactions on target date are excluded`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.internalTransfer, amount = 500.0, date = targetDate),
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 100.0, date = targetDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `date with no transactions returns zero`() {
        val transactions = listOf(
            TestFixtures.transaction(subcategory = TestFixtures.groceries, amount = 200.0, date = otherDate)
        )

        val result = sut(transactions, targetDate)

        assertEquals(0.0, result, 0.001)
    }
}

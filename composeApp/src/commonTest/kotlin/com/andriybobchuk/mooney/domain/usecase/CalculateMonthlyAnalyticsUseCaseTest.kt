package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.CalculateMonthlyAnalyticsUseCase
import com.andriybobchuk.mooney.mooney.domain.usecase.GetTransactionsUseCase
import com.andriybobchuk.mooney.testutil.FakeCoreRepository
import com.andriybobchuk.mooney.testutil.TestFixtures
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalculateMonthlyAnalyticsUseCaseTest {

    // Rates: PLN=1.0, USD=0.25, EUR=0.22, UAH=10.0
    // convert(amount, from, to) = amount / fromRate * toRate

    private lateinit var repository: FakeCoreRepository
    private lateinit var getTransactionsUseCase: GetTransactionsUseCase
    private val currencyManager = TestFixtures.currencyManager()
    private lateinit var sut: CalculateMonthlyAnalyticsUseCase

    // March 2024: [startDate, endDate)
    private val startDate = LocalDate(2024, 3, 1)
    private val endDate = LocalDate(2024, 4, 1)

    @BeforeTest
    fun setup() = runTest {
        repository = FakeCoreRepository()
        getTransactionsUseCase = GetTransactionsUseCase(repository)
        sut = CalculateMonthlyAnalyticsUseCase(getTransactionsUseCase, currencyManager)
        currencyManager.refreshExchangeRates()
    }

    @Test
    fun `revenue includes only income transactions in range`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.salary,
                amount = 5000.0,
                date = LocalDate(2024, 3, 10)
            )
        )
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.groceries,
                amount = 200.0,
                date = LocalDate(2024, 3, 15)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(5000.0, result.totalRevenue, 0.001)
    }

    @Test
    fun `expenses exclude tax ZUS and PIT transactions`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 300.0,
                date = LocalDate(2024, 3, 5)
            )
        )
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.zus,
                amount = 500.0,
                date = LocalDate(2024, 3, 6)
            )
        )
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 3,
                subcategory = TestFixtures.pit,
                amount = 1000.0,
                date = LocalDate(2024, 3, 7)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        // Only groceries counts; ZUS and PIT are excluded
        assertEquals(300.0, result.totalExpenses, 0.001)
    }

    @Test
    fun `transactions outside date range are excluded`() = runTest {
        // February transaction — before range
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 999.0,
                date = LocalDate(2024, 2, 28)
            )
        )
        // April transaction — after range (endDate is exclusive)
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.rent,
                amount = 888.0,
                date = LocalDate(2024, 4, 1)
            )
        )
        // March transaction — in range
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 3,
                subcategory = TestFixtures.utilities,
                amount = 150.0,
                date = LocalDate(2024, 3, 15)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(150.0, result.totalExpenses, 0.001)
    }

    @Test
    fun `top categories grouped by parent category`() = runTest {
        // Two rent transactions — both should roll up to "housing" parent
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.rent,
                amount = 1200.0,
                date = LocalDate(2024, 3, 1)
            )
        )
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.utilities,
                amount = 300.0,
                date = LocalDate(2024, 3, 2)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        // Both subcategories belong to "housing" — should appear as one entry
        assertEquals(1, result.topCategories.size)
        assertEquals(TestFixtures.housing, result.topCategories[0].category)
        assertEquals(1500.0, result.topCategories[0].amount, 0.001)
    }

    @Test
    fun `top categories sorted by amount descending`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 400.0,
                date = LocalDate(2024, 3, 5)
            )
        )
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.rent,
                amount = 1200.0,
                date = LocalDate(2024, 3, 10)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertTrue(result.topCategories.size >= 2)
        assertTrue(result.topCategories[0].amount >= result.topCategories[1].amount)
    }

    @Test
    fun `empty month returns zero revenue and expenses`() = runTest {
        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(0.0, result.totalRevenue, 0.001)
        assertEquals(0.0, result.totalExpenses, 0.001)
        assertTrue(result.topCategories.isEmpty())
        assertTrue(result.transactions.isEmpty())
    }

    @Test
    fun `revenue does not include expense transactions`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 200.0,
                date = LocalDate(2024, 3, 5)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(0.0, result.totalRevenue, 0.001)
    }

    @Test
    fun `expenses do not include income transactions`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.salary,
                amount = 5000.0,
                date = LocalDate(2024, 3, 1)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(0.0, result.totalExpenses, 0.001)
    }

    @Test
    fun `USD income transaction is converted to PLN revenue`() = runTest {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.salary,
                amount = 100.0,
                account = usdAccount,
                date = LocalDate(2024, 3, 1)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(400.0, result.totalRevenue, 0.001)
    }

    @Test
    fun `USD expense transaction is converted to PLN in total`() = runTest {
        // 100 USD to PLN: 100 / 0.25 * 1.0 = 400 PLN
        val usdAccount = TestFixtures.account(currency = Currency.USD)
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                account = usdAccount,
                date = LocalDate(2024, 3, 5)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(400.0, result.totalExpenses, 0.001)
    }

    @Test
    fun `result transactions list contains only in-range transactions`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.groceries,
                amount = 100.0,
                date = LocalDate(2024, 3, 15)
            )
        )
        // Out of range
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 2,
                subcategory = TestFixtures.rent,
                amount = 1200.0,
                date = LocalDate(2024, 2, 1)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(1, result.transactions.size)
        assertEquals(1, result.transactions[0].id)
    }

    @Test
    fun `transfer transactions are not counted as revenue or expenses`() = runTest {
        repository.upsertTransaction(
            TestFixtures.transaction(
                id = 1,
                subcategory = TestFixtures.internalTransfer,
                amount = 500.0,
                date = LocalDate(2024, 3, 5)
            )
        )

        val result = sut(startDate, endDate, Currency.PLN)

        assertEquals(0.0, result.totalRevenue, 0.001)
        assertEquals(0.0, result.totalExpenses, 0.001)
    }
}

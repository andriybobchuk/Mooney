package com.andriybobchuk.mooney.domain.usecase

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.usecase.TransactionValidation
import com.andriybobchuk.mooney.mooney.domain.usecase.ValidateTransactionUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateTransactionUseCaseTest {

    private val sut = ValidateTransactionUseCase()

    private fun account(
        id: Int = 1,
        title: String = "Main",
        amount: Double = 1000.0,
        currency: Currency = Currency.PLN
    ) = Account(
        id = id,
        title = title,
        amount = amount,
        currency = currency,
        emoji = "🏦",
        assetCategory = AssetCategory.BANK_ACCOUNT
    )

    @Test
    fun `zero amount returns error`() {
        val result = sut(0.0, CategoryType.EXPENSE, account())
        assertIs<TransactionValidation.Error>(result)
        assertEquals("Enter an amount", result.message)
    }

    @Test
    fun `negative amount returns error`() {
        val result = sut(-5.0, CategoryType.INCOME, account())
        assertIs<TransactionValidation.Error>(result)
        assertEquals("Enter an amount", result.message)
    }

    @Test
    fun `null source account returns error`() {
        val result = sut(100.0, CategoryType.EXPENSE, null)
        assertIs<TransactionValidation.Error>(result)
        assertEquals("Select an account", result.message)
    }

    @Test
    fun `transfer without destination returns error`() {
        val result = sut(100.0, CategoryType.TRANSFER, account(), null)
        assertIs<TransactionValidation.Error>(result)
        assertEquals("Select a destination account", result.message)
    }

    @Test
    fun `transfer with different currencies returns error`() {
        val source = account(currency = Currency.PLN)
        val dest = account(id = 2, title = "USD Account", currency = Currency.USD)
        val result = sut(100.0, CategoryType.TRANSFER, source, dest)
        assertIs<TransactionValidation.Error>(result)
        assertEquals("Accounts are in different currencies", result.message)
    }

    @Test
    fun `transfer exceeding balance returns warning`() {
        val source = account(amount = 500.0)
        val dest = account(id = 2, title = "Savings", amount = 200.0)
        val result = sut(600.0, CategoryType.TRANSFER, source, dest)
        assertIs<TransactionValidation.Warning>(result)
        assertEquals("Balance on Main will go negative", result.message)
    }

    @Test
    fun `expense exceeding balance returns warning`() {
        val source = account(amount = 100.0)
        val result = sut(150.0, CategoryType.EXPENSE, source)
        assertIs<TransactionValidation.Warning>(result)
        assertEquals("Balance on Main will go negative", result.message)
    }

    @Test
    fun `income never warns even if large`() {
        val source = account(amount = 0.0)
        val result = sut(999999.0, CategoryType.INCOME, source)
        assertIs<TransactionValidation.Valid>(result)
    }

    @Test
    fun `expense at exact balance is valid`() {
        val source = account(amount = 500.0)
        val result = sut(500.0, CategoryType.EXPENSE, source)
        assertIs<TransactionValidation.Valid>(result)
    }
}

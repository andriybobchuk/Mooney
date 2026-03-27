package com.andriybobchuk.mooney.mooney.domain

import com.andriybobchuk.mooney.core.data.database.AccountEntity
import com.andriybobchuk.mooney.core.data.database.TransactionEntity


fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    title = title,
    amount = amount,
    currency = currency.name,
    emoji = emoji,
    assetCategory = assetCategory.name,
    isPrimary = isPrimary
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    subcategoryId = subcategory.id,
    amount = amount,
    accountId = account.id,
    date = date.toString()
)

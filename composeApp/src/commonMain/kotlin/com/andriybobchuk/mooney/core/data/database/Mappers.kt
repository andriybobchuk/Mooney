package com.andriybobchuk.mooney.core.data.database

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalGroup
import com.andriybobchuk.mooney.mooney.domain.Transaction
import kotlinx.datetime.LocalDate

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    title = title,
    amount = amount,
    currency = Currency.valueOf(currency),
    emoji = emoji,
    assetCategory = AssetCategory.fromString(assetCategory)
)

fun TransactionEntity.toDomain(
    subcategory: Category,
    account: Account
): Transaction = Transaction(
    id = id,
    subcategory = subcategory,
    amount = amount,
    account = account,
    date = LocalDate.parse(date)
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    emoji = emoji,
    title = title,
    description = description,
    targetAmount = targetAmount,
    currency = Currency.valueOf(currency),
    createdDate = LocalDate.parse(createdDate),
    groupName = groupName,
    imagePath = imagePath
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    emoji = emoji,
    title = title,
    description = description,
    targetAmount = targetAmount,
    currency = currency.name,
    createdDate = createdDate.toString(),
    groupName = groupName,
    imagePath = imagePath
)

fun GoalGroupEntity.toDomain(): GoalGroup = GoalGroup(
    id = id,
    name = name,
    emoji = emoji,
    color = color,
    createdDate = LocalDate.parse(createdDate)
)

fun GoalGroup.toEntity(): GoalGroupEntity = GoalGroupEntity(
    id = id,
    name = name,
    emoji = emoji,
    color = color,
    createdDate = createdDate.toString()
)

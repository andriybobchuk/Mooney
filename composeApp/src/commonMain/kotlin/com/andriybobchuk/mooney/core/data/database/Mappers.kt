package com.andriybobchuk.mooney.core.data.database

import com.andriybobchuk.mooney.mooney.domain.Account
import com.andriybobchuk.mooney.mooney.domain.AssetCategory
import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType
import com.andriybobchuk.mooney.mooney.domain.Currency
import com.andriybobchuk.mooney.mooney.domain.Goal
import com.andriybobchuk.mooney.mooney.domain.GoalGroup
import com.andriybobchuk.mooney.mooney.domain.GoalTrackingType
import com.andriybobchuk.mooney.mooney.domain.Transaction
import com.andriybobchuk.mooney.mooney.domain.UserCurrency
import kotlinx.datetime.LocalDate

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    title = title,
    amount = amount,
    currency = Currency.valueOf(currency),
    emoji = emoji,
    assetCategory = AssetCategory.fromString(assetCategory),
    assetCategoryId = assetCategory,
    isPrimary = isPrimary,
    isLiability = isLiability
)

fun CategoryEntity.toDomain(parent: Category? = null): Category = Category(
    id = id,
    title = title,
    type = CategoryType.valueOf(type),
    emoji = emoji,
    parent = parent
)

fun UserCurrencyEntity.toDomain(): UserCurrency = UserCurrency(
    code = code,
    sortOrder = sortOrder
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
    imagePath = imagePath,
    trackingType = try { GoalTrackingType.valueOf(trackingType) } catch (_: Exception) { GoalTrackingType.NET_WORTH },
    accountId = accountId
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
    imagePath = imagePath,
    trackingType = trackingType.name,
    accountId = accountId
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

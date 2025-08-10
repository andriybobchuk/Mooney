package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType

object CategoryDataSource {

    // Top-level
    val expense = Category("expense", "Expense", CategoryType.EXPENSE, emoji = "☺\uFE0F")
    val income = Category("income", "Income", CategoryType.INCOME, emoji = "\uD83E\uDD72")

    // Level 2 & 3
    val groceries = Category("groceries", "Groceries & Household", CategoryType.EXPENSE, emoji = "🛒", parent = expense)

    val joy = Category("joy", "Joy", CategoryType.EXPENSE, emoji = "🎮", parent = expense)
    val joySub = listOf(
        Category("joy_purchases", "Purchases", CategoryType.EXPENSE, parent = joy),
        Category("joy_vacation", "Vacation", CategoryType.EXPENSE, parent = joy),
        Category("joy_meetups", "Meetups", CategoryType.EXPENSE, parent = joy),
        Category("joy_dates", "Dates", CategoryType.EXPENSE, parent = joy)
    )

    val business = Category("business", "Business Expense", CategoryType.EXPENSE, emoji = "👨‍💻", parent = expense)
    val businessSub = listOf(
        Category("business_equipment", "Tech/Equipment", CategoryType.EXPENSE, parent = business),
        Category("business_courses", "Courses", CategoryType.EXPENSE, parent = business),
        Category("business_meetups", "Networking/Meetups", CategoryType.EXPENSE, parent = business),
        Category("business_communities", "Paid Communities", CategoryType.EXPENSE, parent = business),
        Category("business_linkedin", "LinkedIn", CategoryType.EXPENSE, parent = business),
        Category("software", "Software Tools", CategoryType.EXPENSE, parent = business),
    )

    val health = Category("health", "Health", CategoryType.EXPENSE, emoji = "❤️", parent = expense)
    val healthSub = listOf(
        Category("health_massage", "Massage", CategoryType.EXPENSE, parent = health),
        Category("health_medications", "Medications", CategoryType.EXPENSE, parent = health),
        Category("health_doctor", "Doctor’s Appointment", CategoryType.EXPENSE, parent = health),
        Category("health_exams", "Examinations", CategoryType.EXPENSE, parent = health)
    )

    val sport = Category("sport", "Sport", CategoryType.EXPENSE, emoji = "💪", parent = expense)
    val sportSub = listOf(
        Category("sport_gym", "Gym", CategoryType.EXPENSE, parent = sport),
        Category("sport_pool", "Pool", CategoryType.EXPENSE, parent = sport),
        Category("sport_equipment", "Equipment", CategoryType.EXPENSE, parent = sport),
        Category("sport_supplements", "Supplements", CategoryType.EXPENSE, parent = sport),
        Category("sport_boxing", "Boxing", CategoryType.EXPENSE, parent = sport)
    )

    val gifts = Category("gifts", "Gifts", CategoryType.EXPENSE, emoji = "🎁", parent = expense)
    val giftsSub = listOf(
        Category("gifts_family", "Family", CategoryType.EXPENSE, parent = gifts),
        Category("gifts_friends", "Friends", CategoryType.EXPENSE, parent = gifts),
        Category("gifts_girlfriend", "Girlfriend", CategoryType.EXPENSE, parent = gifts)
    )

    val housing = Category("housing", "Housing", CategoryType.EXPENSE, emoji = "🏠", parent = expense)
    val housingSub = listOf(
        Category("rent", "Rent", CategoryType.EXPENSE, parent = housing),
        Category("mortgage", "Mortgage", CategoryType.EXPENSE, parent = housing),
        Category("utilities", "Utilities", CategoryType.EXPENSE, parent = housing)
    )

    val tax = Category("tax", "Tax", CategoryType.EXPENSE, emoji = "🏦", parent = expense)
    val taxSub = listOf(
        Category("zus", "ZUS", CategoryType.EXPENSE, parent = tax),
        Category("pit", "PIT", CategoryType.EXPENSE, parent = tax),
        Category("gov_fee", "Government Fee", CategoryType.EXPENSE, parent = tax),
        Category("fine", "Fine", CategoryType.EXPENSE, parent = tax),
    )

    val transport = Category("transport", "Transportation", CategoryType.EXPENSE, emoji = "🚲", parent = expense)
    val transportSub = listOf(
        Category("transport_bike", "City Bike", CategoryType.EXPENSE, parent = transport),
        Category("transport_train", "Train", CategoryType.EXPENSE, parent = transport),
        Category("transport_metro", "Metro & Bus & Tram", CategoryType.EXPENSE, parent = transport),
        Category("transport_taxi", "Taxi", CategoryType.EXPENSE, parent = transport)
    )

    val travelling = Category("travelling", "Travelling", CategoryType.EXPENSE, emoji = "\uD83C\uDFDD\uFE0F", parent = expense)
    val travellingSub = listOf(
        Category("accommodation", "Accommodation", CategoryType.EXPENSE, parent = transport),
        Category("transport", "Local Transport", CategoryType.EXPENSE, parent = travelling),
        Category("flights", "Flights", CategoryType.EXPENSE, parent = travelling),
        Category("food_drinks", "Food & Drinks", CategoryType.EXPENSE, parent = travelling),
        Category("travelling_groceries", "Groceries", CategoryType.EXPENSE, parent = travelling),
        Category("activities", "Activities", CategoryType.EXPENSE, parent = travelling),
        Category("tickets", "Attractions & Tickets", CategoryType.EXPENSE, parent = travelling),
        Category("souvenirs", "Souvenirs", CategoryType.EXPENSE, parent = travelling),
        Category("shopping", "Shopping", CategoryType.EXPENSE, parent = travelling),
        Category("luggage", "Luggage", CategoryType.EXPENSE, parent = travelling),
        Category("airbnb_rent", "Accommodation", CategoryType
            .EXPENSE, parent = travelling),
    )

    val barber = Category("barber", "Barber", CategoryType.EXPENSE, emoji = "💈", parent = expense)

    val clothing = Category("clothing", "Clothing", CategoryType.EXPENSE, emoji = "👕", parent = expense)
    val clothingSub = listOf(
        Category("shoes", "Shoes", CategoryType.EXPENSE, parent = clothing)
    )

    val reconciliation = Category("reconciliation", "Account Reconciliation", CategoryType.EXPENSE, emoji = "💱", parent = expense)

    val subscriptions = Category("subscriptions", "Subscriptions", CategoryType.EXPENSE, emoji = "🎧", parent = expense)
    val subscriptionsSub = listOf(
        Category("spotify", "Spotify", CategoryType.EXPENSE, parent = subscriptions),
        Category("internet", "Phone & Internet", CategoryType.EXPENSE, parent = subscriptions),
        Category("apple", "Apple", CategoryType.EXPENSE, parent = subscriptions)
    )

    val beverages = Category("beverages", "Beverages", CategoryType.EXPENSE, emoji = "🥙", parent = expense)
    val beveragesSub = listOf(
        Category("pubs", "Pubs", CategoryType.EXPENSE, parent = beverages),
        Category("eating_out", "Eating Out", CategoryType.EXPENSE, parent = beverages),
        Category("soft_drinks", "Soft Drinks & Snacks", CategoryType.EXPENSE, parent = beverages)
    )

    // Income
    val salary = Category("salary", "Salary", CategoryType.INCOME, emoji = "💸", parent = income)
    val positive_reconciliation =
        Category("positive_reconciliation", "Account Reconciliation", CategoryType.INCOME, emoji = "💸", parent = income)
    val tax_return = Category("tax_return", "Tax Return", CategoryType.INCOME, emoji = "💸", parent = income)
    val refund = Category("refund", "Refund", CategoryType.INCOME, emoji = "💸", parent = income)
    val repayment = Category("repayment", "Repayment", CategoryType.INCOME, emoji = "💸", parent = income)


    // Final list
    val categories: List<Category> = buildList {
        addAll(
            listOf(
                expense, income,
                groceries,
                joy, business, health, sport, gifts,
                housing, tax, transport,
                barber, clothing, reconciliation,
                subscriptions, beverages, travelling,
                salary, positive_reconciliation, tax_return, refund, repayment
            )
        )
        addAll(joySub)
        addAll(businessSub)
        addAll(healthSub)
        addAll(sportSub)
        addAll(giftsSub)
        addAll(housingSub)
        addAll(taxSub)
        addAll(transportSub)
        addAll(clothingSub)
        addAll(subscriptionsSub)
        addAll(beveragesSub)
        addAll(travellingSub)
    }
}

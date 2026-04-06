package com.andriybobchuk.mooney.mooney.data

import com.andriybobchuk.mooney.mooney.domain.Category
import com.andriybobchuk.mooney.mooney.domain.CategoryType

/**
 * Reference copy of the default category tree for NEW users.
 * The actual source of truth is the Room DB (seeded via SEED_DATABASE_CALLBACK).
 * This file exists as documentation — it is NOT used at runtime.
 *
 * Existing users who upgraded from older versions may have the legacy categories
 * (barber, reconciliation, joy_dates, etc.) from historical migrations.
 */
object CategoryDataSource {

    // Top-level
    val expense = Category("expense", "Expense", CategoryType.EXPENSE, emoji = "☺\uFE0F")
    val income = Category("income", "Income", CategoryType.INCOME, emoji = "\uD83E\uDD72")
    val transfer = Category("transfer", "Transfer", CategoryType.TRANSFER, emoji = "↔️")

    // Transfer
    val internalTransfer = Category("internal_transfer", "Internal Transfer", CategoryType.TRANSFER, emoji = "🔄", parent = transfer)

    // ── EXPENSE: General categories ──

    val groceries = Category("groceries", "Groceries & Household", CategoryType.EXPENSE, emoji = "🛒", parent = expense)

    val diningAndDrinks = Category("beverages", "Dining & Drinks", CategoryType.EXPENSE, emoji = "🍽️", parent = expense)
    val diningAndDrinksSub = listOf(
        Category("eating_out", "Eating Out", CategoryType.EXPENSE, parent = diningAndDrinks),
        Category("pubs", "Pubs & Bars", CategoryType.EXPENSE, parent = diningAndDrinks),
        Category("soft_drinks", "Soft Drinks & Snacks", CategoryType.EXPENSE, parent = diningAndDrinks)
    )

    val housing = Category("housing", "Housing", CategoryType.EXPENSE, emoji = "🏠", parent = expense)
    val housingSub = listOf(
        Category("rent", "Rent", CategoryType.EXPENSE, parent = housing),
        Category("mortgage", "Mortgage", CategoryType.EXPENSE, parent = housing),
        Category("utilities", "Utilities", CategoryType.EXPENSE, parent = housing)
    )

    val transport = Category("transport", "Transportation", CategoryType.EXPENSE, emoji = "🚲", parent = expense)
    val transportSub = listOf(
        Category("transport_metro", "Public Transit", CategoryType.EXPENSE, parent = transport),
        Category("transport_taxi", "Taxi & Rideshare", CategoryType.EXPENSE, parent = transport),
        Category("transport_bike", "Bike & Scooter", CategoryType.EXPENSE, parent = transport),
        Category("transport_train", "Train", CategoryType.EXPENSE, parent = transport)
    )

    val car = Category("car", "Car & Vehicle", CategoryType.EXPENSE, emoji = "🚗", parent = expense)
    val carSub = listOf(
        Category("car_fuel", "Fuel", CategoryType.EXPENSE, parent = car),
        Category("car_parking", "Parking", CategoryType.EXPENSE, parent = car),
        Category("car_maintenance", "Maintenance & Repairs", CategoryType.EXPENSE, parent = car),
        Category("car_wash", "Car Wash", CategoryType.EXPENSE, parent = car)
    )

    val subscriptions = Category("subscriptions", "Subscriptions", CategoryType.EXPENSE, emoji = "🎧", parent = expense)
    val subscriptionsSub = listOf(
        Category("spotify", "Music & Streaming", CategoryType.EXPENSE, parent = subscriptions),
        Category("internet", "Phone & Internet", CategoryType.EXPENSE, parent = subscriptions),
        Category("apple", "Cloud & Storage", CategoryType.EXPENSE, parent = subscriptions),
        Category("software", "Software & Apps", CategoryType.EXPENSE, parent = subscriptions)
    )

    val health = Category("health", "Health", CategoryType.EXPENSE, emoji = "❤️", parent = expense)
    val healthSub = listOf(
        Category("health_doctor", "Doctor", CategoryType.EXPENSE, parent = health),
        Category("health_medications", "Medications", CategoryType.EXPENSE, parent = health),
        Category("health_exams", "Examinations", CategoryType.EXPENSE, parent = health)
    )

    val sport = Category("sport", "Sport", CategoryType.EXPENSE, emoji = "💪", parent = expense)
    val sportSub = listOf(
        Category("sport_gym", "Gym", CategoryType.EXPENSE, parent = sport),
        Category("sport_equipment", "Equipment", CategoryType.EXPENSE, parent = sport),
        Category("sport_supplements", "Supplements", CategoryType.EXPENSE, parent = sport)
    )

    val personalCare = Category("personal_care", "Personal Care", CategoryType.EXPENSE, emoji = "✨", parent = expense)
    val personalCareSub = listOf(
        Category("personal_care_haircut", "Haircut", CategoryType.EXPENSE, parent = personalCare),
        Category("personal_care_skincare", "Skincare & Beauty", CategoryType.EXPENSE, parent = personalCare)
    )

    val clothing = Category("clothing", "Clothing", CategoryType.EXPENSE, emoji = "👕", parent = expense)
    val clothingSub = listOf(
        Category("shoes", "Shoes", CategoryType.EXPENSE, parent = clothing)
    )

    val education = Category("education", "Education", CategoryType.EXPENSE, emoji = "🎓", parent = expense)
    val educationSub = listOf(
        Category("education_tuition", "Tuition", CategoryType.EXPENSE, parent = education),
        Category("education_courses", "Courses", CategoryType.EXPENSE, parent = education),
        Category("books", "Books", CategoryType.EXPENSE, parent = education)
    )

    val joy = Category("joy", "Entertainment", CategoryType.EXPENSE, emoji = "🎮", parent = expense)
    val joySub = listOf(
        Category("joy_purchases", "Purchases", CategoryType.EXPENSE, parent = joy),
        Category("joy_meetups", "Meetups & Events", CategoryType.EXPENSE, parent = joy)
    )

    val gifts = Category("gifts", "Gifts", CategoryType.EXPENSE, emoji = "🎁", parent = expense)
    val giftsSub = listOf(
        Category("gifts_family", "Family", CategoryType.EXPENSE, parent = gifts),
        Category("gifts_friends", "Friends", CategoryType.EXPENSE, parent = gifts),
        Category("gifts_girlfriend", "Partner", CategoryType.EXPENSE, parent = gifts)
    )

    val kids = Category("kids", "Kids & Family", CategoryType.EXPENSE, emoji = "👶", parent = expense)
    val kidsSub = listOf(
        Category("kids_childcare", "Childcare", CategoryType.EXPENSE, parent = kids),
        Category("kids_school", "School", CategoryType.EXPENSE, parent = kids),
        Category("kids_activities", "Activities", CategoryType.EXPENSE, parent = kids)
    )

    val pets = Category("pets", "Pets", CategoryType.EXPENSE, emoji = "🐾", parent = expense)
    val petsSub = listOf(
        Category("pets_vet", "Vet", CategoryType.EXPENSE, parent = pets),
        Category("pets_food", "Pet Food", CategoryType.EXPENSE, parent = pets),
        Category("pets_grooming", "Grooming", CategoryType.EXPENSE, parent = pets)
    )

    val insurance = Category("insurance", "Insurance", CategoryType.EXPENSE, emoji = "🛡️", parent = expense)
    val insuranceSub = listOf(
        Category("insurance_health", "Health Insurance", CategoryType.EXPENSE, parent = insurance),
        Category("insurance_car", "Car Insurance", CategoryType.EXPENSE, parent = insurance),
        Category("insurance_home", "Home Insurance", CategoryType.EXPENSE, parent = insurance),
        Category("insurance_life", "Life Insurance", CategoryType.EXPENSE, parent = insurance)
    )

    val tax = Category("tax", "Tax", CategoryType.EXPENSE, emoji = "🏦", parent = expense)
    val taxSub = listOf(
        Category("pit", "Income Tax", CategoryType.EXPENSE, parent = tax),
        Category("zus", "Social Security", CategoryType.EXPENSE, parent = tax),
        Category("gov_fee", "Government Fee", CategoryType.EXPENSE, parent = tax),
        Category("fine", "Fine", CategoryType.EXPENSE, parent = tax)
    )

    val business = Category("business", "Business Expense", CategoryType.EXPENSE, emoji = "👨‍💻", parent = expense)
    val businessSub = listOf(
        Category("business_equipment", "Equipment", CategoryType.EXPENSE, parent = business),
        Category("business_courses", "Courses & Training", CategoryType.EXPENSE, parent = business),
        Category("business_meetups", "Networking", CategoryType.EXPENSE, parent = business)
    )

    val travelling = Category("travelling", "Travelling", CategoryType.EXPENSE, emoji = "\uD83C\uDFDD\uFE0F", parent = expense)
    val travellingSub = listOf(
        Category("accommodation", "Accommodation", CategoryType.EXPENSE, parent = travelling),
        Category("flights", "Flights", CategoryType.EXPENSE, parent = travelling),
        Category("travelling_transport", "Local Transport", CategoryType.EXPENSE, parent = travelling),
        Category("food_drinks", "Food & Drinks", CategoryType.EXPENSE, parent = travelling),
        Category("tickets", "Attractions & Tickets", CategoryType.EXPENSE, parent = travelling),
        Category("shopping", "Shopping", CategoryType.EXPENSE, parent = travelling)
    )

    val charity = Category("charity", "Charity & Donations", CategoryType.EXPENSE, emoji = "💝", parent = expense)

    val reconciliation = Category("reconciliation", "Account Reconciliation", CategoryType.EXPENSE, emoji = "💱", parent = expense)

    // ── INCOME ──

    val salary = Category("salary", "Salary", CategoryType.INCOME, emoji = "💸", parent = income)
    val salarySub = listOf(
        Category("primary_job", "Primary Job", CategoryType.INCOME, parent = salary),
        Category("side_income", "Side Income", CategoryType.INCOME, parent = salary),
        Category("freelance", "Freelance", CategoryType.INCOME, parent = salary)
    )

    val investments = Category("investments", "Investments", CategoryType.INCOME, emoji = "📈", parent = income)
    val investmentsSub = listOf(
        Category("dividends", "Dividends", CategoryType.INCOME, parent = investments),
        Category("interest", "Interest", CategoryType.INCOME, parent = investments),
        Category("capital_gains", "Capital Gains", CategoryType.INCOME, parent = investments)
    )

    val businessIncome = Category("business_income", "Business Income", CategoryType.INCOME, emoji = "💼", parent = income)
    val rentalIncome = Category("rental_income", "Rental Income", CategoryType.INCOME, emoji = "🏠", parent = income)
    val giftsReceived = Category("gifts_received", "Gifts Received", CategoryType.INCOME, emoji = "🎁", parent = income)
    val taxReturn = Category("tax_return", "Tax Return", CategoryType.INCOME, emoji = "💸", parent = income)
    val refund = Category("refund", "Refund", CategoryType.INCOME, emoji = "💸", parent = income)
    val repayment = Category("repayment", "Repayment", CategoryType.INCOME, emoji = "💸", parent = income)
    val positiveReconciliation = Category("positive_reconciliation", "Account Reconciliation", CategoryType.INCOME, emoji = "💸", parent = income)

    val categories: List<Category> = buildList {
        addAll(
            listOf(
                expense, income, transfer,
                internalTransfer,
                groceries, diningAndDrinks, housing, transport, car, subscriptions,
                health, sport, personalCare, clothing, education, joy, gifts,
                kids, pets, insurance, tax, business, travelling, charity, reconciliation,
                salary, investments, businessIncome, rentalIncome, giftsReceived,
                taxReturn, refund, repayment, positiveReconciliation
            )
        )
        addAll(diningAndDrinksSub)
        addAll(housingSub)
        addAll(transportSub)
        addAll(carSub)
        addAll(subscriptionsSub)
        addAll(healthSub)
        addAll(sportSub)
        addAll(personalCareSub)
        addAll(clothingSub)
        addAll(educationSub)
        addAll(joySub)
        addAll(giftsSub)
        addAll(kidsSub)
        addAll(petsSub)
        addAll(insuranceSub)
        addAll(taxSub)
        addAll(businessSub)
        addAll(travellingSub)
        addAll(salarySub)
        addAll(investmentsSub)
    }
}

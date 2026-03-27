package com.andriybobchuk.mooney.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migration from version 1 to 2
 * Adds the category_usage table to track frequently used categories
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `category_usage` (
                `categoryId` TEXT NOT NULL,
                `usageCount` INTEGER NOT NULL,
                `lastUsedDate` TEXT NOT NULL,
                PRIMARY KEY(`categoryId`)
            )
            """
        )
    }
}

/**
 * Migration from version 2 to 3
 * Adds the goals table to track financial goals
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goals` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `emoji` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `targetAmount` REAL NOT NULL,
                `currency` TEXT NOT NULL,
                `createdDate` TEXT NOT NULL
            )
            """
        )
    }
}


/**
 * Migration from version 3 to 4
 * Adds groupId and imagePath columns to goals table
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            ALTER TABLE `goals` ADD COLUMN `groupId` INTEGER NOT NULL DEFAULT 0
            """
        )
        connection.execSQL(
            """
            ALTER TABLE `goals` ADD COLUMN `imagePath` TEXT
            """
        )
    }
}

/**
 * Migration from version 4 to 5
 * Adds goal_groups table for organizing goals
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `createdDate` TEXT NOT NULL
            )
            """
        )
    }
}

/**
 * Migration from version 5 to 6
 * Convert groupId to groupName to match actual database structure
 * This is a safety migration to handle any schema mismatches
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        // Check if goals table has groupId column and convert to groupName
        try {
            // If the table has groupId, rename it to groupName
            connection.execSQL("ALTER TABLE goals RENAME COLUMN groupId TO groupName")
        } catch (e: Exception) {
            // If groupId doesn't exist, add groupName column if it doesn't exist
            try {
                connection.execSQL("ALTER TABLE goals ADD COLUMN groupName TEXT NOT NULL DEFAULT 'General'")
            } catch (e2: Exception) {
                // Column already exists, do nothing
            }
        }
        
        // Ensure goal_groups table exists
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `createdDate` TEXT NOT NULL
            )
            """
        )
    }
}

/**
 * Migration from version 6 to 7
 * Adds assetCategory column to AccountEntity table for asset diversification tracking
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            ALTER TABLE AccountEntity ADD COLUMN assetCategory TEXT NOT NULL DEFAULT 'BANK_ACCOUNT'
            """
        )
    }
}

/**
 * Migration from version 7 to 8
 * Adds recurring_transactions and pending_transactions tables for recurring transaction feature
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        // Create recurring_transactions table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `subcategoryId` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `accountId` INTEGER NOT NULL,
                `dayOfMonth` INTEGER NOT NULL,
                `frequency` TEXT NOT NULL,
                `weekDay` INTEGER,
                `monthOfYear` INTEGER,
                `isActive` INTEGER NOT NULL,
                `createdDate` TEXT NOT NULL,
                `lastProcessedDate` TEXT
            )
            """
        )

        // Create pending_transactions table
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recurringTransactionId` INTEGER NOT NULL,
                `subcategoryId` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `accountId` INTEGER NOT NULL,
                `scheduledDate` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `createdDate` TEXT NOT NULL
            )
            """
        )
    }
}

/**
 * Migration from version 8 to 9
 * Adds isPrimary column to AccountEntity, creates user_currencies and categories tables,
 * and pre-populates them with default data.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE AccountEntity ADD COLUMN isPrimary INTEGER NOT NULL DEFAULT 0"
        )
        createUserCurrenciesTable(connection)
        createCategoriesTable(connection)
    }

    private fun createUserCurrenciesTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_currencies` (
                `code` TEXT NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`code`)
            )
            """
        )
        connection.execSQL("INSERT OR IGNORE INTO user_currencies (code, sortOrder) VALUES ('PLN', 0)")
        connection.execSQL("INSERT OR IGNORE INTO user_currencies (code, sortOrder) VALUES ('USD', 1)")
        connection.execSQL("INSERT OR IGNORE INTO user_currencies (code, sortOrder) VALUES ('EUR', 2)")
        connection.execSQL("INSERT OR IGNORE INTO user_currencies (code, sortOrder) VALUES ('UAH', 3)")
    }

    @Suppress("LongMethod")
    private fun createCategoriesTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `emoji` TEXT,
                `parentId` TEXT,
                PRIMARY KEY(`id`)
            )
            """
        )
        insertTopLevelCategories(connection)
        insertExpenseSubcategories(connection)
        insertIncomeCategories(connection)
    }

    private fun insertTopLevelCategories(connection: SQLiteConnection) {
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('expense', 'Expense', 'EXPENSE', '☺️', NULL)")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('income', 'Income', 'INCOME', '\uD83E\uDD72', NULL)")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transfer', 'Transfer', 'TRANSFER', '↔️', NULL)")

        // Expense general categories
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('groceries', 'Groceries & Household', 'EXPENSE', '🛒', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy', 'Joy', 'EXPENSE', '🎮', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business', 'Business Expense', 'EXPENSE', '👨‍💻', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health', 'Health', 'EXPENSE', '❤️', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport', 'Sport', 'EXPENSE', '💪', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts', 'Gifts', 'EXPENSE', '🎁', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('housing', 'Housing', 'EXPENSE', '🏠', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tax', 'Tax', 'EXPENSE', '🏦', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport', 'Transportation', 'EXPENSE', '🚲', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling', 'Travelling', 'EXPENSE', '\uD83C\uDFDD\uFE0F', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('barber', 'Barber', 'EXPENSE', '💈', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('clothing', 'Clothing', 'EXPENSE', '👕', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('reconciliation', 'Account Reconciliation', 'EXPENSE', '💱', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('subscriptions', 'Subscriptions', 'EXPENSE', '🎧', 'expense')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('beverages', 'Beverages', 'EXPENSE', '🥙', 'expense')")

        // Transfer general category
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('internal_transfer', 'Internal Transfer', 'TRANSFER', '🔄', 'transfer')")
    }

    @Suppress("LongMethod")
    private fun insertExpenseSubcategories(connection: SQLiteConnection) {
        // Joy
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_purchases', 'Purchases', 'EXPENSE', NULL, 'joy')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_vacation', 'Vacation', 'EXPENSE', NULL, 'joy')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_meetups', 'Meetups', 'EXPENSE', NULL, 'joy')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_dates', 'Dates', 'EXPENSE', NULL, 'joy')")
        // Business
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_equipment', 'Tech/Equipment', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_courses', 'Courses', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_meetups', 'Networking/Meetups', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_communities', 'Paid Communities', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_linkedin', 'LinkedIn', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('software', 'Software Tools', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('employees', 'Employees', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('ai_assistants', 'AI Assistants', 'EXPENSE', NULL, 'business')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('books', 'Books', 'EXPENSE', NULL, 'business')")
        // Health
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_massage', 'Massage', 'EXPENSE', NULL, 'health')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_medications', 'Medications', 'EXPENSE', NULL, 'health')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_doctor', 'Doctor''s Appointment', 'EXPENSE', NULL, 'health')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_exams', 'Examinations', 'EXPENSE', NULL, 'health')")
        // Sport
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_gym', 'Gym', 'EXPENSE', NULL, 'sport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_pool', 'Pool', 'EXPENSE', NULL, 'sport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_equipment', 'Equipment', 'EXPENSE', NULL, 'sport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_supplements', 'Supplements', 'EXPENSE', NULL, 'sport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_boxing', 'Boxing', 'EXPENSE', NULL, 'sport')")
        // Gifts
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_family', 'Family', 'EXPENSE', NULL, 'gifts')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_friends', 'Friends', 'EXPENSE', NULL, 'gifts')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_girlfriend', 'Girlfriend', 'EXPENSE', NULL, 'gifts')")
        // Housing
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('rent', 'Rent', 'EXPENSE', NULL, 'housing')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('mortgage', 'Mortgage', 'EXPENSE', NULL, 'housing')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('utilities', 'Utilities', 'EXPENSE', NULL, 'housing')")
        // Tax
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('zus', 'ZUS', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pit', 'PIT', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gov_fee', 'Government Fee', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('fine', 'Fine', 'EXPENSE', NULL, 'tax')")
        // Transport
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_bike', 'City Bike', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_train', 'Train', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_metro', 'Metro & Bus & Tram', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_taxi', 'Taxi', 'EXPENSE', NULL, 'transport')")
        // Travelling
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('accommodation', 'Accommodation', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling_transport', 'Local Transport', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('flights', 'Flights', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('food_drinks', 'Food & Drinks', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling_groceries', 'Groceries', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('activities', 'Activities', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tickets', 'Attractions & Tickets', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('souvenirs', 'Souvenirs', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shopping', 'Shopping', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('luggage', 'Luggage', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('airbnb_rent', 'Accommodation', 'EXPENSE', NULL, 'travelling')")
        // Clothing
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shoes', 'Shoes', 'EXPENSE', NULL, 'clothing')")
        // Subscriptions
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('spotify', 'Spotify', 'EXPENSE', NULL, 'subscriptions')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('internet', 'Phone & Internet', 'EXPENSE', NULL, 'subscriptions')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('apple', 'Apple', 'EXPENSE', NULL, 'subscriptions')")
        // Beverages
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pubs', 'Pubs', 'EXPENSE', NULL, 'beverages')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('eating_out', 'Eating Out', 'EXPENSE', NULL, 'beverages')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('soft_drinks', 'Soft Drinks & Snacks', 'EXPENSE', NULL, 'beverages')")
    }

    private fun insertIncomeCategories(connection: SQLiteConnection) {
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('salary', 'Salary', 'INCOME', '💸', 'income')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('positive_reconciliation', 'Account Reconciliation', 'INCOME', '💸', 'income')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tax_return', 'Tax Return', 'INCOME', '💸', 'income')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('refund', 'Refund', 'INCOME', '💸', 'income')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('repayment', 'Repayment', 'INCOME', '💸', 'income')")
        // Salary subcategories
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('effectivesoft', 'EffectiveSoft', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('unikie', 'Unikie', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('squareone', 'SquareOne', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('usoftware', 'USoftware', 'INCOME', NULL, 'salary')")
    }
}


package com.andriybobchuk.mooney.core.data.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Checks if a column exists in a table using PRAGMA table_info.
 * Safe to call on any platform (Android, iOS).
 */
private fun hasColumn(connection: SQLiteConnection, table: String, column: String): Boolean {
    val stmt = connection.prepare("PRAGMA table_info(`$table`)")
    try {
        while (stmt.step()) {
            if (stmt.getText(1) == column) return true
        }
    } finally {
        stmt.close()
    }
    return false
}

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
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('beverages', 'Dining & Drinks', 'EXPENSE', '🍽️', 'expense')")

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
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_equipment', 'Equipment', 'EXPENSE', NULL, 'sport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_supplements', 'Supplements', 'EXPENSE', NULL, 'sport')")
        // Gifts
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_family', 'Family', 'EXPENSE', NULL, 'gifts')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_friends', 'Friends', 'EXPENSE', NULL, 'gifts')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_girlfriend', 'Partner', 'EXPENSE', NULL, 'gifts')")
        // Housing
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('rent', 'Rent', 'EXPENSE', NULL, 'housing')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('mortgage', 'Mortgage', 'EXPENSE', NULL, 'housing')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('utilities', 'Utilities', 'EXPENSE', NULL, 'housing')")
        // Tax
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('zus', 'Social Security', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pit', 'Income Tax', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gov_fee', 'Government Fee', 'EXPENSE', NULL, 'tax')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('fine', 'Fine', 'EXPENSE', NULL, 'tax')")
        // Transport
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_bike', 'City Bike', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_train', 'Train', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_metro', 'Metro & Bus & Tram', 'EXPENSE', NULL, 'transport')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_taxi', 'Taxi', 'EXPENSE', NULL, 'transport')")
        // Travelling
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('accommodation', 'Accommodation', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling_transport', 'Local Transport', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('flights', 'Flights', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('food_drinks', 'Food & Drinks', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling_groceries', 'Groceries', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('activities', 'Activities', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tickets', 'Attractions & Tickets', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('souvenirs', 'Souvenirs', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shopping', 'Shopping', 'EXPENSE', NULL, 'travelling')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('luggage', 'Luggage', 'EXPENSE', NULL, 'travelling')")
        // Clothing
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shoes', 'Shoes', 'EXPENSE', NULL, 'clothing')")
        // Subscriptions
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('spotify', 'Music', 'EXPENSE', NULL, 'subscriptions')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('internet', 'Phone & Internet', 'EXPENSE', NULL, 'subscriptions')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('apple', 'Cloud & Storage', 'EXPENSE', NULL, 'subscriptions')")
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
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('primary_job', 'Primary Job', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('side_income', 'Side Income', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('freelance', 'Freelance', 'INCOME', NULL, 'salary')")
    }
}

/**
 * Migration from version 9 to 10
 * Adds generic salary subcategories for new public release.
 * Existing users keep all their categories untouched — these are additive only.
 * New users get clean categories from MIGRATION_8_9 (which was updated in this release).
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        // Add generic salary subcategories (INSERT OR IGNORE = safe for all users)
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('primary_job', 'Primary Job', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('side_income', 'Side Income', 'INCOME', NULL, 'salary')")
        connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('freelance', 'Freelance', 'INCOME', NULL, 'salary')")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `asset_categories` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `emoji` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT '',
                `color` INTEGER NOT NULL DEFAULT ${0xFF3562F6},
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
        """)

        // Seed with the 11 default categories matching existing enum names
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('BANK_ACCOUNT', 'Bank Account', '🏦', 'Traditional bank accounts and deposits', ${0xFF4285F4}, 0)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('CASH', 'Cash Reserve', '💵', 'Physical cash holdings', ${0xFF34A853}, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('REAL_ESTATE', 'Real Estate', '🏠', 'Property and real estate investments', ${0xFF795548}, 2)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('STOCKS', 'Stocks', '📈', 'Stock market investments', ${0xFFE91E63}, 3)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('BONDS', 'Bonds', '📜', 'Government and corporate bonds', ${0xFF9C27B0}, 4)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('CRYPTO', 'Cryptocurrency', '₿', 'Digital assets and cryptocurrencies', ${0xFFF57C00}, 5)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('PRECIOUS_METALS', 'Precious Metals', '🥇', 'Gold, silver, and other precious metals', ${0xFFFFD700}, 6)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('RETIREMENT', 'Retirement Fund', '🏖️', '401k, IRA, pension funds', ${0xFF00BCD4}, 7)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('BUSINESS', 'Business Assets', '💼', 'Business ownership and investments', ${0xFF607D8B}, 8)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('COLLECTIBLES', 'Collectibles', '🎨', 'Art, antiques, and collectible items', ${0xFFFF5722}, 9)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder) VALUES ('OTHER', 'Other Assets', '📦', 'Miscellaneous assets', ${0xFF9E9E9E}, 10)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(connection: SQLiteConnection) {
        // Add isLiability to AccountEntity (only if not already present)
        if (!hasColumn(connection, "AccountEntity", "isLiability")) {
            connection.execSQL(
                "ALTER TABLE AccountEntity ADD COLUMN isLiability INTEGER NOT NULL DEFAULT 0"
            )
        }
        // Add isLiability to asset_categories (only if not already present)
        if (!hasColumn(connection, "asset_categories", "isLiability")) {
            connection.execSQL(
                "ALTER TABLE asset_categories ADD COLUMN isLiability INTEGER NOT NULL DEFAULT 0"
            )
        }
        // Seed default liability categories
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('MORTGAGE', 'Mortgage', '🏠', '', ${0xFFE53935}, 0, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('LOAN', 'Loan', '📋', '', ${0xFFFF7043}, 1, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('CREDIT_CARD', 'Credit Card', '💳', '', ${0xFFAB47BC}, 2, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('DEBT', 'Debt', '📉', '', ${0xFF78909C}, 3, 1)")
    }
}

/**
 * Safety migration for devices that may have been at v12 during development
 * without the isLiability columns. Uses hasColumn checks to be fully idempotent.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        if (!hasColumn(connection, "AccountEntity", "isLiability")) {
            connection.execSQL(
                "ALTER TABLE AccountEntity ADD COLUMN isLiability INTEGER NOT NULL DEFAULT 0"
            )
        }
        if (!hasColumn(connection, "asset_categories", "isLiability")) {
            connection.execSQL(
                "ALTER TABLE asset_categories ADD COLUMN isLiability INTEGER NOT NULL DEFAULT 0"
            )
        }
        // Seed default liability categories (safe — INSERT OR IGNORE)
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('MORTGAGE', 'Mortgage', '🏠', '', ${0xFFE53935}, 0, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('LOAN', 'Loan', '📋', '', ${0xFFFF7043}, 1, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('CREDIT_CARD', 'Credit Card', '💳', '', ${0xFFAB47BC}, 2, 1)")
        connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('DEBT', 'Debt', '📉', '', ${0xFF78909C}, 3, 1)")
    }
}

/**
 * Migration from version 13 to 14
 * Adds trackingType and accountId columns to goals table for the redesigned Goals feature
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(connection: SQLiteConnection) {
        if (!hasColumn(connection, "goals", "trackingType")) {
            connection.execSQL(
                "ALTER TABLE goals ADD COLUMN trackingType TEXT NOT NULL DEFAULT 'NET_WORTH'"
            )
        }
        if (!hasColumn(connection, "goals", "accountId")) {
            connection.execSQL(
                "ALTER TABLE goals ADD COLUMN accountId INTEGER"
            )
        }
    }
}

/**
 * Room callback that seeds default data on fresh install (onCreate).
 * Migrations only run when upgrading an existing DB — a brand new DB at version 14
 * would have empty categories, asset_categories, and user_currencies tables without this.
 */
val SEED_DATABASE_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        super.onCreate(connection)
        seedTransactionCategories(connection)
        seedAssetCategories(connection)
    }
}

/**
 * Inserts all default transaction categories for fresh installs.
 * Uses INSERT OR IGNORE so it's safe to call on existing data.
 *
 * NOTE: Historical migrations (MIGRATION_8_9 etc.) are NOT updated — they only affect
 * users upgrading from old versions. This function defines what NEW users get.
 */
@Suppress("LongMethod")
fun seedTransactionCategories(connection: SQLiteConnection) {
    // ── Top-level types ──
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('expense', 'Expense', 'EXPENSE', '☺️', NULL)")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('income', 'Income', 'INCOME', '\uD83E\uDD72', NULL)")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transfer', 'Transfer', 'TRANSFER', '↔️', NULL)")

    // ── Transfer ──
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('internal_transfer', 'Internal Transfer', 'TRANSFER', '🔄', 'transfer')")

    // ── EXPENSE: general categories ──
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('groceries', 'Groceries & Household', 'EXPENSE', '🛒', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('beverages', 'Dining & Drinks', 'EXPENSE', '🍽️', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('housing', 'Housing', 'EXPENSE', '🏠', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport', 'Transportation', 'EXPENSE', '🚲', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('car', 'Car & Vehicle', 'EXPENSE', '🚗', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('subscriptions', 'Subscriptions', 'EXPENSE', '🎧', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health', 'Health', 'EXPENSE', '❤️', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport', 'Sport', 'EXPENSE', '💪', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('personal_care', 'Personal Care', 'EXPENSE', '✨', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('clothing', 'Clothing', 'EXPENSE', '👕', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('education', 'Education', 'EXPENSE', '🎓', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy', 'Entertainment', 'EXPENSE', '🎮', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts', 'Gifts', 'EXPENSE', '🎁', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('kids', 'Kids & Family', 'EXPENSE', '👶', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pets', 'Pets', 'EXPENSE', '🐾', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('insurance', 'Insurance', 'EXPENSE', '🛡️', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tax', 'Tax', 'EXPENSE', '🏦', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business', 'Business Expense', 'EXPENSE', '👨‍💻', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling', 'Travelling', 'EXPENSE', '\uD83C\uDFDD\uFE0F', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('charity', 'Charity & Donations', 'EXPENSE', '💝', 'expense')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('reconciliation', 'Account Reconciliation', 'EXPENSE', '💱', 'expense')")

    // ── EXPENSE: subcategories ──

    // Dining & Drinks
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('eating_out', 'Eating Out', 'EXPENSE', NULL, 'beverages')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pubs', 'Pubs & Bars', 'EXPENSE', NULL, 'beverages')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('soft_drinks', 'Soft Drinks & Snacks', 'EXPENSE', NULL, 'beverages')")

    // Housing
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('rent', 'Rent', 'EXPENSE', NULL, 'housing')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('mortgage', 'Mortgage', 'EXPENSE', NULL, 'housing')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('utilities', 'Utilities', 'EXPENSE', NULL, 'housing')")

    // Transportation
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_metro', 'Public Transit', 'EXPENSE', NULL, 'transport')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_taxi', 'Taxi & Rideshare', 'EXPENSE', NULL, 'transport')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_bike', 'Bike & Scooter', 'EXPENSE', NULL, 'transport')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('transport_train', 'Train', 'EXPENSE', NULL, 'transport')")

    // Car & Vehicle
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('car_fuel', 'Fuel', 'EXPENSE', NULL, 'car')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('car_parking', 'Parking', 'EXPENSE', NULL, 'car')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('car_maintenance', 'Maintenance & Repairs', 'EXPENSE', NULL, 'car')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('car_wash', 'Car Wash', 'EXPENSE', NULL, 'car')")

    // Subscriptions
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('spotify', 'Music & Streaming', 'EXPENSE', NULL, 'subscriptions')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('internet', 'Phone & Internet', 'EXPENSE', NULL, 'subscriptions')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('apple', 'Cloud & Storage', 'EXPENSE', NULL, 'subscriptions')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('software', 'Software & Apps', 'EXPENSE', NULL, 'subscriptions')")

    // Health
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_doctor', 'Doctor', 'EXPENSE', NULL, 'health')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_medications', 'Medications', 'EXPENSE', NULL, 'health')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('health_exams', 'Examinations', 'EXPENSE', NULL, 'health')")

    // Sport
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_gym', 'Gym', 'EXPENSE', NULL, 'sport')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_equipment', 'Equipment', 'EXPENSE', NULL, 'sport')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('sport_supplements', 'Supplements', 'EXPENSE', NULL, 'sport')")

    // Personal Care
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('personal_care_haircut', 'Haircut', 'EXPENSE', NULL, 'personal_care')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('personal_care_skincare', 'Skincare & Beauty', 'EXPENSE', NULL, 'personal_care')")

    // Clothing
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shoes', 'Shoes', 'EXPENSE', NULL, 'clothing')")

    // Education
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('education_tuition', 'Tuition', 'EXPENSE', NULL, 'education')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('education_courses', 'Courses', 'EXPENSE', NULL, 'education')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('books', 'Books', 'EXPENSE', NULL, 'education')")

    // Entertainment (was "Joy")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_purchases', 'Purchases', 'EXPENSE', NULL, 'joy')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('joy_meetups', 'Meetups & Events', 'EXPENSE', NULL, 'joy')")

    // Gifts
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_family', 'Family', 'EXPENSE', NULL, 'gifts')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_friends', 'Friends', 'EXPENSE', NULL, 'gifts')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_girlfriend', 'Partner', 'EXPENSE', NULL, 'gifts')")

    // Kids & Family
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('kids_childcare', 'Childcare', 'EXPENSE', NULL, 'kids')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('kids_school', 'School', 'EXPENSE', NULL, 'kids')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('kids_activities', 'Activities', 'EXPENSE', NULL, 'kids')")

    // Pets
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pets_vet', 'Vet', 'EXPENSE', NULL, 'pets')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pets_food', 'Pet Food', 'EXPENSE', NULL, 'pets')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pets_grooming', 'Grooming', 'EXPENSE', NULL, 'pets')")

    // Insurance
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('insurance_health', 'Health Insurance', 'EXPENSE', NULL, 'insurance')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('insurance_car', 'Car Insurance', 'EXPENSE', NULL, 'insurance')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('insurance_home', 'Home Insurance', 'EXPENSE', NULL, 'insurance')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('insurance_life', 'Life Insurance', 'EXPENSE', NULL, 'insurance')")

    // Tax
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('pit', 'Income Tax', 'EXPENSE', NULL, 'tax')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('zus', 'Social Security', 'EXPENSE', NULL, 'tax')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gov_fee', 'Government Fee', 'EXPENSE', NULL, 'tax')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('fine', 'Fine', 'EXPENSE', NULL, 'tax')")

    // Business Expense (trimmed — power users can add more)
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_equipment', 'Equipment', 'EXPENSE', NULL, 'business')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_courses', 'Courses & Training', 'EXPENSE', NULL, 'business')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_meetups', 'Networking', 'EXPENSE', NULL, 'business')")

    // Travelling
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('accommodation', 'Accommodation', 'EXPENSE', NULL, 'travelling')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('flights', 'Flights', 'EXPENSE', NULL, 'travelling')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('travelling_transport', 'Local Transport', 'EXPENSE', NULL, 'travelling')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('food_drinks', 'Food & Drinks', 'EXPENSE', NULL, 'travelling')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tickets', 'Attractions & Tickets', 'EXPENSE', NULL, 'travelling')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('shopping', 'Shopping', 'EXPENSE', NULL, 'travelling')")

    // ── INCOME: general categories ──
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('salary', 'Salary', 'INCOME', '💸', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('investments', 'Investments', 'INCOME', '📈', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('business_income', 'Business Income', 'INCOME', '💼', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('rental_income', 'Rental Income', 'INCOME', '🏠', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('gifts_received', 'Gifts Received', 'INCOME', '🎁', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('tax_return', 'Tax Return', 'INCOME', '💸', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('refund', 'Refund', 'INCOME', '💸', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('repayment', 'Repayment', 'INCOME', '💸', 'income')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('positive_reconciliation', 'Account Reconciliation', 'INCOME', '💸', 'income')")

    // ── INCOME: subcategories ──

    // Salary
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('primary_job', 'Primary Job', 'INCOME', NULL, 'salary')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('side_income', 'Side Income', 'INCOME', NULL, 'salary')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('freelance', 'Freelance', 'INCOME', NULL, 'salary')")

    // Investments
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('dividends', 'Dividends', 'INCOME', NULL, 'investments')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('interest', 'Interest', 'INCOME', NULL, 'investments')")
    connection.execSQL("INSERT OR IGNORE INTO categories (id, title, type, emoji, parentId) VALUES ('capital_gains', 'Capital Gains', 'INCOME', NULL, 'investments')")
}

/**
 * Inserts all default asset categories (assets + liabilities).
 * Uses INSERT OR IGNORE so it's safe to call on existing data.
 */
fun seedAssetCategories(connection: SQLiteConnection) {
    // Asset categories
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('BANK_ACCOUNT', 'Bank Account', '🏦', 'Traditional bank accounts and deposits', ${0xFF4285F4}, 0, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('CASH', 'Cash Reserve', '💵', 'Physical cash holdings', ${0xFF34A853}, 1, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('REAL_ESTATE', 'Real Estate', '🏠', 'Property and real estate investments', ${0xFF795548}, 2, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('STOCKS', 'Stocks', '📈', 'Stock market investments', ${0xFFE91E63}, 3, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('BONDS', 'Bonds', '📜', 'Government and corporate bonds', ${0xFF9C27B0}, 4, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('CRYPTO', 'Cryptocurrency', '₿', 'Digital assets and cryptocurrencies', ${0xFFF57C00}, 5, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('PRECIOUS_METALS', 'Precious Metals', '🥇', 'Gold, silver, and other precious metals', ${0xFFFFD700}, 6, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('RETIREMENT', 'Retirement Fund', '🏖️', '401k, IRA, pension funds', ${0xFF00BCD4}, 7, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('BUSINESS', 'Business Assets', '💼', 'Business ownership and investments', ${0xFF607D8B}, 8, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('COLLECTIBLES', 'Collectibles', '🎨', 'Art, antiques, and collectible items', ${0xFFFF5722}, 9, 0)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('OTHER', 'Other Assets', '📦', 'Miscellaneous assets', ${0xFF9E9E9E}, 10, 0)")

    // Liability categories
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('MORTGAGE', 'Mortgage', '🏠', '', ${0xFFE53935}, 0, 1)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('LOAN', 'Loan', '📋', '', ${0xFFFF7043}, 1, 1)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('CREDIT_CARD', 'Credit Card', '💳', '', ${0xFFAB47BC}, 2, 1)")
    connection.execSQL("INSERT OR IGNORE INTO asset_categories (id, title, emoji, description, color, sortOrder, isLiability) VALUES ('DEBT', 'Debt', '📉', '', ${0xFF78909C}, 3, 1)")
}


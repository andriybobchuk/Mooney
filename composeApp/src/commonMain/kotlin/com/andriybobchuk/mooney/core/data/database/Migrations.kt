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
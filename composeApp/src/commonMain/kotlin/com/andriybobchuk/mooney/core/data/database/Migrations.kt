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
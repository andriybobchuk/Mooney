package com.andriybobchuk.time.core.data.database

import androidx.room.RoomDatabase

expect class MooneyDatabaseFactory {
    fun create(): RoomDatabase.Builder<AppDatabase>
}
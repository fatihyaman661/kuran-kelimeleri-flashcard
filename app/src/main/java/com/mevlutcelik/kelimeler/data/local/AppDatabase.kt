package com.mevlutcelik.kelimeler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntity::class, ReviewEntity::class, CardStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardStateDao(): CardStateDao
    abstract fun reviewDao(): ReviewDao
}

package com.mevlutcelik.kelimeler.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/** Sürüm 2: askıya alma için `suspended` tablosu (otomatik geçiş, tablo boş başlar). */
@Database(
    entities = [WordEntity::class, ReviewEntity::class, CardStateEntity::class, SuspendedEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun cardStateDao(): CardStateDao
    abstract fun reviewDao(): ReviewDao
    abstract fun suspendedDao(): SuspendedDao
}

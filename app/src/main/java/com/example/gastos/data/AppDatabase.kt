package com.example.gastos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, NotificationLog::class, LearnedPattern::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun learnedPatternDao(): LearnedPatternDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notification_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "packageName TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "text TEXT NOT NULL, " +
                        "parsed INTEGER NOT NULL, " +
                        "merchant TEXT, " +
                        "amount REAL, " +
                        "bank TEXT, " +
                        "reason TEXT, " +
                        "inTargetList INTEGER NOT NULL, " +
                        "type TEXT, " +
                        "dateMillis INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS learned_patterns (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "keyword TEXT NOT NULL, " +
                        "kind TEXT NOT NULL, " +
                        "dateMillis INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gastos.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
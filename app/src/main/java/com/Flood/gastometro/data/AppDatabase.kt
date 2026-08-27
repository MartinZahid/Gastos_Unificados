package com.Flood.gastometro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, NotificationLog::class, LearnedPattern::class],
    version = 4,
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

        // Agrega "reviewed" para poder distinguir, dentro de las notificaciones
        // que no se lograron parsear, cuáles ya revisó el usuario en Modo dev.
        // Sin esto no hay forma de avisar "tienes N nuevas sin revisar" en vez
        // de recontar siempre el total histórico.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notification_log ADD COLUMN reviewed INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN month TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "UPDATE transactions SET month = " +
                        "printf('%04d-%02d', " +
                        "CAST(strftime('%Y', dateMillis / 1000, 'unixepoch') AS INTEGER), " +
                        "CAST(strftime('%m', dateMillis / 1000, 'unixepoch') AS INTEGER)) " +
                        "WHERE month = ''"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
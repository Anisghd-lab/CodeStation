package com.ghdinteractivestudio.codestation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BotEntity::class,
        ChatMessageEntity::class,
        ApprovalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CodeStationDatabase : RoomDatabase() {
    abstract fun botDao(): BotDao
    abstract fun messageDao(): MessageDao
    abstract fun approvalDao(): ApprovalDao

    companion object {
        @Volatile
        private var INSTANCE: CodeStationDatabase? = null

        fun getDatabase(context: Context): CodeStationDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbPath = context.getDatabasePath("codestation.db")
                val legacyDbPath = context.getDatabasePath("synapse.db")
                if (!dbPath.exists() && legacyDbPath.exists()) {
                    try {
                        legacyDbPath.copyTo(dbPath, overwrite = true)
                    } catch (_: Exception) {
                        // Ignorer et démarrer sur base propre si la copie échoue
                    }
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeStationDatabase::class.java,
                    "codestation.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.ghdinteractivestudio.synapse.data.local

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
    version = 1,
    exportSchema = false
)
abstract class SynapseDatabase : RoomDatabase() {
    abstract fun botDao(): BotDao
    abstract fun messageDao(): MessageDao
    abstract fun approvalDao(): ApprovalDao

    companion object {
        @Volatile
        private var INSTANCE: SynapseDatabase? = null

        fun getDatabase(context: Context): SynapseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SynapseDatabase::class.java,
                    "synapse.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

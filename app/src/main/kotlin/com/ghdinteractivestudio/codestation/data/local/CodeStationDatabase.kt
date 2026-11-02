package com.ghdinteractivestudio.codestation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BotEntity::class,
        ChatMessageEntity::class,
        ApprovalEntity::class,
        ConversationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CodeStationDatabase : RoomDatabase() {
    abstract fun botDao(): BotDao
    abstract fun messageDao(): MessageDao
    abstract fun approvalDao(): ApprovalDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: CodeStationDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN conversationId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE chat_messages SET conversationId = botId WHERE conversationId = ''")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS conversations (
                        id TEXT PRIMARY KEY NOT NULL,
                        botId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        cliId TEXT,
                        runnerId TEXT,
                        cliSessionId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        lastMessageText TEXT NOT NULL DEFAULT '',
                        messageCount INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_botId ON conversations(botId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_updatedAt ON conversations(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_conversationId ON chat_messages(conversationId)")
            }
        }

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
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

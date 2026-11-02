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
                applyMigration2To3(db)
            }
        }

        val MIGRATION_1_3 = object : androidx.room.migration.Migration(1, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                applyMigration2To3(db)
            }
        }

        private fun applyMigration2To3(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            try {
                // 1. Recréer la table chat_messages selon le schéma exact attendu par Room
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages_new` (
                        `id` TEXT NOT NULL,
                        `botId` TEXT NOT NULL,
                        `authorType` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isFinal` INTEGER NOT NULL,
                        `replyToMessageId` TEXT,
                        `replyToAuthor` TEXT,
                        `replyToText` TEXT,
                        `attachmentsJson` TEXT,
                        `conversationId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                var hasConversationId = false
                val cursor = db.query("PRAGMA table_info(chat_messages)")
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1 && cursor.getString(nameIndex) == "conversationId") {
                        hasConversationId = true
                        break
                    }
                }
                cursor.close()

                if (hasConversationId) {
                    db.execSQL("""
                        INSERT OR REPLACE INTO `chat_messages_new` (
                            `id`, `botId`, `authorType`, `authorId`, `authorName`, `text`,
                            `timestamp`, `isFinal`, `replyToMessageId`, `replyToAuthor`,
                            `replyToText`, `attachmentsJson`, `conversationId`
                        )
                        SELECT 
                            `id`, `botId`, `authorType`, `authorId`, `authorName`, `text`,
                            `timestamp`, `isFinal`, `replyToMessageId`, `replyToAuthor`,
                            `replyToText`, `attachmentsJson`, COALESCE(NULLIF(`conversationId`, ''), `botId`)
                        FROM `chat_messages`
                    """.trimIndent())
                } else {
                    db.execSQL("""
                        INSERT OR REPLACE INTO `chat_messages_new` (
                            `id`, `botId`, `authorType`, `authorId`, `authorName`, `text`,
                            `timestamp`, `isFinal`, `replyToMessageId`, `replyToAuthor`,
                            `replyToText`, `attachmentsJson`, `conversationId`
                        )
                        SELECT 
                            `id`, `botId`, `authorType`, `authorId`, `authorName`, `text`,
                            `timestamp`, `isFinal`, `replyToMessageId`, `replyToAuthor`,
                            `replyToText`, `attachmentsJson`, `botId`
                        FROM `chat_messages`
                    """.trimIndent())
                }

                db.execSQL("DROP TABLE `chat_messages`")
                db.execSQL("ALTER TABLE `chat_messages_new` RENAME TO `chat_messages`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_botId` ON `chat_messages` (`botId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")

                // 2. Créer la table conversations et ses index
                db.execSQL("DROP TABLE IF EXISTS `conversations`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL,
                        `botId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `cliId` TEXT,
                        `runnerId` TEXT,
                        `cliSessionId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastMessageText` TEXT NOT NULL,
                        `messageCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_botId` ON `conversations` (`botId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_updatedAt` ON `conversations` (`updatedAt`)")
            } catch (_: Exception) {
                // Fallback de sécurité : recréer les tables proprement si altération impossible
                db.execSQL("DROP TABLE IF EXISTS `chat_messages`")
                db.execSQL("DROP TABLE IF EXISTS `conversations`")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` TEXT NOT NULL,
                        `botId` TEXT NOT NULL,
                        `authorType` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isFinal` INTEGER NOT NULL,
                        `replyToMessageId` TEXT,
                        `replyToAuthor` TEXT,
                        `replyToText` TEXT,
                        `attachmentsJson` TEXT,
                        `conversationId` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_botId` ON `chat_messages` (`botId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_conversationId` ON `chat_messages` (`conversationId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `conversations` (
                        `id` TEXT NOT NULL,
                        `botId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `cliId` TEXT,
                        `runnerId` TEXT,
                        `cliSessionId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastMessageText` TEXT NOT NULL,
                        `messageCount` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_botId` ON `conversations` (`botId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversations_updatedAt` ON `conversations` (`updatedAt`)")
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_1_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

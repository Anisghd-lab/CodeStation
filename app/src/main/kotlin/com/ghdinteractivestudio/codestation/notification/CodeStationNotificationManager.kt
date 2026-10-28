package com.ghdinteractivestudio.codestation.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ghdinteractivestudio.codestation.MainActivity
import com.ghdinteractivestudio.codestation.R

enum class TaskNotificationPhase {
    QUEUED,
    STARTING,
    WORKING,
    RESPONSE_RECEIVED,
    NEEDS_APPROVAL,
    COMPLETED,
    FAILED,
    STOPPED,
    CANCELLED,
    DISCONNECTED
}

object CodeStationNotificationManager {

    const val CHANNEL_LIVE_TASKS = "codestation_live_tasks_v2"
    const val CHANNEL_APPROVALS = "codestation_approvals_v2"
    const val CHANNEL_COMPLETED = "codestation_completed_v2"

    private const val NOTIFICATION_ID_BASE = 1000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            // Nettoyage proactif des anciens canaux si existants
            try {
                notificationManager.deleteNotificationChannel("codestation_live_tasks")
                notificationManager.deleteNotificationChannel("codestation_approvals")
                notificationManager.deleteNotificationChannel("codestation_completed")
            } catch (_: Exception) {}

            // 1. Canal des tâches en cours (Live Updates / silencieux mais AVEC icône visible dans la barre d'état)
            // IMPORTANCE_DEFAULT combiné avec setSound(null, null) garantit l'icône dans la barre d'état Android 14/15/16
            val liveChannel = NotificationChannel(
                CHANNEL_LIVE_TASKS,
                "Tâches en cours (Live Updates)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Progression et états d'exécution en temps réel des agents"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // 2. Canal des approbations (Haute priorité avec son/vibration)
            val approvalChannel = NotificationChannel(
                CHANNEL_APPROVALS,
                "Demandes d'autorisation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Demandes d'approbation requises pour commandes shell et modifications"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            // 3. Canal des tâches terminées (Priorité par défaut avec alerte discrète)
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Tâches terminées",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications des réponses finales et fins d'exécution"
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannels(listOf(liveChannel, approvalChannel, completedChannel))
        }
    }

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun getNotificationIdForBot(botId: String): Int {
        return NOTIFICATION_ID_BASE + (botId.hashCode() and 0x7FFFFFFF) % 100000
    }

    fun formatAgentDisplayName(botId: String, botName: String? = null): String {
        if (!botName.isNullOrBlank() && botName != botId && botName != "CodeStation Agent") {
            return botName
        }
        return when (botId) {
            "bot-opencode-glm" -> "OpenCode GLM"
            "bot-antigravity" -> "Google Antigravity"
            "bot-codex-terminal" -> "Codex Terminal"
            "lead" -> "Lead Agent"
            else -> botName ?: botId
        }
    }

    fun formatBackendDisplayName(agentBackend: String): String {
        return when (agentBackend.lowercase()) {
            "agy", "antigravity" -> "Google Antigravity CLI"
            "opencode" -> "OpenCode CLI"
            "codex" -> "Codex Termux"
            "acp" -> "Protocole ACP"
            else -> agentBackend
        }
    }

    fun updateTaskNotification(
        context: Context,
        botId: String,
        botName: String,
        agentBackend: String,
        phase: TaskNotificationPhase,
        summary: String,
        startedAtMs: Long? = null,
        approvalId: String? = null
    ) {
        if (!hasPermission(context)) return

        createChannels(context)

        val notificationId = getNotificationIdForBot(botId)
        val channelId = when (phase) {
            TaskNotificationPhase.NEEDS_APPROVAL -> CHANNEL_APPROVALS
            TaskNotificationPhase.COMPLETED,
            TaskNotificationPhase.FAILED,
            TaskNotificationPhase.STOPPED,
            TaskNotificationPhase.CANCELLED -> CHANNEL_COMPLETED
            else -> CHANNEL_LIVE_TASKS
        }

        val displayName = formatAgentDisplayName(botId, botName)
        val formattedBackend = formatBackendDisplayName(agentBackend)

        val title = when (phase) {
            TaskNotificationPhase.STARTING,
            TaskNotificationPhase.QUEUED -> "$displayName · Démarrage..."
            TaskNotificationPhase.WORKING -> "$displayName · En cours"
            TaskNotificationPhase.RESPONSE_RECEIVED -> "$displayName · Réception de la réponse"
            TaskNotificationPhase.NEEDS_APPROVAL -> "⚠️ $displayName · Autorisation requise"
            TaskNotificationPhase.COMPLETED -> "✓ $displayName · Tâche terminée"
            TaskNotificationPhase.FAILED -> "✗ $displayName · Échec"
            TaskNotificationPhase.STOPPED -> "■ $displayName · Arrêté"
            TaskNotificationPhase.CANCELLED -> "■ $displayName · Annulé"
            TaskNotificationPhase.DISCONNECTED -> "CodeStation · Déconnecté"
        }

        val ongoing = phase == TaskNotificationPhase.WORKING ||
                phase == TaskNotificationPhase.STARTING ||
                phase == TaskNotificationPhase.QUEUED ||
                phase == TaskNotificationPhase.RESPONSE_RECEIVED ||
                phase == TaskNotificationPhase.NEEDS_APPROVAL

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("codestation://chat?botId=$botId")
            putExtra("botId", botId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFF6366F1.toInt())
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\nAgent : $formattedBackend"))
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(ongoing)

        if ((phase == TaskNotificationPhase.WORKING || phase == TaskNotificationPhase.STARTING || phase == TaskNotificationPhase.QUEUED) && startedAtMs != null && startedAtMs > 0) {
            builder.setUsesChronometer(true)
            builder.setWhen(startedAtMs)
            builder.setShowWhen(true)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(true)
            builder.setWhen(System.currentTimeMillis())
        }

        // Action 1 : Ouvrir le chat dans l'application
        builder.addAction(
            R.drawable.sf_star_fill,
            "Ouvrir",
            pendingIntent
        )

        // Action 2 : Arrêter la tâche si le bot est en cours d'exécution
        if (ongoing && phase != TaskNotificationPhase.NEEDS_APPROVAL) {
            val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_STOP_BOT
                putExtra(NotificationActionReceiver.EXTRA_BOT_ID, botId)
                putExtra(NotificationActionReceiver.EXTRA_BOT_NAME, displayName)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.sf_stop_fill,
                "Arrêter",
                stopPendingIntent
            )
        }

        // Actions 3 & 4 : Autoriser ou Refuser si en attente d'approbation
        if (phase == TaskNotificationPhase.NEEDS_APPROVAL && approvalId != null) {
            val approveIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_APPROVE
                putExtra(NotificationActionReceiver.EXTRA_BOT_ID, botId)
                putExtra(NotificationActionReceiver.EXTRA_BOT_NAME, displayName)
                putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approvalId)
            }
            val approvePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                approveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.sf_checkmark,
                "Autoriser",
                approvePendingIntent
            )

            val rejectIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REJECT
                putExtra(NotificationActionReceiver.EXTRA_BOT_ID, botId)
                putExtra(NotificationActionReceiver.EXTRA_BOT_NAME, displayName)
                putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approvalId)
            }
            val rejectPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 3,
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.sf_xmark,
                "Refuser",
                rejectPendingIntent
            )
        }

        if (phase == TaskNotificationPhase.NEEDS_APPROVAL) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_CALL)
        } else if (ongoing) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setCategory(NotificationCompat.CATEGORY_STATUS)
        }

        try {
            val notification = builder.build()
            if (ongoing) {
                // Drapeaux explicites pour persistance garantie sur Android 14/15/16
                notification.flags = notification.flags or
                        android.app.Notification.FLAG_ONGOING_EVENT or
                        android.app.Notification.FLAG_NO_CLEAR
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission refusée dynamiquement
        }
    }

    fun clearTaskNotification(context: Context, botId: String) {
        val notificationId = getNotificationIdForBot(botId)
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun clearAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

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
    WORKING,
    NEEDS_APPROVAL,
    COMPLETED,
    FAILED,
    STOPPED,
    DISCONNECTED
}

object CodeStationNotificationManager {

    const val CHANNEL_LIVE_TASKS = "codestation_live_tasks"
    const val CHANNEL_APPROVALS = "codestation_approvals"
    const val CHANNEL_COMPLETED = "codestation_completed"

    private const val NOTIFICATION_ID_BASE = 1000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

            // 1. Canal des tâches en cours (Live Updates / silencieux mais persistant)
            val liveChannel = NotificationChannel(
                CHANNEL_LIVE_TASKS,
                "Tâches en cours (Live Updates)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progression et états d'exécution en temps réel des bots"
                setShowBadge(false)
            }

            // 2. Canal des approvations (Haute priorité avec son/vibration)
            val approvalChannel = NotificationChannel(
                CHANNEL_APPROVALS,
                "Demandes d'autorisation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Demandes d'approbation requises pour commandes shell et modifications"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Canal des tâches terminées (Priorité normale révocable)
            val completedChannel = NotificationChannel(
                CHANNEL_COMPLETED,
                "Tâches terminées",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications des réponses finales et fins d'exécution"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(liveChannel, approvalChannel, completedChannel))
        }
    }

    private fun hasPermission(context: Context): Boolean {
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
            TaskNotificationPhase.STOPPED -> CHANNEL_COMPLETED
            else -> CHANNEL_LIVE_TASKS
        }

        val title = when (phase) {
            TaskNotificationPhase.QUEUED -> "$botName · En file d'attente"
            TaskNotificationPhase.WORKING -> "$botName · Au travail"
            TaskNotificationPhase.NEEDS_APPROVAL -> "$botName · Autorisation requise"
            TaskNotificationPhase.COMPLETED -> "$botName · Tâche terminée"
            TaskNotificationPhase.FAILED -> "$botName · Erreur"
            TaskNotificationPhase.STOPPED -> "$botName · Arrêté"
            TaskNotificationPhase.DISCONNECTED -> "CodeStation · Connexion perdue"
        }

        val ongoing = phase == TaskNotificationPhase.WORKING ||
                phase == TaskNotificationPhase.QUEUED ||
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
            .setSmallIcon(R.drawable.sf_point_3_connected_trianglepath_dotted)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\nAgent : $agentBackend"))
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(phase == TaskNotificationPhase.WORKING)

        if (phase == TaskNotificationPhase.WORKING && startedAtMs != null && startedAtMs > 0) {
            builder.setUsesChronometer(true)
            builder.setWhen(startedAtMs)
            builder.setShowWhen(true)
        }

        if (phase == TaskNotificationPhase.NEEDS_APPROVAL) {
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setCategory(NotificationCompat.CATEGORY_CALL)
        } else if (ongoing) {
            builder.setCategory(NotificationCompat.CATEGORY_PROGRESS)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
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

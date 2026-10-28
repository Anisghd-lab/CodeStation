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

    const val CHANNEL_LIVE_TASKS = "codestation_live_tasks"
    const val CHANNEL_APPROVALS = "codestation_approvals"
    const val CHANNEL_COMPLETED = "codestation_completed"

    private const val NOTIFICATION_ID_BASE = 1000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

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
            }

            // 3. Canal des tâches terminées (Priorité par défaut avec alerte discrète)
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
            TaskNotificationPhase.STOPPED,
            TaskNotificationPhase.CANCELLED -> CHANNEL_COMPLETED
            else -> CHANNEL_LIVE_TASKS
        }

        val title = when (phase) {
            TaskNotificationPhase.STARTING -> "$botName · Démarrage"
            TaskNotificationPhase.WORKING -> "$botName · En cours"
            TaskNotificationPhase.RESPONSE_RECEIVED -> "$botName · Réponse reçue"
            TaskNotificationPhase.NEEDS_APPROVAL -> "$botName · Autorisation requise"
            TaskNotificationPhase.COMPLETED -> "$botName · Terminé"
            TaskNotificationPhase.FAILED -> "$botName · Échec"
            TaskNotificationPhase.STOPPED,
            TaskNotificationPhase.CANCELLED -> "$botName · Arrêté"
            TaskNotificationPhase.DISCONNECTED -> "CodeStation · Déconnecté"
        }

        val ongoing = phase == TaskNotificationPhase.WORKING ||
                phase == TaskNotificationPhase.STARTING ||
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
            .setSmallIcon(R.drawable.sf_star_fill)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$summary\nAgent : $agentBackend"))
            .setContentIntent(pendingIntent)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(ongoing)

        if ((phase == TaskNotificationPhase.WORKING || phase == TaskNotificationPhase.STARTING) && startedAtMs != null && startedAtMs > 0) {
            builder.setUsesChronometer(true)
            builder.setWhen(startedAtMs)
            builder.setShowWhen(true)
        }

        // Action 1 : Ouvrir le chat dans l'application
        builder.addAction(
            R.drawable.sf_star_fill,
            "Ouvrir",
            pendingIntent
        )

        // Action 2 : Arrêter la tâche si le bot est actif
        if (ongoing) {
            val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_STOP_BOT
                putExtra(NotificationActionReceiver.EXTRA_BOT_ID, botId)
                putExtra(NotificationActionReceiver.EXTRA_BOT_NAME, botName)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.sf_star_fill,
                "Arrêter",
                stopPendingIntent
            )
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

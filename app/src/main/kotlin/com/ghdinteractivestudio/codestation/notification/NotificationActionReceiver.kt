package com.ghdinteractivestudio.codestation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.util.RequestTracer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récepteur d'actions déclenchées depuis les notifications Android (ex: Arrêter une tâche en cours).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_BOT = "com.ghdinteractivestudio.codestation.ACTION_STOP_BOT"
        const val EXTRA_BOT_ID = "botId"
        const val EXTRA_BOT_NAME = "botName"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_STOP_BOT) {
            val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: return
            val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "Agent CodeStation"
            val app = context.applicationContext as? CodeStationApplication ?: return

            RequestTracer.logEvent(
                requestId = "notif-$botId",
                event = RequestTracer.EVENT_REQUEST_CANCELLED,
                details = mapOf("source" to "notification_action", "botId" to botId)
            )

            // Mise à jour immédiate de la notification système en état "Arrêté"
            CodeStationNotificationManager.updateTaskNotification(
                context = context,
                botId = botId,
                botName = botName,
                agentBackend = "agent",
                phase = TaskNotificationPhase.STOPPED,
                summary = "Tâche interrompue par l'utilisateur."
            )

            // Déclenchement de l'arrêt réel côté hôte et repository
            CoroutineScope(Dispatchers.IO).launch {
                app.container.botRepository.stopBotTurn(botId)
            }
        }
    }
}

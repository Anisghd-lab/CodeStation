package com.ghdinteractivestudio.codestation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.util.RequestTracer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récepteur d'actions déclenchées depuis les notifications persistantes Android
 * (Arrêter une tâche en cours, Autoriser/Refuser une commande).
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_BOT = "com.ghdinteractivestudio.codestation.ACTION_STOP_BOT"
        const val ACTION_APPROVE = "com.ghdinteractivestudio.codestation.ACTION_APPROVE"
        const val ACTION_REJECT = "com.ghdinteractivestudio.codestation.ACTION_REJECT"

        const val EXTRA_BOT_ID = "botId"
        const val EXTRA_BOT_NAME = "botName"
        const val EXTRA_APPROVAL_ID = "approvalId"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val botId = intent.getStringExtra(EXTRA_BOT_ID) ?: return
        val botName = intent.getStringExtra(EXTRA_BOT_NAME) ?: "Agent CodeStation"
        val app = context.applicationContext as? CodeStationApplication ?: return

        when (action) {
            ACTION_STOP_BOT -> {
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

            ACTION_APPROVE -> {
                val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: return
                RequestTracer.logEvent(
                    requestId = "approval-$approvalId",
                    event = "notification_approved",
                    details = mapOf("botId" to botId, "approvalId" to approvalId)
                )

                CodeStationNotificationManager.updateTaskNotification(
                    context = context,
                    botId = botId,
                    botName = botName,
                    agentBackend = "agent",
                    phase = TaskNotificationPhase.WORKING,
                    summary = "Autorisation accordée. Reprise de l'exécution..."
                )

                CoroutineScope(Dispatchers.IO).launch {
                    app.container.chatRepository.respondApproval(approvalId, ApprovalStatus.APPROVED, false)
                }
            }

            ACTION_REJECT -> {
                val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: return
                RequestTracer.logEvent(
                    requestId = "approval-$approvalId",
                    event = "notification_rejected",
                    details = mapOf("botId" to botId, "approvalId" to approvalId)
                )

                CodeStationNotificationManager.updateTaskNotification(
                    context = context,
                    botId = botId,
                    botName = botName,
                    agentBackend = "agent",
                    phase = TaskNotificationPhase.WORKING,
                    summary = "Action refusée par l'utilisateur."
                )

                CoroutineScope(Dispatchers.IO).launch {
                    app.container.chatRepository.respondApproval(approvalId, ApprovalStatus.REJECTED, false)
                }
            }
        }
    }
}

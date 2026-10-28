package com.ghdinteractivestudio.codestation.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.MainActivity
import com.ghdinteractivestudio.codestation.R
import com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager

class CodeStationTaskSyncService : Service() {

    private var eventStreamClient: CodeStationEventStreamClient? = null

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 9999

        fun start(context: Context) {
            try {
                val intent = Intent(context, CodeStationTaskSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Évite tout crash si invoqué en arrière-plan sous des règles système strictes
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, CodeStationTaskSyncService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        CodeStationNotificationManager.createChannels(this)

        val app = application as? CodeStationApplication
        val container = app?.container

        if (container != null) {
            eventStreamClient = container.eventStreamClient
            eventStreamClient?.start()
        }

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CodeStationNotificationManager.CHANNEL_LIVE_TASKS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFF6366F1.toInt())
            .setContentTitle("CodeStation · Surveillance active")
            .setContentText("Hôte local connecté (127.0.0.1:19222)")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        notification.flags = notification.flags or
                android.app.Notification.FLAG_ONGOING_EVENT or
                android.app.Notification.FLAG_NO_CLEAR

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        eventStreamClient?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        eventStreamClient?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.ghdinteractivestudio.codestation.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.R
import com.ghdinteractivestudio.codestation.data.remote.CodeStationEventStreamClient
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager

class CodeStationTaskSyncService : Service() {

    private var eventStreamClient: CodeStationEventStreamClient? = null

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 9999

        fun start(context: Context) {
            val intent = Intent(context, CodeStationTaskSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CodeStationTaskSyncService::class.java)
            context.stopService(intent)
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

        // Notification minimale de service d'arrière-plan
        val notification = NotificationCompat.Builder(this, CodeStationNotificationManager.CHANNEL_LIVE_TASKS)
            .setSmallIcon(R.drawable.sf_point_3_connected_trianglepath_dotted)
            .setContentTitle("CodeStation Sync")
            .setContentText("Surveillance locale active sur 127.0.0.1:19222")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

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

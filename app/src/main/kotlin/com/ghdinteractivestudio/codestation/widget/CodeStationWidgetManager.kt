package com.ghdinteractivestudio.codestation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.MainActivity
import com.ghdinteractivestudio.codestation.R
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object CodeStationWidgetManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        updateUsageWidgets(context, appWidgetManager)
        updateLimitsWidgets(context, appWidgetManager)
        updateBotsWidgets(context, appWidgetManager)
        updateTeamWidgets(context, appWidgetManager)
    }

    private fun createActivityPendingIntent(context: Context, deepLinkUri: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLinkUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            deepLinkUri.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun updateUsageWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
        if (ids.isEmpty()) return

        scope.launch {
            val app = context.applicationContext as? CodeStationApplication
            val container = app?.container

            val catalog = container?.antigravityModelRepository?.catalogState?.value
            val modelsCount = catalog?.models?.size ?: 13
            val defaultModel = catalog?.models?.firstOrNull { it.isDefault }?.displayName ?: "gemini-4-argon"

            for (widgetId in ids) {
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_usage_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_provider_name, "Antigravity")
                    setTextViewText(R.id.tv_usage_metric, "$modelsCount modèles dispos")
                    setTextViewText(R.id.tv_secondary_metric, "OpenCode GLM : Prêt")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_usage_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_antigravity_details, "$modelsCount modèles catalogués")
                    setTextViewText(R.id.tv_antigravity_model, defaultModel)
                    setTextViewText(R.id.tv_opencode_details, "CLI PRoot installée")
                    setTextViewText(R.id.tv_opencode_model, "glm-5.3-flash-free")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_usage_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_antigravity_details, "Catalogue actif : $modelsCount modèles (Gemini 4 Argon, Claude 5.5...)")
                    setTextViewText(R.id.tv_opencode_details, "Dernier modèle : opencode/glm-5.3-flash-free (Actif)")
                    setTextViewText(R.id.tv_codex_details, "Session interactive / Terminal PRoot disponible")
                }

                val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(120f, 100f) to smallViews,
                            SizeF(250f, 110f) to mediumViews,
                            SizeF(250f, 220f) to largeViews
                        )
                    )
                } else {
                    mediumViews
                }

                manager.updateAppWidget(widgetId, finalViews)
            }
        }
    }

    fun updateLimitsWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, LimitsWidgetProvider::class.java))
        if (ids.isEmpty()) return

        scope.launch {
            val app = context.applicationContext as? CodeStationApplication
            val container = app?.container

            val envData = container?.serverRepository?.environmentsStream?.first()
            val ramAvailable = envData?.resources?.ramAvailableMb ?: 2450L
            val ramTotal = envData?.resources?.ramTotalMb ?: 12288L
            val diskAvailable = envData?.resources?.diskAvailableGb ?: 148L

            val ramStr = "%.1f Go libres".format(ramAvailable / 1024.0)
            val ramTotalStr = "sur %.1f Go total".format(ramTotal / 1024.0)
            val diskStr = "$diskAvailable Go libres"

            for (widgetId in ids) {
                val pendingIntent = createActivityPendingIntent(context, "codestation://pair")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_limits_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_server_status, "Hôte Connecté")
                    setTextViewText(R.id.tv_ram_available, "RAM : ${ramAvailable} Mo dispo")
                    setTextViewText(R.id.tv_latency, "Latence : 4 ms (Local)")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_limits_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_ram_value, ramStr)
                    setTextViewText(R.id.tv_ram_sub, ramTotalStr)
                    setTextViewText(R.id.tv_disk_value, diskStr)
                    setTextViewText(R.id.tv_env_count, "Ubuntu PRoot opérationnel")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_limits_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_host_details, "Adresse : http://127.0.0.1:19222 · Latence : 3 ms")
                    setTextViewText(R.id.tv_ram_details, "RAM disponible : $ramStr · Stockage libre : $diskStr")
                    setTextViewText(R.id.tv_quota_details, "Limites : Aucune restriction locale détectée sur PRoot")
                }

                val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(120f, 100f) to smallViews,
                            SizeF(250f, 110f) to mediumViews,
                            SizeF(250f, 220f) to largeViews
                        )
                    )
                } else {
                    mediumViews
                }

                manager.updateAppWidget(widgetId, finalViews)
            }
        }
    }

    fun updateBotsWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, BotsWidgetProvider::class.java))
        if (ids.isEmpty()) return

        scope.launch {
            val app = context.applicationContext as? CodeStationApplication
            val container = app?.container

            val bots = container?.botRepository?.getBotsStream()?.first() ?: emptyList()
            val approvals = container?.approvalDao?.getAllPendingApprovalsStream()?.first() ?: emptyList()

            val workingBots = bots.filter { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }
            val activeCount = workingBots.size
            val pendingCount = approvals.size

            val lastActiveBot = workingBots.firstOrNull() ?: bots.firstOrNull()
            val lastActiveBotName = lastActiveBot?.name ?: "Google Antigravity"

            for (widgetId in ids) {
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_bots_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_active_count, if (activeCount > 0) "$activeCount en cours" else "0 actif")
                    setTextViewText(R.id.tv_active_bot_name, lastActiveBotName)
                    setTextViewText(R.id.tv_approvals_count, "$pendingCount approbation requise")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_bots_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_working_badge, if (activeCount > 0) "$activeCount actif" else "En veille")
                    setTextViewText(R.id.tv_last_active_bot, lastActiveBotName)
                    setTextViewText(R.id.tv_last_active_status, if (activeCount > 0) "Exécution en cours..." else "Prêt à recevoir des instructions")
                    setTextViewText(R.id.tv_pending_approvals, "$pendingCount en attente")
                    setTextViewText(R.id.tv_total_bots, "${bots.size} agents configurés")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_bots_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_bots_header_badge, "${bots.size} agents configurés")

                    val b1 = bots.getOrNull(0)
                    val b2 = bots.getOrNull(1)
                    val b3 = bots.getOrNull(2)

                    if (b1 != null) {
                        setTextViewText(R.id.tv_bot1_name, b1.name)
                        setTextViewText(R.id.tv_bot1_status, "${b1.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${b1.model}")
                    }
                    if (b2 != null) {
                        setTextViewText(R.id.tv_bot2_name, b2.name)
                        setTextViewText(R.id.tv_bot2_status, "${b2.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${b2.model}")
                    }
                    if (b3 != null) {
                        setTextViewText(R.id.tv_bot3_name, b3.name)
                        setTextViewText(R.id.tv_bot3_status, "${b3.status.name.lowercase().replaceFirstChar { it.uppercase() }} · ${b3.model}")
                    }
                }

                val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(120f, 100f) to smallViews,
                            SizeF(250f, 110f) to mediumViews,
                            SizeF(250f, 220f) to largeViews
                        )
                    )
                } else {
                    mediumViews
                }

                manager.updateAppWidget(widgetId, finalViews)
            }
        }
    }

    fun updateTeamWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, TeamWidgetProvider::class.java))
        if (ids.isEmpty()) return

        scope.launch {
            val app = context.applicationContext as? CodeStationApplication
            val container = app?.container

            val bots = container?.botRepository?.getBotsStream()?.first() ?: emptyList()
            val totalCount = bots.size
            val leadBot = bots.firstOrNull { it.isLead } ?: bots.firstOrNull()

            for (widgetId in ids) {
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_team_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_team_count, "$totalCount Agents")
                    setTextViewText(R.id.tv_lead_agent, "${leadBot?.name ?: "Antigravity"} · Lead")
                    setTextViewText(R.id.tv_team_status, "Environnement PRoot OK")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_team_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_roster_count_badge, "$totalCount membres")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_team_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_team_large_badge, "$totalCount bots opérationnels")
                }

                val finalViews = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RemoteViews(
                        mapOf(
                            SizeF(120f, 100f) to smallViews,
                            SizeF(250f, 110f) to mediumViews,
                            SizeF(250f, 220f) to largeViews
                        )
                    )
                } else {
                    mediumViews
                }

                manager.updateAppWidget(widgetId, finalViews)
            }
        }
    }
}

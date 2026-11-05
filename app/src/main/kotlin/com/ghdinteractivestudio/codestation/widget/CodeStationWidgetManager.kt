package com.ghdinteractivestudio.codestation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.MainActivity
import com.ghdinteractivestudio.codestation.R
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object CodeStationWidgetManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        WidgetStateRepository.refresh(context)

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

    private fun safeColor(hex: String, fallback: Int = 0xFF1084FE.toInt()): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }

    fun updateUsageWidgets(context: Context, manager: AppWidgetManager) {
        val ids = manager.getAppWidgetIds(ComponentName(context, UsageWidgetProvider::class.java))
        if (ids.isEmpty()) return

        scope.launch {
            val app = context.applicationContext as? CodeStationApplication
            val container = app?.container

            val catalog = container?.antigravityModelRepository?.catalogState?.value
            val modelsCount = catalog?.models?.size ?: 6
            val defaultModel = catalog?.models?.firstOrNull { it.isDefault }?.displayName ?: "gemini-3.8-flash-medium"

            // Usage percentages
            val sessionPct = 16.0
            val weeklyPct = 59.0

            // Bitmaps
            val providerAvatar = WidgetBitmapRenderer.renderAvatar("blob", "blue", 64, highlightColor = 0xFF438DFF.toInt())
            val sessionTicksSmall = WidgetBitmapRenderer.renderUsageTicks(sessionPct, 0xFF38BDF8.toInt(), 280, 24)
            val sessionTicksMed = WidgetBitmapRenderer.renderUsageTicks(sessionPct, 0xFF38BDF8.toInt(), 280, 24)
            val weeklyTicksMed = WidgetBitmapRenderer.renderUsageTicks(weeklyPct, 0xFF38BDF8.toInt(), 280, 24)
            val sessionTicksLarge = WidgetBitmapRenderer.renderUsageTicks(sessionPct, 0xFF38BDF8.toInt(), 500, 24)
            val weeklyTicksLarge = WidgetBitmapRenderer.renderUsageTicks(weeklyPct, 0xFF38BDF8.toInt(), 500, 24)

            val antigravityAvatar = WidgetBitmapRenderer.renderAvatar("blob", "blue", 48, highlightColor = 0xFF438DFF.toInt())
            val opencodeAvatar = WidgetBitmapRenderer.renderAvatar("burst", "green", 48, highlightColor = 0xFF10B981.toInt())
            val codexAvatar = WidgetBitmapRenderer.renderAvatar("hex", "orange", 48, highlightColor = 0xFFF0A030.toInt())

            for (widgetId in ids) {
                WidgetStateRepository.recordWidgetUpdate(context, widgetId, WidgetCategory.USAGE)
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_usage_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_provider_avatar, providerAvatar)
                    setTextViewText(R.id.tv_provider_name, "Antigravity")
                    setTextViewText(R.id.tv_session_percent, "${sessionPct.toInt()}%")
                    setImageViewBitmap(R.id.iv_session_progress, sessionTicksSmall)
                    setTextViewText(R.id.tv_session_reset, "reset dans 3h 20m")
                    setTextViewText(R.id.tv_secondary_metric, "$modelsCount modèles dispos")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_usage_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_provider_avatar, providerAvatar)
                    setTextViewText(R.id.tv_title, "Consommation IA")
                    setTextViewText(R.id.tv_status_badge, "Opérationnel")
                    setTextViewText(R.id.tv_session_percent, "${sessionPct.toInt()}%")
                    setImageViewBitmap(R.id.iv_session_progress, sessionTicksMed)
                    setTextViewText(R.id.tv_session_reset, "réinitialisation dans 3h 20m")
                    setTextViewText(R.id.tv_weekly_percent, "${weeklyPct.toInt()}%")
                    setImageViewBitmap(R.id.iv_weekly_progress, weeklyTicksMed)
                    setTextViewText(R.id.tv_weekly_reset, "réinitialisation dans 2d 6h")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_usage_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_provider_avatar, providerAvatar)
                    setTextViewText(R.id.tv_active_model_desc, "$modelsCount modèles catalogués · $defaultModel")
                    setTextViewText(R.id.tv_status_badge, "Multi-moteurs")
                    setTextViewText(R.id.tv_session_percent, "${sessionPct.toInt()}%")
                    setImageViewBitmap(R.id.iv_session_progress, sessionTicksLarge)
                    setTextViewText(R.id.tv_weekly_percent, "${weeklyPct.toInt()}%")
                    setImageViewBitmap(R.id.iv_weekly_progress, weeklyTicksLarge)

                    setImageViewBitmap(R.id.iv_antigravity_avatar, antigravityAvatar)
                    setTextViewText(R.id.tv_antigravity_details, "Catalogue actif : $modelsCount modèles (Gemini 4 Argon, Claude 5.5...)")

                    setImageViewBitmap(R.id.iv_opencode_avatar, opencodeAvatar)
                    setTextViewText(R.id.tv_opencode_details, "Dernier modèle : glm-5.3-flash-free (Actif)")

                    setImageViewBitmap(R.id.iv_codex_avatar, codexAvatar)
                    setTextViewText(R.id.tv_codex_details, "Terminal Termux PRoot disponible")
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

            val envData = try {
                container?.serverRepository?.environmentsStream?.firstOrNull()
            } catch (e: Exception) {
                null
            }
            val ramAvailable = envData?.resources?.ramAvailableMb ?: 2450L
            val ramTotal = envData?.resources?.ramTotalMb ?: 12288L
            val diskAvailable = envData?.resources?.diskAvailableGb ?: 148L

            val ramPct = ((ramTotal - ramAvailable).toDouble() / ramTotal.toDouble() * 100.0).coerceIn(0.0, 100.0)
            val ramFreePct = (ramAvailable.toDouble() / ramTotal.toDouble() * 100.0).coerceIn(0.0, 100.0)
            val diskPct = 35.0

            val ramStr = "%.1f Go libres".format(ramAvailable / 1024.0)
            val ramTotalStr = "sur %.1f Go total".format(ramTotal / 1024.0)
            val diskStr = "$diskAvailable Go libres"

            val serverIcon = WidgetBitmapRenderer.renderServerIcon(48)
            val ramProgressSmall = WidgetBitmapRenderer.renderProgressBar(ramPct, 0xFF38BDF8.toInt(), 280, 16)
            val ramProgressMed = WidgetBitmapRenderer.renderProgressBar(ramPct, 0xFF38BDF8.toInt(), 280, 16)
            val diskProgressMed = WidgetBitmapRenderer.renderProgressBar(diskPct, 0xFF10B981.toInt(), 280, 16)
            val ramProgressLarge = WidgetBitmapRenderer.renderProgressBar(ramPct, 0xFF38BDF8.toInt(), 500, 16)

            for (widgetId in ids) {
                WidgetStateRepository.recordWidgetUpdate(context, widgetId, WidgetCategory.LIMITS)
                val pendingIntent = createActivityPendingIntent(context, "codestation://pair")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_limits_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_host_icon, serverIcon)
                    setTextViewText(R.id.tv_server_status_badge, "En ligne")
                    setTextViewText(R.id.tv_server_status, "Hôte Connecté")
                    setTextViewText(R.id.tv_ram_available, ramStr)
                    setImageViewBitmap(R.id.iv_ram_progress, ramProgressSmall)
                    setTextViewText(R.id.tv_latency, "Latence : 3 ms (Local)")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_limits_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_host_icon, serverIcon)
                    setTextViewText(R.id.tv_host_url, "127.0.0.1:19222")
                    setTextViewText(R.id.tv_ram_percent, "${ramFreePct.toInt()}% dispo")
                    setTextViewText(R.id.tv_ram_value, ramStr)
                    setImageViewBitmap(R.id.iv_ram_progress, ramProgressMed)
                    setTextViewText(R.id.tv_ram_sub, ramTotalStr)
                    setTextViewText(R.id.tv_disk_value, diskStr)
                    setImageViewBitmap(R.id.iv_disk_progress, diskProgressMed)
                    setTextViewText(R.id.tv_env_count, "Ubuntu PRoot opérationnel")
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_limits_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_host_icon, serverIcon)
                    setTextViewText(R.id.tv_host_sub, "Ubuntu PRoot · Environnement local")
                    setTextViewText(R.id.tv_server_status, "Serveur en ligne")
                    setTextViewText(R.id.tv_ram_percent, "${ramFreePct.toInt()}% libre")
                    setImageViewBitmap(R.id.iv_ram_progress, ramProgressLarge)
                    setTextViewText(R.id.tv_ram_details, "RAM disponible : $ramStr $ramTotalStr")
                    setTextViewText(R.id.tv_host_details, "Adresse : http://127.0.0.1:19222 · Latence : 3 ms")
                    setTextViewText(R.id.tv_quota_details, "Stockage : $diskStr · Aucune restriction locale détectée sur PRoot")
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

            val bots = try {
                container?.botRepository?.getBotsStream()?.firstOrNull() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val approvals = try {
                container?.approvalDao?.getAllPendingApprovalsStream()?.firstOrNull() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val workingBots = bots.filter { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }
            val activeCount = workingBots.size
            val pendingCount = approvals.size

            val lastActiveBot = workingBots.firstOrNull() ?: bots.firstOrNull()
            val lastActiveBotName = lastActiveBot?.name ?: "Google Antigravity"
            val lastActiveBotStatus = if (lastActiveBot?.status == BotStatus.WORKING) "Au travail…" else "Prêt à coder"

            val leadAvatar = WidgetBitmapRenderer.renderAvatar(
                shape = lastActiveBot?.avatarGlyph ?: "diamond",
                colorName = "blue",
                sizePx = 76,
                highlightColor = safeColor(lastActiveBot?.avatarColorHex ?: "#00D2FF"),
                status = if (activeCount > 0) "working" else "idle"
            )

            for (widgetId in ids) {
                WidgetStateRepository.recordWidgetUpdate(context, widgetId, WidgetCategory.BOTS)
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_bots_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_active_badge, if (activeCount > 0) "$activeCount actif" else "En veille")
                    setImageViewBitmap(R.id.iv_bot_avatar, leadAvatar)
                    setTextViewText(R.id.tv_active_bot_name, lastActiveBotName)
                    setTextViewText(R.id.tv_active_bot_status, lastActiveBotStatus)
                    setTextViewText(R.id.tv_approvals_count, "$pendingCount approbation requise")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_bots_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_working_badge, if (activeCount > 0) "$activeCount actif" else "En veille")
                    setImageViewBitmap(R.id.iv_bot_avatar, leadAvatar)
                    setTextViewText(R.id.tv_last_active_bot, lastActiveBotName)
                    setTextViewText(R.id.tv_last_active_status, if (activeCount > 0) "Exécution en cours…" else "Prêt à recevoir des instructions")
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
                        setImageViewBitmap(
                            R.id.iv_bot1_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b1.avatarGlyph,
                                "blue",
                                64,
                                highlightColor = safeColor(b1.avatarColorHex),
                                status = b1.status.name.lowercase()
                            )
                        )
                        setTextViewText(R.id.tv_bot1_name, b1.name)
                        setTextViewText(R.id.tv_bot1_status, "${formatBotStatus(b1.status)} · ${b1.model}")
                        setTextViewText(R.id.tv_bot1_badge, if (b1.isLead) "Lead" else formatBotStatus(b1.status))
                    }
                    if (b2 != null) {
                        setImageViewBitmap(
                            R.id.iv_bot2_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b2.avatarGlyph,
                                "green",
                                64,
                                highlightColor = safeColor(b2.avatarColorHex),
                                status = b2.status.name.lowercase()
                            )
                        )
                        setTextViewText(R.id.tv_bot2_name, b2.name)
                        setTextViewText(R.id.tv_bot2_status, "${formatBotStatus(b2.status)} · ${b2.model}")
                        setTextViewText(R.id.tv_bot2_badge, formatBotStatus(b2.status))
                    }
                    if (b3 != null) {
                        setImageViewBitmap(
                            R.id.iv_bot3_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b3.avatarGlyph,
                                "orange",
                                64,
                                highlightColor = safeColor(b3.avatarColorHex),
                                status = b3.status.name.lowercase()
                            )
                        )
                        setTextViewText(R.id.tv_bot3_name, b3.name)
                        setTextViewText(R.id.tv_bot3_status, "${formatBotStatus(b3.status)} · ${b3.model}")
                        setTextViewText(R.id.tv_bot3_badge, formatBotStatus(b3.status))
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

            val bots = try {
                container?.botRepository?.getBotsStream()?.firstOrNull() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            val totalCount = bots.size
            val leadBot = bots.firstOrNull { it.isLead } ?: bots.firstOrNull()

            val leadAvatar = WidgetBitmapRenderer.renderAvatar(
                shape = leadBot?.avatarGlyph ?: "diamond",
                colorName = "blue",
                sizePx = 76,
                highlightColor = safeColor(leadBot?.avatarColorHex ?: "#438DFF")
            )

            for (widgetId in ids) {
                WidgetStateRepository.recordWidgetUpdate(context, widgetId, WidgetCategory.TEAM)
                val pendingIntent = createActivityPendingIntent(context, "codestation://roster")

                val smallViews = RemoteViews(context.packageName, R.layout.widget_team_small).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setImageViewBitmap(R.id.iv_lead_avatar, leadAvatar)
                    setTextViewText(R.id.tv_team_count, "$totalCount Agents")
                    setTextViewText(R.id.tv_lead_agent, "${leadBot?.name ?: "Antigravity"} · Lead")
                    setTextViewText(R.id.tv_team_status, "Environnement PRoot OK")
                }

                val mediumViews = RemoteViews(context.packageName, R.layout.widget_team_medium).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_roster_count_badge, "$totalCount membres")

                    val b1 = bots.getOrNull(0) ?: leadBot
                    val b2 = bots.getOrNull(1)

                    if (b1 != null) {
                        setImageViewBitmap(
                            R.id.iv_agent1_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b1.avatarGlyph,
                                "blue",
                                60,
                                highlightColor = safeColor(b1.avatarColorHex)
                            )
                        )
                        setTextViewText(R.id.tv_agent1_name, b1.name)
                        setTextViewText(R.id.tv_agent1_role, if (b1.isLead) "Lead Agent" else "Agent IA")
                        setTextViewText(R.id.tv_agent1_status, "Statut : ${formatBotStatus(b1.status)}")
                    }
                    if (b2 != null) {
                        setImageViewBitmap(
                            R.id.iv_agent2_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b2.avatarGlyph,
                                "green",
                                60,
                                highlightColor = safeColor(b2.avatarColorHex)
                            )
                        )
                        setTextViewText(R.id.tv_agent2_name, b2.name)
                        setTextViewText(R.id.tv_agent2_role, "Spécialiste")
                        setTextViewText(R.id.tv_agent2_status, "Statut : ${formatBotStatus(b2.status)}")
                    }
                }

                val largeViews = RemoteViews(context.packageName, R.layout.widget_team_large).apply {
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                    setTextViewText(R.id.tv_team_large_badge, "$totalCount bots opérationnels")

                    val b1 = bots.getOrNull(0) ?: leadBot
                    val b2 = bots.getOrNull(1)
                    val b3 = bots.getOrNull(2)

                    if (b1 != null) {
                        setImageViewBitmap(
                            R.id.iv_team1_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b1.avatarGlyph,
                                "blue",
                                64,
                                highlightColor = safeColor(b1.avatarColorHex)
                            )
                        )
                        setTextViewText(R.id.tv_team1_name, b1.name)
                        setTextViewText(R.id.tv_team1_desc, "${if (b1.isLead) "Lead Agent" else "Agent"} · ${b1.model}")
                        setTextViewText(R.id.tv_team1_badge, if (b1.isLead) "Lead" else formatBotStatus(b1.status))
                    }
                    if (b2 != null) {
                        setImageViewBitmap(
                            R.id.iv_team2_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b2.avatarGlyph,
                                "green",
                                64,
                                highlightColor = safeColor(b2.avatarColorHex)
                            )
                        )
                        setTextViewText(R.id.tv_team2_name, b2.name)
                        setTextViewText(R.id.tv_team2_desc, "Code Specialist · ${b2.model}")
                        setTextViewText(R.id.tv_team2_badge, formatBotStatus(b2.status))
                    }
                    if (b3 != null) {
                        setImageViewBitmap(
                            R.id.iv_team3_avatar,
                            WidgetBitmapRenderer.renderAvatar(
                                b3.avatarGlyph,
                                "orange",
                                64,
                                highlightColor = safeColor(b3.avatarColorHex)
                            )
                        )
                        setTextViewText(R.id.tv_team3_name, b3.name)
                        setTextViewText(R.id.tv_team3_desc, "Assistant Terminal · ${b3.model}")
                        setTextViewText(R.id.tv_team3_badge, formatBotStatus(b3.status))
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

    private fun formatBotStatus(status: BotStatus): String = when (status) {
        BotStatus.WORKING -> "Au travail"
        BotStatus.BUSY -> "Occupé"
        BotStatus.IDLE -> "Prêt"
        BotStatus.OFFLINE -> "Hors ligne"
    }
}
package com.ghdinteractivestudio.codestation.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WidgetCategory(val id: String, val title: String, val providerClass: Class<*>) {
    USAGE("usage", "Consommation IA", UsageWidgetProvider::class.java),
    LIMITS("limits", "Limites & Santé Système", LimitsWidgetProvider::class.java),
    BOTS("bots", "Agents & Tâches", BotsWidgetProvider::class.java),
    TEAM("team", "Équipe d'agents", TeamWidgetProvider::class.java);

    companion object {
        fun fromProvider(cls: Class<*>): WidgetCategory? = values().firstOrNull { it.providerClass == cls }
        fun fromId(id: String): WidgetCategory = values().firstOrNull { it.id == id } ?: USAGE
    }
}

data class InstalledWidgetInfo(
    val appWidgetId: Int,
    val category: WidgetCategory,
    val installedAtMillis: Long,
    val lastUpdatedMillis: Long,
    val isActive: Boolean = true
)

/**
 * Single source of truth for CodeStation Android Widgets detection and synchronization.
 * Detects actual widgets placed on the user's home screen via AppWidgetManager,
 * tracks their lifecycle (onEnabled, onUpdate, onDeleted, onDisabled),
 * and syncs their status to the StateScreen.
 */
object WidgetStateRepository {

    private const val PREFS_NAME = "codestation_widgets_state"
    private const val KEY_INSTALLED_IDS = "installed_widget_ids"

    private val _installedWidgetsFlow = MutableStateFlow<List<InstalledWidgetInfo>>(emptyList())
    val installedWidgetsFlow: StateFlow<List<InstalledWidgetInfo>> = _installedWidgetsFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Queries AppWidgetManager for all registered providers, reconciling stored state.
     */
    fun refresh(context: Context): List<InstalledWidgetInfo> {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return emptyList()
        val prefs = getPrefs(context)
        val now = System.currentTimeMillis()

        val results = mutableListOf<InstalledWidgetInfo>()

        for (category in WidgetCategory.values()) {
            val componentName = ComponentName(context, category.providerClass)
            val ids = try {
                appWidgetManager.getAppWidgetIds(componentName)
            } catch (e: Exception) {
                intArrayOf()
            }

            for (widgetId in ids) {
                val installedKey = "widget_${widgetId}_installed_at"
                val updatedKey = "widget_${widgetId}_updated_at"

                val installedAt = prefs.getLong(installedKey, 0L).let {
                    if (it == 0L) {
                        prefs.edit().putLong(installedKey, now).apply()
                        now
                    } else it
                }
                val lastUpdated = prefs.getLong(updatedKey, now)

                results.add(
                    InstalledWidgetInfo(
                        appWidgetId = widgetId,
                        category = category,
                        installedAtMillis = installedAt,
                        lastUpdatedMillis = lastUpdated,
                        isActive = true
                    )
                )
            }
        }

        // Clean up preferences for removed widgets
        val activeIds = results.map { it.appWidgetId.toString() }.toSet()
        val allStoredKeys = prefs.all.keys.toList()
        val editor = prefs.edit()
        for (key in allStoredKeys) {
            if (key.startsWith("widget_")) {
                val parts = key.split("_")
                if (parts.size >= 2) {
                    val idStr = parts[1]
                    if (idStr !in activeIds) {
                        editor.remove(key)
                    }
                }
            }
        }
        editor.putStringSet(KEY_INSTALLED_IDS, activeIds).apply()

        _installedWidgetsFlow.value = results
        return results
    }

    /**
     * Called when a widget update occurs to persist timestamp.
     */
    fun recordWidgetUpdate(context: Context, widgetId: Int, category: WidgetCategory) {
        val now = System.currentTimeMillis()
        getPrefs(context).edit()
            .putLong("widget_${widgetId}_updated_at", now)
            .apply()
        refresh(context)
    }

    /**
     * Called when widgets are deleted by the user on the home screen.
     */
    fun onWidgetsDeleted(context: Context, widgetIds: IntArray) {
        val editor = getPrefs(context).edit()
        for (id in widgetIds) {
            editor.remove("widget_${id}_installed_at")
            editor.remove("widget_${id}_updated_at")
        }
        editor.apply()
        refresh(context)
    }

    /**
     * Returns true if at least one CodeStation widget is currently installed on the launcher.
     */
    fun hasInstalledWidgets(context: Context): Boolean {
        if (_installedWidgetsFlow.value.isNotEmpty()) return true
        return refresh(context).isNotEmpty()
    }
}

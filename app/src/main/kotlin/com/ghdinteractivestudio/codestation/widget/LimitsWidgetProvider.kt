package com.ghdinteractivestudio.codestation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class LimitsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (id in appWidgetIds) {
            WidgetStateRepository.recordWidgetUpdate(context, id, WidgetCategory.LIMITS)
        }
        CodeStationWidgetManager.updateLimitsWidgets(context, appWidgetManager)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetStateRepository.refresh(context)
        CodeStationWidgetManager.updateAllWidgets(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        WidgetStateRepository.onWidgetsDeleted(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetStateRepository.refresh(context)
    }
}
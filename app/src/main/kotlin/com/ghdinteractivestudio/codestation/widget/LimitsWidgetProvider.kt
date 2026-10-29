package com.ghdinteractivestudio.codestation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class LimitsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CodeStationWidgetManager.updateLimitsWidgets(context, appWidgetManager)
    }

    override fun onEnabled(context: Context) {
        CodeStationWidgetManager.updateAllWidgets(context)
    }
}

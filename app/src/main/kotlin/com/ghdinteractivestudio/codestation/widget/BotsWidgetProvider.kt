package com.ghdinteractivestudio.codestation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class BotsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CodeStationWidgetManager.updateBotsWidgets(context, appWidgetManager)
    }

    override fun onEnabled(context: Context) {
        CodeStationWidgetManager.updateAllWidgets(context)
    }
}

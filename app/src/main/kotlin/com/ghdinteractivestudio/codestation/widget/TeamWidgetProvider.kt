package com.ghdinteractivestudio.codestation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class TeamWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CodeStationWidgetManager.updateTeamWidgets(context, appWidgetManager)
    }

    override fun onEnabled(context: Context) {
        CodeStationWidgetManager.updateAllWidgets(context)
    }
}

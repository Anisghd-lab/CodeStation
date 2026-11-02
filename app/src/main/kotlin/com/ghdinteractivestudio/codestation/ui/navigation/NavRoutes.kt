package com.ghdinteractivestudio.codestation.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Pairing : Screen("pairing")
    data object Roster : Screen("roster")
    data object Chat : Screen("chat/{botId}?conversationId={conversationId}") {
        fun createRoute(botId: String, conversationId: String? = null) =
            if (!conversationId.isNullOrBlank()) "chat/$botId?conversationId=$conversationId" else "chat/$botId"
    }
    data object BotEditor : Screen("bot_editor?botId={botId}") {
        fun createRoute(botId: String? = null) = if (botId != null) "bot_editor?botId=$botId" else "bot_editor"
    }
    data object GroupEditor : Screen("group_editor")
    data object Trace : Screen("trace/{botId}") {
        fun createRoute(botId: String) = "trace/$botId"
    }
    data object Approvals : Screen("approvals")
    data object Settings : Screen("settings")
    data object Memory : Screen("memory")
    data object Routines : Screen("routines")
    data object Extensions : Screen("extensions")
    data object Voice : Screen("voice")
    data object CamcsTask : Screen("camcs_task")
    data object CamcsControlCenter : Screen("camcs_control_center?missionId={missionId}") {
        fun createRoute(missionId: String? = null) = if (missionId != null) "camcs_control_center?missionId=$missionId" else "camcs_control_center"
    }
}

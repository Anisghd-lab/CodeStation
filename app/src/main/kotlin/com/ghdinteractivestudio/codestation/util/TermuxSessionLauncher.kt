package com.ghdinteractivestudio.codestation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object TermuxSessionLauncher {

    const val TERMUX_PACKAGE = "com.termux"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    /**
     * Ouvre une session Termux visible exécutant l'environnement Ubuntu PRoot du bot spécifié.
     */
    fun openBotTerminalSession(context: Context, botId: String, engine: String) {
        val launchCmd = "proot-distro login ubuntu -- bash -c '/root/CodeStation/environments/$botId/run.sh bash'"

        // 1. Copie de secours dans le presse-papier
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Termux Bot Command", launchCmd))

        // 2. Envoi de l'intent RUN_COMMAND avec session visible au premier plan
        var serviceStarted = false
        try {
            val runIntent = Intent(ACTION_RUN_COMMAND).apply {
                setPackage(TERMUX_PACKAGE)
                putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(EXTRA_ARGUMENTS, arrayOf("-c", launchCmd))
                putExtra(EXTRA_BACKGROUND, false)
                putExtra(EXTRA_SESSION_ACTION, "0")
            }
            context.startService(runIntent)
            serviceStarted = true
        } catch (_: Exception) {
            serviceStarted = false
        }

        // 3. Basculement vers Termux au premier plan
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val msg = if (serviceStarted) "Session $botId lancée dans Termux" else "Termux ouvert (Commande copiée)"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Application Termux non trouvée", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur d'ouverture Termux: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Ouvre simplement l'application Termux.
     */
    fun openTermux(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Application Termux non trouvée", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible d'ouvrir Termux: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.ghdinteractivestudio.codestation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CodeStationApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = CodeStationContainer(this)

        // Initialisation douce du repository en arrière-plan
        CoroutineScope(Dispatchers.IO).launch {
            container.botRepository.refreshBots()
        }
    }
}

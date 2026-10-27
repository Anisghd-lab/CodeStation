package com.ghdinteractivestudio.synapse

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SynapseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = SynapseContainer(this)

        // Initialisation douce du repository en arrière-plan
        CoroutineScope(Dispatchers.IO).launch {
            container.botRepository.refreshBots()
        }
    }
}

package com.ghdinteractivestudio.codestation.domain.repository

import com.ghdinteractivestudio.codestation.domain.model.CamcsMission
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface pour la supervision et le contrôle temps réel du moteur CAMCS
 * depuis n'importe quel composant de l'application (Dynamic Island, Notch, etc.).
 */
interface CamcsRepository {
    val activeMissionStream: StateFlow<CamcsMission?>
    val allMissionsStream: StateFlow<List<CamcsMission>>
    suspend fun refreshMissions()
    suspend fun stopTask(missionId: String, taskId: String? = null, agentId: String? = null): Boolean
    suspend fun cancelMission(missionId: String): Boolean
}

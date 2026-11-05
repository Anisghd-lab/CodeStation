package com.ghdinteractivestudio.codestation.ui

import com.ghdinteractivestudio.codestation.ui.components.CapsuleIconType
import com.ghdinteractivestudio.codestation.ui.components.symbolRes
import com.ghdinteractivestudio.codestation.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CamcsTaskCardTest {

    @Test
    fun `test CamcsTask route is properly registered`() {
        assertEquals("camcs_task", Screen.CamcsTask.route)
    }

    @Test
    fun `test CamcsControlCenter createRoute resolves correct paths`() {
        assertEquals("camcs_control_center", Screen.CamcsControlCenter.createRoute())
        assertEquals("camcs_control_center", Screen.CamcsControlCenter.createRoute(null))
        assertEquals("camcs_control_center?missionId=m_42", Screen.CamcsControlCenter.createRoute("m_42"))
    }

    @Test
    fun `test CamcsHistoryButton symbol resolves correctly`() {
        assertNotNull("L'icône d'historique clock.arrow.circlepath doit être résolue", symbolRes("clock.arrow.circlepath"))
    }

    @Test
    fun `test CapsuleIconType contains all four required categories`() {
        val types = CapsuleIconType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(CapsuleIconType.Folder))
        assertTrue(types.contains(CapsuleIconType.Globe))
        assertTrue(types.contains(CapsuleIconType.Terminal))
        assertTrue(types.contains(CapsuleIconType.Automation))
    }

    @Test
    fun `test Symbol mapping resolves info and chevron symbols`() {
        assertNotNull(symbolRes("info.circle"))
        assertNotNull(symbolRes("info"))
        assertNotNull(symbolRes("chevron.right"))
        assertNotNull(symbolRes("plus"))
        assertNotNull(symbolRes("folder"))
        assertNotNull(symbolRes("globe"))
        assertNotNull(symbolRes("terminal"))
        assertNotNull(symbolRes("gearshape"))
    }

    @Test
    fun `test Glassmorphic New Task button symbols and structure are valid`() {
        // Tâche 12 : Vérification de la disponibilité des symboles SF utilisés dans le bouton glassmorphic
        assertNotNull("L'icône + doit être disponible pour le badge glassmorphic", symbolRes("plus"))
        assertNotNull("Le chevron droit doit être disponible pour la flèche d'action", symbolRes("chevron.right"))
    }
}

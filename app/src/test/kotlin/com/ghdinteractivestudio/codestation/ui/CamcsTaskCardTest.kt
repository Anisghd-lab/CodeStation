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
}

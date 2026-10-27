package com.ghdinteractivestudio.synapse.domain

import com.ghdinteractivestudio.synapse.data.local.BotEntity
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.domain.model.PermissionPolicy
import com.ghdinteractivestudio.synapse.domain.model.ProviderType
import com.ghdinteractivestudio.synapse.domain.model.WorkspaceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelValidationTest {

    @Test
    fun `test Bot entity to domain roundtrip conversion`() {
        val bot = Bot(
            id = "bot-alpha",
            name = "Alpha Bot",
            description = "Agent de test",
            avatarGlyph = "diamond",
            avatarColorHex = "#00D2FF",
            provider = ProviderType.OPENAI_CODEX,
            model = "gpt-4o",
            isFastMode = true,
            workspaceMode = WorkspaceMode.PERSONAL,
            workspacePath = "/root",
            instructions = "Instructions de test",
            permissionPolicy = PermissionPolicy.ASK_SHELL,
            isLead = true,
            isPinned = true,
            status = BotStatus.WORKING,
            lastMessage = "Analyse terminée.",
            unreadCount = 2,
            pendingApprovalsCount = 1,
            memberBotIds = listOf("sub-1", "sub-2")
        )

        val entity = BotEntity.fromDomain(bot)
        val roundtrip = entity.toDomain()

        assertEquals(bot.id, roundtrip.id)
        assertEquals(bot.name, roundtrip.name)
        assertEquals(bot.provider, roundtrip.provider)
        assertEquals(bot.isLead, roundtrip.isLead)
        assertEquals(bot.isPinned, roundtrip.isPinned)
        assertEquals(bot.status, roundtrip.status)
        assertEquals(bot.memberBotIds, roundtrip.memberBotIds)
        assertTrue(roundtrip.isGroup)
    }

    @Test
    fun `test lead bot cannot be modified to false group status`() {
        val lead = Bot(
            id = "lead-main",
            name = "Synapse Lead",
            isLead = true
        )
        assertTrue(lead.isLead)
        assertEquals(0, lead.memberBotIds.size)
        assertTrue(!lead.isGroup)
    }
}

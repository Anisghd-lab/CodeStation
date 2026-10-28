package com.ghdinteractivestudio.codestation.domain

import android.net.Uri
import com.ghdinteractivestudio.codestation.domain.model.PendingAttachment
import com.ghdinteractivestudio.codestation.util.TranscriptionResult
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentAndVoiceTest {

    @Test
    fun `test PendingAttachment image classification and formatted size`() {
        val testUri = mockk<Uri>(relaxed = true)
        val imageAttachment = PendingAttachment(
            id = "att-1",
            uri = testUri,
            name = "screenshot.png",
            mimeType = "image/png",
            sizeBytes = 1536 * 1024L // 1.5 MB
        )

        assertTrue(imageAttachment.isImage)
        assertFalse(imageAttachment.isVideo)
        assertEquals("1.5 Mo", imageAttachment.formattedSize)
    }

    @Test
    fun `test PendingAttachment video warning flag`() {
        val testUri = mockk<Uri>(relaxed = true)
        val videoAttachment = PendingAttachment(
            id = "att-2",
            uri = testUri,
            name = "demo_recording.mp4",
            mimeType = "video/mp4",
            sizeBytes = 850 * 1024L // 850 KB
        )

        assertFalse(videoAttachment.isImage)
        assertTrue(videoAttachment.isVideo)
        assertEquals("850 Ko", videoAttachment.formattedSize)
    }

    @Test
    fun `test PendingAttachment document file handling`() {
        val testUri = mockk<Uri>(relaxed = true)
        val docAttachment = PendingAttachment(
            id = "att-3",
            uri = testUri,
            name = "report.pdf",
            mimeType = "application/pdf",
            sizeBytes = 500L
        )

        assertFalse(docAttachment.isImage)
        assertFalse(docAttachment.isVideo)
        assertEquals("500 octets", docAttachment.formattedSize)
    }

    @Test
    fun `test voice transcription result sealed class states`() {
        val success = TranscriptionResult.Success("Bonjour CodeStation")
        val partial = TranscriptionResult.Partial("Bonjour")
        val error = TranscriptionResult.Error("Microphone occupé")

        assertEquals("Bonjour CodeStation", success.text)
        assertEquals("Bonjour", partial.text)
        assertEquals("Microphone occupé", error.message)
    }
}

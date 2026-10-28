package com.ghdinteractivestudio.codestation.domain

import android.net.Uri
import com.ghdinteractivestudio.codestation.domain.model.PendingAttachment
import com.ghdinteractivestudio.codestation.util.TranscriptionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class AttachmentAndVoiceTest {

    @Test
    fun `test PendingAttachment image classification and formatted size`() {
        val mockUri = mock(Uri::class.java)
        val imageAttachment = PendingAttachment(
            id = "att-1",
            uri = mockUri,
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
        val mockUri = mock(Uri::class.java)
        val videoAttachment = PendingAttachment(
            id = "att-2",
            uri = mockUri,
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
        val mockUri = mock(Uri::class.java)
        val docAttachment = PendingAttachment(
            id = "att-3",
            uri = mockUri,
            name = "report.pdf",
            mimeType = "application/pdf",
            sizeBytes = 500L
        )

        assertFalse(docAttachment.isImage)
        assertFalse(docAttachment.isVideo)
        assertEquals("500 octets", docAttachment.formattedSize)
    }

    @Test
    fun `test TranscriptionResult sealed class handling`() {
        val success = TranscriptionResult.Success("Bonjour comment vas-tu")
        val error = TranscriptionResult.Error("Microphone muet")
        val partial = TranscriptionResult.Partial("Bonj...")

        assertTrue(success is TranscriptionResult.Success)
        assertEquals("Bonjour comment vas-tu", success.text)

        assertTrue(error is TranscriptionResult.Error)
        assertEquals("Microphone muet", error.message)

        assertTrue(partial is TranscriptionResult.Partial)
        assertEquals("Bonj...", partial.text)
    }
}

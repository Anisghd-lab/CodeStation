package com.ghdinteractivestudio.codestation.domain.model

import android.net.Uri

data class PendingAttachment(
    val id: String,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isImage: Boolean = mimeType.startsWith("image/"),
    val isVideo: Boolean = mimeType.startsWith("video/"),
    val isAudio: Boolean = mimeType.startsWith("audio/"),
    val uploadId: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null
) {
    val formattedSize: String get() {
        return when {
            sizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f Mo", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.0f Ko", sizeBytes / 1024.0)
            else -> "$sizeBytes octets"
        }
    }
}

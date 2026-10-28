package com.ghdinteractivestudio.codestation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorder(private val context: Context) {

    private val tag = "VoiceRecorder"
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L

    var isRecording = false
        private set

    fun startRecording(): Result<File> {
        return runCatching {
            if (isRecording) {
                stopRecording()
            }

            val dir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            Log.d(tag, "Started voice recording at ${file.absolutePath}")
            file
        }
    }

    fun getDurationSeconds(): Int {
        if (!isRecording || recordingStartTime == 0L) return 0
        return ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentOutputFile
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            Log.d(tag, "Stopped voice recording: ${currentOutputFile?.absolutePath}")
            currentOutputFile
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop recording cleanly: ${e.message}")
            recorder?.release()
            recorder = null
            isRecording = false
            currentOutputFile
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                recorder?.apply {
                    stop()
                    release()
                }
            }
        } catch (_: Exception) {
            recorder?.release()
        } finally {
            recorder = null
            isRecording = false
            currentOutputFile?.let {
                if (it.exists()) it.delete()
            }
            currentOutputFile = null
        }
    }

    fun cleanup(file: File? = currentOutputFile) {
        try {
            if (file != null && file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to cleanup voice file: ${e.message}")
        }
    }
}

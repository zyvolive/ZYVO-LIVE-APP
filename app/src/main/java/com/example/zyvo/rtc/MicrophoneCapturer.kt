package com.example.zyvo.rtc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.sqrt

/**
 * MicrophoneCapturer
 * 
 * Manages real Android microphone hardware via AudioRecord.
 * Provides real-time PCM audio capture, hardware mute/unmute control,
 * real-time voice amplitude calculation (isSpeaking detection),
 * and an audio pipeline ready for WebRTC AudioTrack.
 */
class MicrophoneCapturer(private val context: Context) {
    private val tag = "ZYVO_MicrophoneCapturer"

    private val sampleRate = 16000 // Standard voice communication sample rate
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isCapturing: Boolean = false
    private var isMuted: Boolean = false

    // Callbacks
    var onPcmAudioCaptured: ((audioData: ShortArray, size: Int) -> Unit)? = null
    var onAudioLevelChanged: ((level: Float, isSpeaking: Boolean) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun isRecording(): Boolean = isCapturing
    fun isMuted(): Boolean = isMuted

    /**
     * Initializes the AudioRecord hardware instance.
     */
    @SuppressLint("MissingPermission")
    fun initialize(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError?.invoke("Microphone permission not granted")
            return false
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onError?.invoke("Unable to get valid audio buffer size")
                return false
            }

            val bufferSize = maxOf(minBufferSize, 2048)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                onError?.invoke("Failed to initialize AudioRecord hardware")
                return false
            }

            audioRecord = record
            Log.d(tag, "Microphone initialized successfully (sampleRate=$sampleRate, bufferSize=$bufferSize)")
            return true
        } catch (e: SecurityException) {
            Log.e(tag, "SecurityException initializing microphone", e)
            onError?.invoke("Microphone permission denied")
            return false
        } catch (e: Exception) {
            Log.e(tag, "Exception initializing microphone", e)
            onError?.invoke("Microphone hardware error: ${e.localizedMessage ?: "Unknown"}")
            return false
        }
    }

    /**
     * Starts real hardware audio recording loop.
     */
    fun startCapture(): Boolean {
        if (audioRecord == null) {
            val initSuccess = initialize()
            if (!initSuccess) return false
        }

        val record = audioRecord ?: return false

        try {
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                record.startRecording()
            }
            isCapturing = true

            recordingJob?.cancel()
            recordingJob = scope.launch {
                val bufferSize = 1024
                val audioBuffer = ShortArray(bufferSize)

                while (isActive && isCapturing) {
                    if (isMuted) {
                        // REAL MUTE: Suppress audio pipeline completely
                        onAudioLevelChanged?.invoke(0f, false)
                        delay(60)
                        continue
                    }

                    val readCount = record.read(audioBuffer, 0, audioBuffer.size)
                    if (readCount > 0) {
                        // Calculate real Root-Mean-Square (RMS) amplitude from real microphone sound wave
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        // Normalize 0.0 to 1.0 (typical speaking speech RMS is 1000..8000)
                        val level = (rms / 12000.0).toFloat().coerceIn(0f, 1f)
                        val isSpeaking = level > 0.04f

                        onAudioLevelChanged?.invoke(level, isSpeaking)

                        // Forward real PCM audio frames to WebRTC audio pipeline
                        onPcmAudioCaptured?.invoke(audioBuffer, readCount)
                    } else if (readCount < 0) {
                        Log.w(tag, "AudioRecord read returned error code: $readCount")
                        delay(50)
                    }
                }
            }

            Log.d(tag, "Microphone capture started")
            return true
        } catch (e: Exception) {
            Log.e(tag, "Failed to start microphone capture", e)
            onError?.invoke("Could not start microphone: ${e.localizedMessage ?: "Error"}")
            isCapturing = false
            return false
        }
    }

    /**
     * Real mute/unmute control.
     * When muted, actual audio capture is muted and no sound frames are emitted.
     */
    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            onAudioLevelChanged?.invoke(0f, false)
        }
        Log.d(tag, "Microphone mute state changed to: $muted")
    }

    /**
     * Stops audio capture.
     */
    fun stopCapture() {
        isCapturing = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error stopping AudioRecord", e)
        }

        onAudioLevelChanged?.invoke(0f, false)
        Log.d(tag, "Microphone capture stopped")
    }

    /**
     * Releases audio hardware resources completely.
     */
    fun release() {
        stopCapture()
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing AudioRecord", e)
        }
        audioRecord = null
        scope.cancel()
        Log.d(tag, "Microphone resources released")
    }
}

package com.example.zyvo.rtc

import android.content.Context
import android.media.AudioManager
import android.util.Log

/**
 * AudioRouteManager
 *
 * Manages Android device audio routing during live broadcasting and viewing sessions.
 * Switches device audio mode to MODE_IN_COMMUNICATION with speakerphone enabled by default,
 * supporting external Bluetooth headsets/wired headsets, and restoring MODE_NORMAL on session exit.
 */
class AudioRouteManager(private val context: Context) {
    companion object {
        private const val TAG = "ZYVO_AUDIO"
    }

    private val audioManager: AudioManager? = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var isAudioSessionActive = false

    fun startLiveAudioSession() {
        if (isAudioSessionActive) return
        val am = audioManager ?: run {
            Log.w(TAG, "AUDIO_ERROR: AudioManager unavailable")
            return
        }

        try {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = true
            isAudioSessionActive = true
            Log.d(TAG, "AUDIO_ROUTE_CHANGED: Mode set to MODE_IN_COMMUNICATION with speakerphone ON")
        } catch (e: Exception) {
            Log.e(TAG, "AUDIO_ERROR: Failed to set live audio mode", e)
        }
    }

    fun stopLiveAudioSession() {
        if (!isAudioSessionActive) return
        val am = audioManager ?: return

        try {
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
            isAudioSessionActive = false
            Log.d(TAG, "AUDIO_ROUTE_CHANGED: Mode reset to MODE_NORMAL with speakerphone OFF")
        } catch (e: Exception) {
            Log.e(TAG, "AUDIO_ERROR: Failed to reset audio mode", e)
        }
    }
}

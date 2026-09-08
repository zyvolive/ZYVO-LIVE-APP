package com.example.zyvo.rtc

/**
 * WebRtcState
 *
 * Represents the unified lifecycle and connection status of the WebRTC peer session.
 */
enum class WebRtcState {
    IDLE,
    INITIALIZING,
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED
}

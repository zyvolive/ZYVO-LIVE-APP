package com.example.zyvo.rtc

import org.webrtc.PeerConnection

/**
 * RtcConfig
 *
 * Encapsulates ICE servers (STUN/TURN) and WebRTC peer connection configuration parameters.
 */
data class RtcConfig(
    val iceServers: List<PeerConnection.IceServer> = defaultIceServers,
    val enableDtlsSrtp: Boolean = true
) {
    companion object {
        val defaultIceServers: List<PeerConnection.IceServer> = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )
    }
}

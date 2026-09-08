package com.example.zyvo.data.model

import com.google.firebase.firestore.PropertyName
import org.webrtc.SessionDescription

/**
 * SessionDescriptionModel
 *
 * Firestore-serializable representation of a WebRTC SDP SessionDescription (offer or answer).
 */
data class SessionDescriptionModel(
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "",

    @get:PropertyName("sdp")
    @set:PropertyName("sdp")
    var sdp: String = "",

    @get:PropertyName("senderUid")
    @set:PropertyName("senderUid")
    var senderUid: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    fun toWebRtc(): SessionDescription? {
        if (type.isBlank() || sdp.isBlank()) return null
        return try {
            val sdpType = SessionDescription.Type.fromCanonicalForm(type.lowercase())
            SessionDescription(sdpType, sdp)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun fromWebRtc(desc: SessionDescription, senderUid: String = ""): SessionDescriptionModel {
            return SessionDescriptionModel(
                type = desc.type.canonicalForm(),
                sdp = desc.description,
                senderUid = senderUid,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

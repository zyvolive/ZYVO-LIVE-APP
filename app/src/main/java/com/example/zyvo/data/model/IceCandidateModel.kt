package com.example.zyvo.data.model

import com.google.firebase.firestore.PropertyName
import org.webrtc.IceCandidate

/**
 * IceCandidateModel
 *
 * Firestore-serializable representation of a WebRTC IceCandidate.
 */
data class IceCandidateModel(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("candidate")
    @set:PropertyName("candidate")
    var candidate: String = "",

    @get:PropertyName("sdpMid")
    @set:PropertyName("sdpMid")
    var sdpMid: String? = null,

    @get:PropertyName("sdpMLineIndex")
    @set:PropertyName("sdpMLineIndex")
    var sdpMLineIndex: Int? = null,

    @get:PropertyName("serverUrl")
    @set:PropertyName("serverUrl")
    var serverUrl: String? = null,

    @get:PropertyName("senderUid")
    @set:PropertyName("senderUid")
    var senderUid: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = System.currentTimeMillis()
) {
    fun toWebRtc(): IceCandidate? {
        if (candidate.isBlank()) return null
        return try {
            IceCandidate(
                sdpMid ?: "",
                sdpMLineIndex ?: 0,
                candidate
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun fromWebRtc(candidate: IceCandidate, senderUid: String = "", id: String = ""): IceCandidateModel {
            val candidateId = if (id.isNotBlank()) {
                id
            } else {
                val midPart = candidate.sdpMid ?: "0"
                val linePart = candidate.sdpMLineIndex
                val hashPart = candidate.sdp.hashCode()
                "${midPart}_${linePart}_${hashPart}"
            }
            return IceCandidateModel(
                id = candidateId,
                candidate = candidate.sdp,
                sdpMid = candidate.sdpMid,
                sdpMLineIndex = candidate.sdpMLineIndex,
                serverUrl = candidate.serverUrl,
                senderUid = senderUid,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

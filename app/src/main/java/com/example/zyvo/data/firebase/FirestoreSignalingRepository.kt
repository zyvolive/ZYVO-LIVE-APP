package com.example.zyvo.data.firebase

import android.util.Log
import com.example.zyvo.data.model.IceCandidateModel
import com.example.zyvo.data.model.SessionDescriptionModel
import com.example.zyvo.model.LiveRoom
import com.example.zyvo.model.RoomType
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FirestoreSignalingRepository
 *
 * Dedicated repository for Firestore WebRTC signaling and live room metadata.
 * Operates strictly on Firestore documents and collections without holding WebRTC media objects.
 *
 * Firestore Data Schema:
 *   liveRooms/{roomId}                                -> Room metadata (hostId, status, title, viewerCount, etc.)
 *   liveRooms/{roomId}/signaling/offer                -> Host SDP Offer document
 *   liveRooms/{roomId}/signaling/answer               -> Viewer SDP Answer document
 *   liveRooms/{roomId}/hostCandidates/{candidateId}   -> Host ICE candidates collection
 *   liveRooms/{roomId}/viewerCandidates/{candidateId} -> Viewer ICE candidates collection
 */
class FirestoreSignalingRepository(
    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        Log.e("ZYVO_SIGNALING", "Failed to get FirebaseFirestore instance: ${e.message}", e)
        null
    }
) {

    companion object {
        private const val TAG_SIGNALING = "ZYVO_SIGNALING"
        private const val TAG_ROOM = "ZYVO_ROOM"
        const val TAG_ROOM_CREATE = "ZYVO_ROOM_CREATE"
        const val TAG_DISCOVERY = "ZYVO_DISCOVERY"

        const val STATUS_CREATED = "CREATED"
        const val STATUS_LIVE = "LIVE"
        const val STATUS_ENDED = "ENDED"
    }

    // Safe extraction helpers to prevent ClassCastException on Timestamps / Numbers
    private fun parseString(doc: DocumentSnapshot, vararg fieldNames: String): String? {
        for (field in fieldNames) {
            val v = doc.get(field)
            if (v is String && v.isNotBlank()) return v
        }
        return null
    }

    private fun parseLong(doc: DocumentSnapshot, vararg fieldNames: String): Long? {
        for (field in fieldNames) {
            val v = doc.get(field) ?: continue
            when (v) {
                is Number -> return v.toLong()
                is String -> v.toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun parseTimestamp(doc: DocumentSnapshot, vararg fieldNames: String): Long? {
        for (field in fieldNames) {
            val v = doc.get(field) ?: continue
            when (v) {
                is Number -> return v.toLong()
                is com.google.firebase.Timestamp -> return v.toDate().time
                is java.util.Date -> return v.time
                is String -> v.toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    // ==========================================
    // ROOM METADATA & LIFECYCLE
    // ==========================================

    /**
     * Publishes a new live room document to Firestore.
     */
    suspend fun createLiveRoom(room: LiveRoom, hostUid: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: run {
            Log.e(TAG_ROOM_CREATE, "Cannot create room: FirebaseFirestore instance is null")
            return@withContext false
        }
        try {
            val now = System.currentTimeMillis()
            Log.d(TAG_ROOM_CREATE, "Creating live room in Firestore: roomId=${room.id}, hostUid=$hostUid, title=${room.title}")

            val roomData = hashMapOf<String, Any?>(
                "id" to room.id,
                "roomId" to room.id,
                "title" to room.title,
                "description" to room.description,
                "hostId" to hostUid,
                "hostUid" to hostUid,
                "creatorIdentity" to hostUid,
                "hostName" to room.hostName,
                "hostAvatar" to room.hostAvatar,
                "hostAvatarUrl" to room.hostAvatarUrl,
                "hostGender" to room.hostGender,
                "roomCoverUrl" to room.roomCoverUrl,
                "coverStyle" to room.coverStyle,
                "roomType" to room.roomType.name,
                "category" to room.category,
                "tags" to room.tags,
                "viewerCount" to 0,
                "likesCount" to room.likesCount,
                "isLive" to true,
                "status" to STATUS_LIVE,
                "createdAt" to now,
                "startedAt" to now,
                "lastHeartbeat" to now,
                "lastHeartbeatAt" to now
            )

            fs.collection("liveRooms")
                .document(room.id)
                .set(roomData, SetOptions.merge())
                .awaitResult()

            Log.d(TAG_ROOM_CREATE, "Room created successfully in Firestore: id=${room.id}, hostUid=$hostUid, status=$STATUS_LIVE, isLive=true, title=${room.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM_CREATE, "Failed to create room in Firestore: roomId=${room.id}, hostUid=$hostUid, error=${e.message}", e)
            false
        }
    }

    /**
     * Sends periodic heartbeat for an active live room.
     */
    suspend fun sendRoomHeartbeat(roomId: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val now = System.currentTimeMillis()
            fs.collection("liveRooms").document(roomId)
                .update(mapOf(
                    "lastHeartbeatAt" to now,
                    "lastHeartbeat" to now
                ))
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to send room heartbeat for $roomId: ${e.message}")
            false
        }
    }

    /**
     * Updates the status of a live room (e.g. LIVE -> ENDED).
     */
    suspend fun updateRoomStatus(roomId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val isLive = status == STATUS_LIVE
            fs.collection("liveRooms")
                .document(roomId)
                .update(
                    mapOf(
                        "status" to status,
                        "isLive" to isLive,
                        "endedAt" to if (!isLive) System.currentTimeMillis() else null
                    )
                )
                .awaitResult()

            Log.d(TAG_ROOM, "Room $roomId status updated to: $status")
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to update room status: ${e.message}", e)
            false
        }
    }

    /**
     * Updates viewer count for a room.
     */
    suspend fun updateViewerCount(roomId: String, count: Int): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            fs.collection("liveRooms")
                .document(roomId)
                .update("viewerCount", count.coerceAtLeast(1))
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to update viewer count: ${e.message}", e)
            false
        }
    }

    suspend fun incrementViewerCount(roomId: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            fs.collection("liveRooms")
                .document(roomId)
                .update("viewerCount", FieldValue.increment(1))
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to increment viewer count: ${e.message}", e)
            false
        }
    }

    suspend fun decrementViewerCount(roomId: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            fs.collection("liveRooms")
                .document(roomId)
                .update("viewerCount", FieldValue.increment(-1))
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to decrement viewer count: ${e.message}", e)
            false
        }
    }

    suspend fun addParticipant(roomId: String, uid: String, name: String, avatar: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val partData = hashMapOf(
                "uid" to uid,
                "name" to name,
                "avatar" to avatar,
                "joinedAt" to System.currentTimeMillis(),
                "role" to "VIEWER",
                "isActive" to true
            )
            fs.collection("liveRooms").document(roomId)
                .collection("participants").document(uid)
                .set(partData, SetOptions.merge())
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to add participant $uid: ${e.message}")
            false
        }
    }

    suspend fun removeParticipant(roomId: String, uid: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            fs.collection("liveRooms").document(roomId)
                .collection("participants").document(uid)
                .update("isActive", false)
                .awaitResult()
            true
        } catch (e: Exception) {
            Log.e(TAG_ROOM, "Failed to remove participant $uid: ${e.message}")
            false
        }
    }

    /**
     * Observes real-time active live rooms from Firestore where status == LIVE.
     */
    fun observeActiveLiveRooms(): Flow<List<LiveRoom>> = callbackFlow {
        val fs = firestore
        if (fs == null) {
            Log.e(TAG_DISCOVERY, "FirebaseFirestore is null, cannot observe live rooms")
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(TAG_DISCOVERY, "Setting up active live rooms snapshot listener on 'liveRooms' with status == $STATUS_LIVE")
        val colRef = fs.collection("liveRooms")
            .whereEqualTo("status", STATUS_LIVE)

        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_DISCOVERY, "SnapshotListener error observing active live rooms: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val now = System.currentTimeMillis()
                val totalDocs = snapshot.documents.size
                Log.d(TAG_DISCOVERY, "Snapshot listener received $totalDocs room documents from Firestore")

                val roomsList = snapshot.documents.mapNotNull { doc ->
                    val docId = doc.id
                    try {
                        val id = parseString(doc, "id", "roomId") ?: docId
                        val title = parseString(doc, "title") ?: "Live Stream"
                        val description = parseString(doc, "description") ?: ""
                        val hostId = parseString(doc, "hostId", "hostUid", "creatorIdentity") ?: ""
                        val hostName = parseString(doc, "hostName") ?: "Broadcaster"
                        val hostAvatar = parseString(doc, "hostAvatar") ?: "🎙️"
                        val hostAvatarUrl = parseString(doc, "hostAvatarUrl")
                        val hostGender = parseString(doc, "hostGender") ?: "Female"
                        val roomCoverUrl = parseString(doc, "roomCoverUrl")
                        val coverStyle = parseString(doc, "coverStyle") ?: "FULL_BACKDROP"
                        val roomTypeStr = parseString(doc, "roomType") ?: "SINGLE_LIVE"
                        val roomType = try { RoomType.valueOf(roomTypeStr) } catch (_: Exception) { RoomType.SINGLE_LIVE }
                        val category = parseString(doc, "category") ?: "Entertainment"
                        val viewerCount = parseLong(doc, "viewerCount")?.toInt() ?: 1
                        val likesCount = parseLong(doc, "likesCount")?.toInt() ?: 0
                        val status = parseString(doc, "status") ?: STATUS_LIVE
                        val isLive = doc.getBoolean("isLive") ?: status.equals(STATUS_LIVE, ignoreCase = true)
                        val createdAt = parseTimestamp(doc, "createdAt", "startedAt") ?: now
                        val lastHeartbeatAt = parseTimestamp(doc, "lastHeartbeatAt", "lastHeartbeat") ?: createdAt

                        if (!status.equals(STATUS_LIVE, ignoreCase = true) || !isLive) {
                            Log.d(TAG_DISCOVERY, "Skipping doc $docId: status=$status, isLive=$isLive (reason: not actively LIVE)")
                            return@mapNotNull null
                        }

                        // Heartbeat / stale room check with clock-skew protection:
                        // 1. If freshly created within 10 minutes, always allow.
                        // 2. Otherwise require lastHeartbeat within 10 minutes.
                        val timeSinceHeartbeat = now - lastHeartbeatAt
                        val isFreshlyCreated = (now - createdAt) in -600_000L..600_000L
                        if (!isFreshlyCreated && timeSinceHeartbeat > 10 * 60 * 1000L) {
                            Log.w(TAG_DISCOVERY, "Skipping doc $docId: stale heartbeat (lastHeartbeat=${timeSinceHeartbeat}ms ago)")
                            return@mapNotNull null
                        }

                        Log.d(TAG_DISCOVERY, "Accepted active room: docId=$docId, id=$id, title=$title, hostId=$hostId, hostName=$hostName")
                        LiveRoom(
                            id = id,
                            title = title,
                            description = description,
                            creatorIdentity = hostId,
                            hostId = hostId,
                            hostName = hostName,
                            hostAvatar = hostAvatar,
                            hostAvatarUrl = hostAvatarUrl,
                            hostGender = hostGender,
                            roomCoverUrl = roomCoverUrl,
                            coverStyle = coverStyle,
                            roomType = roomType,
                            category = category,
                            viewerCount = viewerCount.coerceAtLeast(1),
                            likesCount = likesCount,
                            isLive = isLive,
                            status = status,
                            createdAt = createdAt,
                            lastHeartbeatAt = lastHeartbeatAt
                        )
                    } catch (e: Exception) {
                        Log.e(TAG_DISCOVERY, "Error parsing active live room doc $docId: ${e.message}", e)
                        null
                    }
                }.sortedByDescending { it.createdAt }

                Log.d(TAG_DISCOVERY, "Active live rooms published to pipeline: ${roomsList.size} rooms (from $totalDocs docs)")
                trySend(roomsList)
            } else {
                Log.d(TAG_DISCOVERY, "Snapshot is null, sending emptyList")
                trySend(emptyList())
            }
        }

        awaitClose {
            Log.d(TAG_DISCOVERY, "Active live rooms listener removed")
            registration.remove()
        }
    }

    /**
     * Observes real-time room metadata from Firestore.
     */
    fun observeRoom(roomId: String): Flow<LiveRoom?> = callbackFlow {
        val docRef = firestore?.collection("liveRooms")?.document(roomId)
        if (docRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_ROOM, "Error observing room $roomId: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    val id = parseString(snapshot, "id", "roomId") ?: roomId
                    val title = parseString(snapshot, "title") ?: "Live Stream"
                    val description = parseString(snapshot, "description") ?: ""
                    val hostId = parseString(snapshot, "hostId", "hostUid", "creatorIdentity") ?: ""
                    val hostName = parseString(snapshot, "hostName") ?: "Broadcaster"
                    val hostAvatar = parseString(snapshot, "hostAvatar") ?: "🎙️"
                    val hostAvatarUrl = parseString(snapshot, "hostAvatarUrl")
                    val hostGender = parseString(snapshot, "hostGender") ?: "Female"
                    val roomCoverUrl = parseString(snapshot, "roomCoverUrl")
                    val coverStyle = parseString(snapshot, "coverStyle") ?: "FULL_BACKDROP"
                    val roomTypeStr = parseString(snapshot, "roomType") ?: "SINGLE_LIVE"
                    val roomType = try { RoomType.valueOf(roomTypeStr) } catch (_: Exception) { RoomType.SINGLE_LIVE }
                    val category = parseString(snapshot, "category") ?: "Entertainment"
                    val viewerCount = parseLong(snapshot, "viewerCount")?.toInt() ?: 1
                    val likesCount = parseLong(snapshot, "likesCount")?.toInt() ?: 0
                    val status = parseString(snapshot, "status") ?: STATUS_LIVE
                    val isLive = snapshot.getBoolean("isLive") ?: status.equals(STATUS_LIVE, ignoreCase = true)
                    val createdAt = parseTimestamp(snapshot, "createdAt", "startedAt") ?: System.currentTimeMillis()
                    val lastHeartbeatAt = parseTimestamp(snapshot, "lastHeartbeatAt", "lastHeartbeat") ?: createdAt

                    val room = LiveRoom(
                        id = id,
                        title = title,
                        description = description,
                        creatorIdentity = hostId,
                        hostId = hostId,
                        hostName = hostName,
                        hostAvatar = hostAvatar,
                        hostAvatarUrl = hostAvatarUrl,
                        hostGender = hostGender,
                        roomCoverUrl = roomCoverUrl,
                        coverStyle = coverStyle,
                        roomType = roomType,
                        category = category,
                        viewerCount = viewerCount,
                        likesCount = likesCount,
                        isLive = isLive,
                        status = status,
                        createdAt = createdAt,
                        lastHeartbeatAt = lastHeartbeatAt
                    )
                    trySend(room)
                } catch (e: Exception) {
                    Log.e(TAG_ROOM, "Error parsing room snapshot for $roomId: ${e.message}", e)
                }
            } else {
                trySend(null)
            }
        }

        awaitClose {
            Log.d(TAG_ROOM, "signaling listeners removed (room): $roomId")
            registration.remove()
        }
    }

    // ==========================================
    // SDP OFFER & ANSWER SIGNALING
    // ==========================================

    /**
     * Publishes host SDP offer to liveRooms/{roomId}/signaling/offer.
     */
    suspend fun publishOffer(roomId: String, offer: SessionDescriptionModel): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val docRef = fs.collection("liveRooms")
                .document(roomId)
                .collection("signaling")
                .document("offer")

            docRef.set(offer).awaitResult()
            Log.d(TAG_SIGNALING, "offer published for room $roomId")
            true
        } catch (e: Exception) {
            Log.e(TAG_SIGNALING, "Failed to publish offer: ${e.message}", e)
            false
        }
    }

    /**
     * Observes real-time host SDP offer from liveRooms/{roomId}/signaling/offer.
     */
    fun observeOffer(roomId: String): Flow<SessionDescriptionModel?> = callbackFlow {
        val docRef = firestore?.collection("liveRooms")
            ?.document(roomId)
            ?.collection("signaling")
            ?.document("offer")

        if (docRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_SIGNALING, "Error observing offer: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val model = snapshot.toObject(SessionDescriptionModel::class.java)
                Log.d(TAG_SIGNALING, "offer received for room $roomId")
                trySend(model)
            } else {
                trySend(null)
            }
        }

        awaitClose {
            Log.d(TAG_SIGNALING, "signaling listeners removed (offer): $roomId")
            registration.remove()
        }
    }

    /**
     * Publishes viewer SDP answer to liveRooms/{roomId}/signaling/answer.
     */
    suspend fun publishAnswer(roomId: String, answer: SessionDescriptionModel): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val docRef = fs.collection("liveRooms")
                .document(roomId)
                .collection("signaling")
                .document("answer")

            docRef.set(answer).awaitResult()
            Log.d(TAG_SIGNALING, "answer published for room $roomId")
            true
        } catch (e: Exception) {
            Log.e(TAG_SIGNALING, "Failed to publish answer: ${e.message}", e)
            false
        }
    }

    /**
     * Observes real-time viewer SDP answer from liveRooms/{roomId}/signaling/answer.
     */
    fun observeAnswer(roomId: String): Flow<SessionDescriptionModel?> = callbackFlow {
        val docRef = firestore?.collection("liveRooms")
            ?.document(roomId)
            ?.collection("signaling")
            ?.document("answer")

        if (docRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_SIGNALING, "Error observing answer: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val model = snapshot.toObject(SessionDescriptionModel::class.java)
                Log.d(TAG_SIGNALING, "answer received for room $roomId")
                trySend(model)
            } else {
                trySend(null)
            }
        }

        awaitClose {
            Log.d(TAG_SIGNALING, "signaling listeners removed (answer): $roomId")
            registration.remove()
        }
    }

    // ==========================================
    // ICE CANDIDATE SIGNALING
    // ==========================================

    /**
     * Publishes a host ICE candidate to liveRooms/{roomId}/hostCandidates/{candidateId}.
     */
    suspend fun publishHostIceCandidate(roomId: String, candidate: IceCandidateModel): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val candId = if (candidate.id.isNotBlank()) candidate.id else "cand_${UUID.randomUUID().toString().take(10)}"
            val model = candidate.copy(id = candId)

            fs.collection("liveRooms")
                .document(roomId)
                .collection("hostCandidates")
                .document(candId)
                .set(model)
                .awaitResult()

            Log.d(TAG_SIGNALING, "ICE candidate published (host): $candId")
            true
        } catch (e: Exception) {
            Log.e(TAG_SIGNALING, "Failed to publish host ICE candidate: ${e.message}", e)
            false
        }
    }

    /**
     * Observes real-time host ICE candidates from liveRooms/{roomId}/hostCandidates.
     * Uses DocumentChange.Type.ADDED to emit candidates as they arrive.
     */
    fun observeHostIceCandidates(roomId: String): Flow<IceCandidateModel> = callbackFlow {
        val colRef = firestore?.collection("liveRooms")
            ?.document(roomId)
            ?.collection("hostCandidates")

        if (colRef == null) {
            close()
            return@callbackFlow
        }

        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_SIGNALING, "Error observing host candidates: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.ADDED) {
                    val candidate = change.document.toObject(IceCandidateModel::class.java)
                    Log.d(TAG_SIGNALING, "ICE candidate received (host): ${candidate.id}")
                    trySend(candidate)
                }
            }
        }

        awaitClose {
            Log.d(TAG_SIGNALING, "signaling listeners removed (hostCandidates): $roomId")
            registration.remove()
        }
    }

    /**
     * Publishes a viewer ICE candidate to liveRooms/{roomId}/viewerCandidates/{candidateId}.
     */
    suspend fun publishViewerIceCandidate(roomId: String, candidate: IceCandidateModel): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val candId = if (candidate.id.isNotBlank()) candidate.id else "cand_${UUID.randomUUID().toString().take(10)}"
            val model = candidate.copy(id = candId)

            fs.collection("liveRooms")
                .document(roomId)
                .collection("viewerCandidates")
                .document(candId)
                .set(model)
                .awaitResult()

            Log.d(TAG_SIGNALING, "ICE candidate published (viewer): $candId")
            true
        } catch (e: Exception) {
            Log.e(TAG_SIGNALING, "Failed to publish viewer ICE candidate: ${e.message}", e)
            false
        }
    }

    /**
     * Observes real-time viewer ICE candidates from liveRooms/{roomId}/viewerCandidates.
     * Uses DocumentChange.Type.ADDED to emit candidates as they arrive.
     */
    fun observeViewerIceCandidates(roomId: String): Flow<IceCandidateModel> = callbackFlow {
        val colRef = firestore?.collection("liveRooms")
            ?.document(roomId)
            ?.collection("viewerCandidates")

        if (colRef == null) {
            close()
            return@callbackFlow
        }

        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG_SIGNALING, "Error observing viewer candidates: ${error.message}")
                return@addSnapshotListener
            }
            snapshot?.documentChanges?.forEach { change ->
                if (change.type == DocumentChange.Type.ADDED) {
                    val candidate = change.document.toObject(IceCandidateModel::class.java)
                    Log.d(TAG_SIGNALING, "ICE candidate received (viewer): ${candidate.id}")
                    trySend(candidate)
                }
            }
        }

        awaitClose {
            Log.d(TAG_SIGNALING, "signaling listeners removed (viewerCandidates): $roomId")
            registration.remove()
        }
    }

    /**
     * Cleans up signaling documents (offer, answer, candidates) when a session ends.
     */
    suspend fun clearSignaling(roomId: String): Boolean = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext false
        try {
            val roomRef = fs.collection("liveRooms").document(roomId)

            // Delete offer and answer documents
            try { roomRef.collection("signaling").document("offer").delete().awaitResult() } catch (_: Exception) {}
            try { roomRef.collection("signaling").document("answer").delete().awaitResult() } catch (_: Exception) {}

            // Batch delete host candidates
            val hostCands = roomRef.collection("hostCandidates").get().awaitResult()
            if (!hostCands.isEmpty) {
                val batch = fs.batch()
                for (doc in hostCands.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitResult()
            }

            // Batch delete viewer candidates
            val viewerCands = roomRef.collection("viewerCandidates").get().awaitResult()
            if (!viewerCands.isEmpty) {
                val batch = fs.batch()
                for (doc in viewerCands.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().awaitResult()
            }

            Log.d(TAG_SIGNALING, "Signaling data cleared for room $roomId")
            true
        } catch (e: Exception) {
            Log.e(TAG_SIGNALING, "Failed to clear signaling for room $roomId: ${e.message}", e)
            false
        }
    }
}

/**
 * Extension to safely await Task completion in Kotlin Coroutines.
 */
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
}

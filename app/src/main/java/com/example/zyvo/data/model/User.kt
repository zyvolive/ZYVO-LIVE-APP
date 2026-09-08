package com.example.zyvo.data.model

import androidx.annotation.Keep
import com.example.zyvo.model.UserProfile
import com.example.zyvo.model.VipTier

@Keep
data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatar: String = "👑",
    val createdAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true
) {
    /**
     * Maps the core authenticated User model to the visual UserProfile used by ZYVO UI
     */
    fun toUserProfile(
        bio: String = "Official ZYVO Broadcaster 🎙️ Live on ZYVO!",
        gender: String = "Unspecified",
        location: String = "Global HQ 🌍",
        userLevel: Int = 1,
        userXp: Int = 0,
        followersCount: Int = 0,
        followingCount: Int = 3,
        likesCount: Int = 0,
        vipTier: VipTier = VipTier.NONE
    ): UserProfile {
        val isEmoji = avatar.isNotBlank() && !avatar.startsWith("http")
        return UserProfile(
            userId = uid,
            username = username.ifBlank { "user_${uid.take(6)}" },
            displayName = displayName.ifBlank { "ZYVO Broadcaster" },
            avatarEmoji = if (isEmoji) avatar else "👑",
            avatarUrl = if (!isEmoji && avatar.startsWith("http")) avatar else null,
            bio = bio,
            gender = gender,
            location = location,
            userLevel = userLevel,
            userXp = userXp,
            followersCount = followersCount,
            followingCount = followingCount,
            followingUserIds = listOf("ceo_rayan", "co_founder_alpha", "ansharah_gahni"),
            likesCount = likesCount,
            vipTier = vipTier,
            badges = listOf("Verified Broadcaster"),
            isLiveNow = false
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                uid = map["uid"] as? String ?: "",
                username = map["username"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                avatar = map["avatar"] as? String ?: "👑",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isOnline = map["isOnline"] as? Boolean ?: true
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "displayName" to displayName,
            "avatar" to avatar,
            "createdAt" to createdAt,
            "isOnline" to isOnline
        )
    }
}

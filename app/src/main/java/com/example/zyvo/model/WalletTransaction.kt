package com.example.zyvo.model

import kotlinx.serialization.Serializable

enum class TransactionType {
    COIN_RECHARGE,
    GIFT_SENT,
    GIFT_RECEIVED,
    VIP_PURCHASE,
    WITHDRAWAL,
    EARNINGS_CONVERSION
}

enum class TransactionStatus {
    COMPLETED,
    PENDING,
    FAILED
}

@Serializable
data class WalletTransaction(
    val id: String,
    val title: String,
    val detail: String,
    val coinAmount: Int = 0, // Positive for gain, negative for spent
    val beanAmount: Int = 0,
    val usdAmount: Double = 0.0,
    val type: TransactionType,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val timestampFormatted: String,
    val iconEmoji: String = "🪙"
)

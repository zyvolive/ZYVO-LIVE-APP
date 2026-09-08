package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.theme.*

@Composable
fun VipStoreDialog(
    userCoinBalance: Int,
    currentVipTier: VipTier,
    onDismiss: () -> Unit,
    onBuyVip: (tier: VipTier, months: Int, cost: Int) -> Unit,
    onOpenRecharge: () -> Unit
) {
    var selectedTier by remember { mutableStateOf(VipTier.VIP_3) }
    var selectedDurationMonths by remember { mutableStateOf(1) }

    val vipList = listOf(
        VipTier.VIP_1, VipTier.VIP_2, VipTier.VIP_3, VipTier.VIP_4, VipTier.VIP_5,
        VipTier.SVIP_1, VipTier.SVIP_2, VipTier.SVIP_3
    )

    val durationMultiplier = when (selectedDurationMonths) {
        3 -> 2.6 // Discounted
        12 -> 8.0 // Huge discount
        else -> 1.0
    }

    val finalCost = (selectedTier.monthlyCoinPrice * durationMultiplier).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("vip_store_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "VIP & SVIP Store", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Wallet Pill Banner
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCardElevated)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Your Coin Balance", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(text = "$userCoinBalance Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)
                            }
                        }

                        TextButton(
                            onClick = onOpenRecharge,
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                        ) {
                            Text(text = "+ Recharge", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tier Selector Cards List
                item {
                    Text(text = "SELECT MEMBERSHIP TIER", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)
                }

                items(vipList) { tier ->
                    val isSelected = selectedTier == tier
                    val isCurrent = currentVipTier == tier
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    if (tier.isSvip) Brush.linearGradient(listOf(Color(0xFF4A0033), Color(0xFF1F0014)))
                                    else Brush.linearGradient(listOf(Color(0xFF1E0C36), Color(0xFF0F061C)))
                                } else SolidColor(DarkCardElevated)
                            )
                            .border(
                                1.5.dp,
                                if (isSelected) Color(android.graphics.Color.parseColor(tier.avatarBorderColorHex)) else OverlayLight,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedTier = tier }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Color(android.graphics.Color.parseColor(tier.avatarBorderColorHex)).copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = if (tier.isSvip) "🌟" else "👑", fontSize = 22.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = tier.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "(Active)", style = MaterialTheme.typography.labelSmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(text = tier.entranceEffect, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${tier.monthlyCoinPrice} Coins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)
                                Text(text = "/ month", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }

                // Duration Selector Row
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "SELECT DURATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple(1, "1 Month", "Standard"),
                            Triple(3, "3 Months", "Save 15%"),
                            Triple(12, "1 Year", "Save 33%")
                        ).forEach { (m, label, badge) ->
                            val isSelected = selectedDurationMonths == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) NeonPurple else DarkCardElevated)
                                    .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(14.dp))
                                    .clickable { selectedDurationMonths = m }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 13.sp)
                                    Text(text = badge, fontSize = 10.sp, color = if (isSelected) GoldAccent else TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userCoinBalance >= finalCost) {
                        onBuyVip(selectedTier, selectedDurationMonths, finalCost)
                    } else {
                        onOpenRecharge()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userCoinBalance >= finalCost) ElectricMagenta else LiveRed
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vip_store_buy_btn")
            ) {
                Text(
                    text = if (userCoinBalance >= finalCost) "Activate ${selectedTier.displayName} ($finalCost Coins)" else "Insufficient Coins — Recharge Now",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    )
}

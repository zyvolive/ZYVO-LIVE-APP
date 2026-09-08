package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.Gift
import com.example.zyvo.model.GiftRarity
import com.example.zyvo.model.PredefinedGifts
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftDialog(
    userCoinBalance: Int,
    onDismiss: () -> Unit,
    onSendGift: (Gift, Int) -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf(0) } // 0: All/Popular, 1: Luxury, 2: PK Boosters, 3: VIP Special
    var selectedGift by remember { mutableStateOf<Gift?>(PredefinedGifts.ALL_GIFTS.firstOrNull()) }
    var selectedMultiplier by remember { mutableStateOf(1) }

    val displayedGifts = remember(selectedCategoryTab) {
        when (selectedCategoryTab) {
            1 -> PredefinedGifts.ALL_GIFTS.filter { it.rarity == GiftRarity.EPIC || it.rarity == GiftRarity.LEGENDARY }
            2 -> PredefinedGifts.ALL_GIFTS.filter { it.coinCost in 50..1200 }
            3 -> PredefinedGifts.ALL_GIFTS.filter { it.rarity == GiftRarity.LEGENDARY || it.coinCost >= 500 }
            else -> PredefinedGifts.ALL_GIFTS
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface.copy(alpha = 0.98f),
        scrimColor = OverlayDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row: Tabs & Coin Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("🔥 Hot", "💎 Luxury", "⚔️ PK", "👑 VIP").forEachIndexed { index, title ->
                        val isTabSelected = selectedCategoryTab == index
                        Column(
                            modifier = Modifier
                                .clickable { selectedCategoryTab = index }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isTabSelected) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isTabSelected) TextPrimary else TextMuted
                            )
                            if (isTabSelected) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(ElectricMagenta)
                                )
                            }
                        }
                    }
                }

                // Balance Pill with quick recharge prompt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkCardElevated)
                        .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🪙", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$userCoinBalance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gift Grid (4 Columns Yeah! Live Kit Layout)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedGifts) { gift ->
                    val isSelected = selectedGift?.id == gift.id
                    val rarityColor = when (gift.rarity) {
                        GiftRarity.COMMON -> NeonCyan
                        GiftRarity.RARE -> CyberBlue
                        GiftRarity.EPIC -> ElectricMagenta
                        GiftRarity.LEGENDARY -> GoldAccent
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) {
                                    Brush.verticalGradient(
                                        listOf(DarkCardElevated, ElectricMagenta.copy(alpha = 0.25f))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(DarkCard, DarkCard)
                                    )
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                brush = if (isSelected) Brush.horizontalGradient(listOf(ElectricMagenta, GoldAccent)) else Brush.linearGradient(listOf(OverlayLight, Color.Transparent)),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { selectedGift = gift }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = gift.iconEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = gift.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🪙", fontSize = 9.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${gift.coinCost}",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Combo Multipliers & Send Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Multipliers: 1x, 10x, 66x, 99x, 520x
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        1 to "1",
                        10 to "10",
                        66 to "66",
                        99 to "99",
                        520 to "520"
                    ).forEach { (count, label) ->
                        val isMulSelected = selectedMultiplier == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMulSelected) ElectricMagenta else DarkCardElevated)
                                .border(1.dp, if (isMulSelected) GoldAccent else OverlayLight, RoundedCornerShape(10.dp))
                                .clickable { selectedMultiplier = count }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${label}x",
                                fontSize = 11.sp,
                                fontWeight = if (isMulSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isMulSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                // Send Button with animated gradient
                val totalCost = (selectedGift?.coinCost ?: 0) * selectedMultiplier
                Button(
                    onClick = {
                        val gift = selectedGift
                        if (gift != null) {
                            onSendGift(gift, selectedMultiplier)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .testTag("confirm_send_gift_button")
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(ElectricMagenta, NeonPurple)
                            )
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SEND ($totalCost 🪙)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

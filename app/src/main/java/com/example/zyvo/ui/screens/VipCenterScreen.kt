package com.example.zyvo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.VipTier
import com.example.zyvo.ui.theme.*

data class VipPrivilegeItem(
    val title: String,
    val icon: String,
    val isSvipExclusive: Boolean = false
)

@Composable
fun VipCenterScreen(
    currentVipTier: VipTier,
    coinBalance: Int,
    onBack: () -> Unit,
    onBuyVip: (VipTier, Int, Int) -> Unit,
    onOpenRecharge: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: VIP, 1: SVIP
    var selectedPlanIndex by remember { mutableStateOf(0) } // 0: Monthly, 1: Quarterly, 2: Yearly

    val privileges = remember {
        listOf(
            VipPrivilegeItem("VIP Badge", "👑"),
            VipPrivilegeItem("Exclusive Frame", "🖼️"),
            VipPrivilegeItem("Chat Bubble", "💬"),
            VipPrivilegeItem("VIP Gifts", "🎁"),
            VipPrivilegeItem("Room Effects", "🔮"),
            VipPrivilegeItem("Entry Effects", "⚡"),
            VipPrivilegeItem("Name Color", "🎨"),
            VipPrivilegeItem("More", "⚙️")
        )
    }

    val svipPrivileges = remember {
        listOf(
            VipPrivilegeItem("SVIP Crown", "🌟", true),
            VipPrivilegeItem("Golden Dragon Frame", "🐲", true),
            VipPrivilegeItem("Cosmic Chat Bubble", "🌌", true),
            VipPrivilegeItem("Supernova Gifts", "🪐", true),
            VipPrivilegeItem("Coronation Effects", "👑", true),
            VipPrivilegeItem("Dragon Mount Entry", "🐉", true),
            VipPrivilegeItem("Rainbow Name Glow", "✨", true),
            VipPrivilegeItem("SVIP Concierge", "💎", true)
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "VIP Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedTab == 0) FontWeight.Black else FontWeight.Medium,
                        color = if (selectedTab == 0) GoldAccent else TextMuted,
                        modifier = Modifier.clickable { selectedTab = 0 }
                    )
                    Text(
                        text = "SVIP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selectedTab == 1) FontWeight.Black else FontWeight.Medium,
                        color = if (selectedTab == 1) NeonCyan else TextMuted,
                        modifier = Modifier.clickable { selectedTab = 1 }
                    )
                }

                Text(
                    text = "History",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.clickable { /* open transactions */ }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card Banner (Exact visual style of reference)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selectedTab == 0) {
                            Brush.linearGradient(
                                listOf(Color(0xFF4A0033), Color(0xFF260538), Color(0xFF140726))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color(0xFF003D4D), Color(0xFF091F38), Color(0xFF0A0714))
                            )
                        }
                    )
                    .border(
                        1.5.dp,
                        if (selectedTab == 0) GoldAccent.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.8f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == 0) "VIP 7" else "SVIP 3",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTab == 0) "Enjoy exclusive VIP privileges & 20% coin bonus" else "Ultimate SVIP status & Golden Dragon entrance",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "8750/10000",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Upgrade",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { 0.875f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (selectedTab == 0) GoldAccent else NeonCyan,
                            trackColor = DarkSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Badge Crown Icon
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GoldAccent.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                            .border(2.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (selectedTab == 0) "👑" else "🐉", fontSize = 28.sp)
                            Text(
                                text = if (selectedTab == 0) "7" else "3",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAccent
                            )
                        }
                    }
                }
            }

            // VIP Privileges Section
            Text(
                text = if (selectedTab == 0) "VIP Privileges" else "SVIP Privileges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            val displayPrivileges = if (selectedTab == 0) privileges else svipPrivileges

            // 4x2 Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                displayPrivileges.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurface)
                                        .border(1.dp, OverlayLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.icon, fontSize = 24.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // VIP Plans Section
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "VIP Plans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val plans = listOf(
                    Triple("Monthly", "$4.99", "HOT"),
                    Triple("Quarterly", "$12.99", "SAVE 15%"),
                    Triple("Yearly", "$49.99", "SAVE 35%")
                )

                plans.forEachIndexed { index, (duration, price, tag) ->
                    val isSelected = selectedPlanIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFF2B0A3D) else DarkSurface)
                            .border(
                                1.5.dp,
                                if (isSelected) ElectricMagenta else OverlayLight,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlanIndex = index }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ElectricMagenta)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = price,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Become VIP Action Button
            Button(
                onClick = {
                    val targetTier = if (selectedTab == 0) VipTier.VIP_4 else VipTier.SVIP_1
                    onBuyVip(targetTier, 1, 5000)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("vip_center_buy_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(ElectricMagenta, NeonPurple)
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "Become VIP" else "Activate SVIP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

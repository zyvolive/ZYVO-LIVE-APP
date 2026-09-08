package com.example.zyvo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
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
import com.example.zyvo.ui.theme.*

data class WalletActionShortcut(
    val title: String,
    val icon: String,
    val onClickAction: String
)

@Composable
fun WalletScreen(
    coinBalance: Int,
    beansBalance: Int,
    onBack: () -> Unit,
    onOpenRecharge: () -> Unit,
    onOpenVip: () -> Unit,
    onOpenWithdrawal: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    val usdValue = coinBalance / 1000.0

    val shortcuts = remember {
        listOf(
            WalletActionShortcut("Recharge", "⚡", "recharge"),
            WalletActionShortcut("VIP", "💎", "vip"),
            WalletActionShortcut("SVIP", "👑", "svip"),
            WalletActionShortcut("Earnings", "💵", "earnings"),
            WalletActionShortcut("Transactions", "📑", "transactions"),
            WalletActionShortcut("Withdraw", "🏦", "withdraw"),
            WalletActionShortcut("Bank", "🏛️", "bank"),
            WalletActionShortcut("More", "⚙️", "more")
        )
    }

    val packages = remember {
        listOf(
            Quadruple("100", "+10", "$0.99", null),
            Quadruple("550", "+65", "$4.99", null),
            Quadruple("1200", "+180", "$9.99", "HOT"),
            Quadruple("3500", "+700", "$24.99", null)
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

                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                IconButton(onClick = onOpenTransactions) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "History", tint = NeonCyan)
                }
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
            // My Balance Card (Exact reference design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF230C36), Color(0xFF140824))
                        )
                    )
                    .border(1.5.dp, OverlayLight, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🪙", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%,d", coinBalance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "= \$${String.format("%.2f", usdValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    // Top Up Pink Pill Button
                    Button(
                        onClick = onOpenRecharge,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricMagenta),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("wallet_top_up_button")
                    ) {
                        Text("Top Up", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }

            // Shortcuts Grid (8 Circular Action Buttons)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                shortcuts.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowItems.forEach { shortcut ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        when (shortcut.onClickAction) {
                                            "recharge" -> onOpenRecharge()
                                            "vip", "svip" -> onOpenVip()
                                            "earnings", "withdraw" -> onOpenWithdrawal()
                                            "transactions" -> onOpenTransactions()
                                        }
                                    }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurface)
                                        .border(1.dp, OverlayLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = shortcut.icon, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = shortcut.title,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Coin Packages Section
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Coin Packages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "View all >",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.clickable { onOpenRecharge() }
                )
            }

            // 2x2 Package Cards Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                packages.chunked(2).forEach { rowPackages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPackages.forEach { (coins, bonus, price, tag) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurface)
                                    .border(1.dp, OverlayLight, RoundedCornerShape(16.dp))
                                    .clickable { onOpenRecharge() }
                                    .padding(14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (tag != null) {
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
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    Text(
                                        text = coins,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )

                                    Text(
                                        text = bonus,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonPurpleDark)
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = price,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

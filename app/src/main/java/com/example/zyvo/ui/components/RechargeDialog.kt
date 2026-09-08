package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.ui.theme.*

data class CoinPackage(
    val id: String,
    val coins: Int,
    val bonusCoins: Int,
    val priceUsd: Double,
    val isPopular: Boolean = false
)

@Composable
fun RechargeDialog(
    userCoinBalance: Int,
    onDismiss: () -> Unit,
    onRecharge: (title: String, coins: Int, bonus: Int, priceUsd: Double, method: String) -> Unit
) {
    var selectedPackage by remember {
        mutableStateOf(CoinPackage("p3", 1200, 200, 9.99, isPopular = true))
    }
    var selectedPaymentMethod by remember { mutableStateOf("Google Pay") }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    val packages = listOf(
        CoinPackage("p1", 100, 0, 0.99),
        CoinPackage("p2", 550, 50, 4.99),
        CoinPackage("p3", 1200, 200, 9.99, isPopular = true),
        CoinPackage("p4", 3500, 700, 24.99),
        CoinPackage("p5", 7500, 2000, 49.99),
        CoinPackage("p6", 16000, 5000, 99.99)
    )

    val paymentMethods = listOf("Google Pay", "Credit Card", "Crypto Wallet", "PayPal")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recharge_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪙", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Recharge Coins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Balance Header Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2C134A), Color(0xFF160A29))
                            )
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "CURRENT COIN BALANCE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "$userCoinBalance Coins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = GoldAccent)
                    }
                    Text(text = "⚡ Instant Delivery", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }

                Text(text = "SELECT COIN PACKAGE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)

                // Package Cards Grid (2 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    items(packages) { pkg ->
                        val isSelected = selectedPackage.id == pkg.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) NeonPurpleDark else DarkCardElevated)
                                .border(
                                    1.5.dp,
                                    if (isSelected) NeonCyan else OverlayLight,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPackage = pkg }
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (pkg.isPopular) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricMagenta)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "MOST POPULAR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "🪙", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${pkg.coins}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = TextPrimary)
                                }

                                if (pkg.bonusCoins > 0) {
                                    Text(
                                        text = "+${pkg.bonusCoins} BONUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldAccent
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurface)
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$${pkg.priceUsd}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Method Selector
                Text(text = "PAYMENT METHOD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else DarkCardElevated)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(10.dp))
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = method, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRecharge(
                        "${selectedPackage.coins} Coins Package",
                        selectedPackage.coins,
                        selectedPackage.bonusCoins,
                        selectedPackage.priceUsd,
                        selectedPaymentMethod
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recharge_pay_now_btn")
            ) {
                Text(
                    text = "Pay $${selectedPackage.priceUsd} for ${selectedPackage.coins + selectedPackage.bonusCoins} Coins",
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    )
}

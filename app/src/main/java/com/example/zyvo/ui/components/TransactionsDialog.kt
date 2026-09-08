package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.TransactionType
import com.example.zyvo.model.WalletTransaction
import com.example.zyvo.ui.theme.*

@Composable
fun TransactionsDialog(
    transactions: List<WalletTransaction>,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredList = remember(transactions, selectedFilter) {
        if (selectedFilter == null) transactions
        else transactions.filter { it.type == selectedFilter }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transactions_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📜", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Transaction History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        null to "All",
                        TransactionType.COIN_RECHARGE to "Recharges",
                        TransactionType.GIFT_SENT to "Gifts",
                        TransactionType.WITHDRAWAL to "Payouts"
                    ).forEach { (type, label) ->
                        val isSelected = selectedFilter == type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else DarkCardElevated)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = type }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No transaction records found.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { tx ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkCardElevated)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = tx.iconEmoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = tx.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = tx.detail, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            Text(text = tx.timestampFormatted, fontSize = 10.sp, color = TextMuted)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (tx.coinAmount != 0) {
                                            val sign = if (tx.coinAmount > 0) "+" else ""
                                            Text(
                                                text = "$sign${tx.coinAmount} Coins",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.coinAmount > 0) GoldAccent else LiveRed
                                            )
                                        }
                                        if (tx.usdAmount > 0) {
                                            Text(
                                                text = "$${String.format("%.2f", tx.usdAmount)} USD",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(EmeraldGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(text = tx.status.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = NeonCyan)
            }
        }
    )
}

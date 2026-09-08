package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.zyvo.ui.theme.*

@Composable
fun WithdrawalDialog(
    userBeansBalance: Int,
    onDismiss: () -> Unit,
    onWithdraw: (beans: Int, method: String, accountDetail: String) -> Unit
) {
    var beansInput by remember { mutableStateOf(userBeansBalance.toString()) }
    var selectedPayoutMethod by remember { mutableStateOf("PayPal") }
    var accountDetail by remember { mutableStateOf("alex.vance@example.com") }

    val beans = beansInput.toIntOrNull() ?: 0
    val usdValue = beans / 100.0
    val isValidAmount = beans in 1000..userBeansBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("withdrawal_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💸", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Host Earnings Payout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
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
                // Balance Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCardElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "AVAILABLE EARNINGS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = "$userBeansBalance Beans ($${String.format("%.2f", userBeansBalance / 100.0)})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    }
                    Text(text = "100 Beans = $1.00 USD", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }

                Text(text = "CASHOUT AMOUNT (MIN 1,000 BEANS)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)

                OutlinedTextField(
                    value = beansInput,
                    onValueChange = { beansInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Beans to Cash Out", color = TextMuted) },
                    trailingIcon = { Text("Beans", color = GoldAccent, modifier = Modifier.padding(end = 12.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = OverlayLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                // Cash Out USD Equivalent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Estimated USD Payout:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(text = "$${String.format("%.2f", usdValue)} USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = EmeraldGreen)
                }

                Text(text = "SELECT PAYOUT METHOD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NeonPurpleLight)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PayPal", "Bank Wire", "USDT Crypto").forEach { method ->
                        val isSelected = selectedPayoutMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else DarkCardElevated)
                                .border(1.dp, if (isSelected) NeonCyan else OverlayLight, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedPayoutMethod = method
                                    accountDetail = when (method) {
                                        "PayPal" -> "alex.vance@example.com"
                                        "Bank Wire" -> "ACH Account ****8821"
                                        else -> "0x7F...9A2C"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = method, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }

                OutlinedTextField(
                    value = accountDetail,
                    onValueChange = { accountDetail = it },
                    label = { Text("$selectedPayoutMethod Destination Account", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = OverlayLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidAmount) {
                        onWithdraw(beans, selectedPayoutMethod, accountDetail)
                    }
                },
                enabled = isValidAmount,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("withdrawal_confirm_btn")
            ) {
                Text(
                    text = if (isValidAmount) "Withdraw $${String.format("%.2f", usdValue)} to $selectedPayoutMethod" else "Enter Valid Amount (Min 1,000 Beans)",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    )
}

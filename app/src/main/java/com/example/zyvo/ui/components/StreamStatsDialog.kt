package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.model.StreamStats
import com.example.zyvo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamStatsDialog(
    stats: StreamStats,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        scrimColor = OverlayDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Stream WebRTC Telemetry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryRow("Connection Quality", stats.connectionQuality, EmeraldGreen)
                TelemetryRow("Resolution & FPS", "${stats.resolution} (${stats.fps} FPS)", NeonCyan)
                TelemetryRow("Bitrate", "${stats.bitrateKbps} kbps", TextPrimary)
                TelemetryRow("Latency (RTT)", "${stats.latencyMs} ms", if (stats.latencyMs < 100) EmeraldGreen else GoldAccent)
                TelemetryRow("Packet Loss", "${stats.packetLossPercent}%", EmeraldGreen)
                TelemetryRow("Video Codec", stats.videoCodec, TextSecondary)
                TelemetryRow("Audio Codec", stats.audioCodec, TextSecondary)
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

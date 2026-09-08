package com.example.zyvo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zyvo.ui.theme.*

data class SoundEffectItem(val name: String, val emoji: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSoundboardDialog(
    onDismiss: () -> Unit,
    onPlaySfx: (String) -> Unit
) {
    val soundEffects = listOf(
        SoundEffectItem("Applause", "👏", "Crowd"),
        SoundEffectItem("Airhorn", "📢", "Hype"),
        SoundEffectItem("Cheer", "🎉", "Crowd"),
        SoundEffectItem("Laugh Track", "😂", "Comedy"),
        SoundEffectItem("Drumroll", "🥁", "Tension"),
        SoundEffectItem("DJ Drop", "💽", "Music"),
        SoundEffectItem("Laser Blast", "🔫", "Gaming"),
        SoundEffectItem("Victory Fanfare", "🎺", "Celebration"),
        SoundEffectItem("Gong Echo", "🔔", "Zen")
    )

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
                Text(text = "🔊", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Studio Soundboard SFX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(soundEffects) { sfx ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkCard)
                            .border(1.dp, OverlayLight, RoundedCornerShape(14.dp))
                            .clickable {
                                onPlaySfx(sfx.name)
                                onDismiss()
                            }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = sfx.emoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sfx.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

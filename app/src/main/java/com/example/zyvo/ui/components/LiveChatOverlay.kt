package com.example.zyvo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.CardGiftcard
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
import com.example.zyvo.model.ChatMessage
import com.example.zyvo.model.MessageType
import com.example.zyvo.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LiveChatOverlay(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    floatingGifts: List<ChatMessage>,
    enableChat: Boolean = true,
    onSendMessage: (String) -> Unit,
    onSendLike: () -> Unit,
    onOpenGiftDialog: () -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Floating Big Gift Banner Animations
        AnimatedVisibility(
            visible = floatingGifts.isNotEmpty(),
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            val topGift = floatingGifts.lastOrNull()
            if (topGift != null && topGift.gift != null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    NeonPurple.copy(alpha = 0.9f),
                                    ElectricMagenta.copy(alpha = 0.9f),
                                    DarkBackground.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(1.5.dp, GoldAccent, RoundedCornerShape(30.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = topGift.senderAvatar, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = topGift.senderName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sent ${topGift.giftCount}x ${topGift.gift.name} ${topGift.gift.iconEmoji}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Chat Stream Window
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 190.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Reaction Emoji Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("❤️", "🔥", "💎", "🚀", "👏", "🥳").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OverlayBackground)
                        .border(1.dp, OverlayLight, CircleShape)
                        .clickable {
                            onSendMessage(emoji)
                            onSendLike()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Chat Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Text Input Field
            TextField(
                value = textState,
                onValueChange = { textState = it },
                enabled = enableChat,
                placeholder = {
                    Text(
                        text = if (enableChat) "Say something friendly…" else "Chat is paused by host",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface.copy(alpha = 0.85f),
                    unfocusedContainerColor = DarkSurface.copy(alpha = 0.85f),
                    disabledContainerColor = DarkBackground.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                trailingIcon = {
                    if (textState.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSendMessage(textState)
                                textState = ""
                                scope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = NeonCyan
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Gift Button
            IconButton(
                onClick = onOpenGiftDialog,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPurple, ElectricMagenta)))
                    .testTag("open_gift_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "Send Gift",
                    tint = TextPrimary
                )
            }

            // Like / Heart Button
            IconButton(
                onClick = onSendLike,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ElectricMagenta, PkRed)))
                    .testTag("send_like_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Like Stream",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    when (message.type) {
        MessageType.SYSTEM -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OverlayBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "📢 ${message.text}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        MessageType.GIFT -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonPurpleDark.copy(alpha = 0.7f),
                                DarkSurface.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = message.senderAvatar, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OverlayBackground)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Text(text = message.senderAvatar, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.senderName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    message.isHost -> GoldAccent
                                    message.isAdmin -> NeonCyan
                                    else -> NeonPurpleLight
                                }
                            )
                            if (message.isHost) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GoldAccent)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = "HOST", fontSize = 8.sp, color = DarkBackground, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

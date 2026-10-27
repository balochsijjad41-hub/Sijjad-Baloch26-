package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun LiveTranscriptView(
    messages: List<ChatMessage>,
    livePartialText: String,
    onSpeakMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(messages.size, livePartialText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "// LIVE TRANSCRIPT LOG",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = accent
            )

            Text(
                text = "${messages.size} ENTRIES",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty() && livePartialText.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mic dabayein ya 'Hey Jarvis' bolein. Roman Urdu ya English mein baat karein...",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    TranscriptItem(
                        message = msg,
                        onReplay = { onSpeakMessage(msg.text) }
                    )
                }

                // Live partial voice input preview
                if (livePartialText.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live speech input",
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$livePartialText...",
                                fontFamily = RajdhaniFontFamily,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptItem(
    message: ChatMessage,
    onReplay: () -> Unit
) {
    val isUser = message.sender == ChatMessage.Sender.USER
    val isSystem = message.sender == ChatMessage.Sender.SYSTEM
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isUser -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    isSystem -> accent.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                }
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (isUser) Icons.Default.Mic else Icons.Default.SmartToy,
                contentDescription = if (isUser) "User" else "JARVIS",
                tint = if (isUser) accent else MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isUser) "COMMAND //" else "JARVIS //",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) accent else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = message.text,
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                if (message.actionSummary != null) {
                    Text(
                        text = "▶ ${message.actionSummary}",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = accent,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        if (!isUser) {
            IconButton(
                onClick = onReplay,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("replay_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speak response out loud",
                    tint = accent.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

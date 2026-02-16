package ai.openclaw.android.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import ai.openclaw.android.chat.ChatMessage
import ai.openclaw.android.chat.ChatMessageContent
import ai.openclaw.android.chat.ChatPendingToolCall
import ai.openclaw.android.tools.ToolDisplayRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role.lowercase() == "user"
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    // Filter to only displayable content parts (text with content, or base64 images)
    val displayableContent = message.content.filter { part ->
        when (part.type) {
            "text" -> !part.text.isNullOrBlank()
            else -> part.base64 != null
        }
    }

    // Skip rendering entirely if no displayable content
    if (displayableContent.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(bubbleBackground(isUser))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    showMenu = true
                                    pressOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                                },
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val textColor = textColorOverBubble(isUser)
                ChatMessageBody(content = displayableContent, textColor = textColor)

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = pressOffset,
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy Text") },
                        onClick = {
                            val fullText =
                                displayableContent
                                    .filter { it.type == "text" }
                                    .joinToString("\n\n") { it.text ?: "" }
                            copyToClipboard(context, fullText)
                            showMenu = false
                        },
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
}

@Composable
private fun ChatMessageBody(content: List<ChatMessageContent>, textColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (part in content) {
            when (part.type) {
                "text" -> {
                    val text = part.text ?: continue
                    ChatMarkdown(text = text, textColor = textColor)
                }
                else -> {
                    val b64 = part.base64 ?: continue
                    ChatBase64Image(base64 = b64, mimeType = part.mimeType)
                }
            }
        }
    }
}

@Composable
fun ChatTypingIndicatorBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DotPulse()
                Text("Thinking…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ChatPendingToolsBubble(toolCalls: List<ChatPendingToolCall>) {
    val context = LocalContext.current
    val displays =
        remember(toolCalls, context) {
            toolCalls.map { ToolDisplayRegistry.resolve(context, it.name, it.args) }
        }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Running tools…", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                for (display in displays.take(6)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${display.emoji} ${display.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                        display.detailLine?.let { detail ->
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                if (toolCalls.size > 6) {
                    Text(
                        "… +${toolCalls.size - 6} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ChatStreamingAssistantBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                ChatMarkdown(text = text, textColor = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun bubbleBackground(isUser: Boolean): Brush {
    return if (isUser) {
        Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun DotPulse(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

@Composable
private fun textColorOverBubble(isUser: Boolean): Color {
    return if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

@Composable
private fun ChatBase64Image(base64: String, mimeType: String?) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(base64) {
        withContext(Dispatchers.IO) {
            try {
                val decodedString = Base64.decode(base64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                bitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Inline Image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}

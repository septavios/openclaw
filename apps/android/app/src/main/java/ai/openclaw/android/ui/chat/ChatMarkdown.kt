package ai.openclaw.android.ui.chat

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

@Composable
fun ChatMarkdown(text: String, textColor: Color) {
    val context = LocalContext.current
    
    // Preprocess text to handle base64 images if needed (ported from previous implementation)
    val processedText = remember(text) { preprocessMarkdown(text) }

    val markwon = remember(context) {
        // Calculate text size for LaTeX
        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f,
            context.resources.displayMetrics
        )
        
        Markwon.builder(context)
            .usePlugin(JLatexMathPlugin.create(textSize))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(CoilImagesPlugin.create(context))
            .build()
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColor.toArgb())
            markwon.setMarkdown(textView, processedText)
        }
    )
}

private fun preprocessMarkdown(text: String): String {
    if (text.isEmpty()) return ""
    // Find raw base64 data URIs and wrap them in Markdown image syntax
    val regex = Regex("data:image/([a-zA-Z0-9+.-]+);base64,([A-Za-z0-9+/=\\n\\r]+)")
    return regex.replace(text) { matchResult ->
        "\n![Image](${matchResult.value})\n"
    }
}

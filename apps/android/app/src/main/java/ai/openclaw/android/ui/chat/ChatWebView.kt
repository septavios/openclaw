package ai.openclaw.android.ui.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChatWebView(text: String, textColor: Color) {
    var webViewHeight = remember { mutableIntStateOf(1) }
    val density = LocalDensity.current

    val isDarkMode =
            (0.299 * textColor.red + 0.587 * textColor.green + 0.114 * textColor.blue) > 0.5

    AndroidView(
            modifier = Modifier.fillMaxWidth().height(webViewHeight.intValue.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    setBackgroundColor(0x00000000)

                    addJavascriptInterface(
                            WebAppInterface { height ->
                                Handler(Looper.getMainLooper()).post {
                                    if (height > 0) {
                                        webViewHeight.intValue = height
                                    }
                                }
                            },
                            "Android"
                    )

                    webViewClient =
                            object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (view != null) {
                                        injectMarkdown(view, text, textColor, isDarkMode)
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest
                                ): Boolean {
                                    val url = request.url.toString()
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        view.context.startActivity(intent)
                                        return true
                                    }
                                    return false
                                }

                                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                                override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        url: String
                                ): Boolean {
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        view.context.startActivity(intent)
                                        return true
                                    }
                                    return false
                                }
                            }

                    loadUrl("file:///android_asset/latex_chat.html")
                }
            },
            update = { webView -> injectMarkdown(webView, text, textColor, isDarkMode) }
    )
}

private fun injectMarkdown(webView: WebView, text: String, textColor: Color, isDarkMode: Boolean) {
    val colorHex = String.format("#%06X", (0xFFFFFF and textColor.toArgb()))
    val escapedText = JSONObject.quote(text)
    val js =
            """
        document.body.style.setProperty('--text-color', '$colorHex');
        if (typeof renderMarkdown === 'function') { 
            renderMarkdown($escapedText, $isDarkMode, '$colorHex'); 
        }
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

class WebAppInterface(val onHeightChanged: (Int) -> Unit) {
    @JavascriptInterface
    fun resize(height: Float) {
        onHeightChanged(height.toInt())
    }
}

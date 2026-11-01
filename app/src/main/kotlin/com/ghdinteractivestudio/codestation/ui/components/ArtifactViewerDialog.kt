package com.ghdinteractivestudio.codestation.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArtifactViewerDialog(
    artifactUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Palette.background)
        ) {
            // Barre supérieure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .background(
                        Palette.glassSurface,
                        RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bouton Fermer
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Palette.text.copy(alpha = 0.08f), CircleShape)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("xmark", size = 16.dp, tint = Palette.text)
                }

                // Titre et badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Document interactif" },
                        style = Type.body.semibold,
                        color = Palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Artifact HTML CodeStation",
                        style = Type.caption2,
                        color = Palette.electricBlue
                    )
                }

                // Bouton Actualiser
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Palette.text.copy(alpha = 0.08f), CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            hasError = false
                            webViewInstance?.reload()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("arrow.clockwise", size = 16.dp, tint = Palette.text)
                }

                // Bouton Ouvrir dans le navigateur externe
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Palette.text.copy(alpha = 0.08f), CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artifactUrl))
                            context.startActivity(Intent.createChooser(intent, "Ouvrir dans le navigateur"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("safari", size = 16.dp, tint = Palette.text)
                }
            }

            // Indicateur de chargement linéaire
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Palette.electricBlue,
                    trackColor = Palette.text.copy(alpha = 0.05f)
                )
            }

            // Corps du document : WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewInstance = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        hasError = true
                                    }
                                }
                            }

                            loadUrl(artifactUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (hasError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Palette.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Symbol("exclamationmark.triangle", size = 40.dp, tint = Palette.warning)
                            Text(
                                text = "Impossible de charger l'artifact",
                                style = Type.title3.semibold,
                                color = Palette.text
                            )
                            Text(
                                text = "Vérifiez que CodeStation Host est bien démarré.",
                                style = Type.caption,
                                color = Palette.secondary
                            )
                            Box(
                                modifier = Modifier
                                    .background(Palette.electricBlue, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        hasError = false
                                        webViewInstance?.reload()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Réessayer",
                                    style = Type.caption.bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

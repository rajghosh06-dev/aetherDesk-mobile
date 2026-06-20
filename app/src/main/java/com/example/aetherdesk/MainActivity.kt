package com.example.aetherdesk

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserRequestCode = 1
    private var myWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial edge-to-edge setup.
        // Using light/dark scrims depending on preferred appearance.
        setStatusBarTheme(isDark = false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                myWebView?.let { webView ->
                    webView.evaluateJavascript("if (typeof window.handleAndroidBack === 'function') window.handleAndroidBack(); else false;") { result ->
                        if (result == "false" || result == "null") {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                } ?: run {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        requestRequiredPermissions()

        setContent {
            WebViewScreen(
                modifier = Modifier.fillMaxSize(),
                onSetup = { webView ->
                    this.myWebView = webView
                    WebView.setWebContentsDebuggingEnabled(true)
                    webView.clearCache(true)

                    with(webView.settings) {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        allowContentAccess = true
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                            val targetPrefix = "https://appassets.androidplatform.net/assets/www/"
                            if (url.startsWith(targetPrefix)) {
                                val assetPath = "www/" + url.substring(targetPrefix.length).substringBefore("?").substringBefore("#")
                                return try {
                                    val mimeType = when {
                                        assetPath.endsWith(".html") -> "text/html"
                                        assetPath.endsWith(".js") -> "application/javascript"
                                        assetPath.endsWith(".css") -> "text/css"
                                        assetPath.endsWith(".png") -> "image/png"
                                        assetPath.endsWith(".svg") -> "image/svg+xml"
                                        else -> "application/octet-stream"
                                    }
                                    WebResourceResponse(mimeType, "UTF-8", assets.open(assetPath))
                                } catch (e: IOException) { null }
                            }
                            return super.shouldInterceptRequest(view, url)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            super.onReceivedError(view, request, error)
                            view?.evaluateJavascript("console.error('WebView Error: ${error?.description}');", null)
                        }

                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            view?.evaluateJavascript("console.error('WebView HTTP Error: ${errorResponse?.statusCode}');", null)
                        }

                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                            super.onReceivedSslError(view, handler, error)
                            handler?.cancel()
                            view?.evaluateJavascript("console.error('WebView SSL Error');", null)
                        }
                    }

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            val grantedPermissions = mutableListOf<String>()
                            for (resource in request.resources) {
                                if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE && checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    grantedPermissions.add(resource)
                                } else if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    grantedPermissions.add(resource)
                                }
                            }
                            if (grantedPermissions.isNotEmpty()) {
                                request.grant(grantedPermissions.toTypedArray())
                            } else {
                                request.deny()
                            }
                        }
                        override fun onShowFileChooser(
                            webView: WebView,
                            callback: ValueCallback<Array<Uri>>,
                            params: FileChooserParams
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback
                            try {
                                startActivityForResult(params.createIntent(), fileChooserRequestCode)
                            } catch (e: ActivityNotFoundException) {
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = null
                                webView.evaluateJavascript("console.error('File chooser failed: ActivityNotFound');", null)
                                return false
                            }
                            return true
                        }
                        
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val logMessage = "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}"
                                if (it.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    android.util.Log.e("WebViewConsole", logMessage)
                                } else {
                                    android.util.Log.d("WebViewConsole", logMessage)
                                }
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webView.addJavascriptInterface(AndroidBridge(this@MainActivity), "AndroidBridge")
                    webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
                }
            )
        }
    }

    fun setStatusBarTheme(isDark: Boolean) {
        val style = if (isDark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            @Suppress("DEPRECATION")
            permissions.addAll(listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }

        val toRequest = permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (toRequest.isNotEmpty()) requestPermissions(toRequest.toTypedArray(), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val denied = permissions.filterIndexed { index, _ -> grantResults[index] != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                val deniedJson = JSONArray(denied).toString()
                myWebView?.evaluateJavascript("if (typeof window.onPermissionsDenied === 'function') window.onPermissionsDenied($deniedJson); else console.error('Permissions denied: $deniedJson');", null)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileChooserRequestCode) {
            filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            filePathCallback = null
        }
    }

    override fun onDestroy() {
        myWebView?.destroy()
        myWebView = null
        super.onDestroy()
    }
}

@Composable
fun WebViewScreen(modifier: Modifier = Modifier, onSetup: (WebView) -> Unit) {
    AndroidView(modifier = modifier, factory = { context -> WebView(context).apply(onSetup) })
}
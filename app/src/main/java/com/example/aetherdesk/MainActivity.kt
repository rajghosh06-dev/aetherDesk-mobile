package com.example.aetherdesk

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    val fileChooserRequestCode = 1
    var myWebView: WebView? = null

    fun setStatusBarTheme(isDark: Boolean) {
        val statusStyle = if (isDark) {
            androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        }
        enableEdgeToEdge(
            statusBarStyle = statusStyle,
            navigationBarStyle = statusStyle
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val webView = myWebView
                if (webView != null) {
                    webView.evaluateJavascript("if (typeof window.handleAndroidBack === 'function') window.handleAndroidBack(); else false;") { result ->
                        if (result == "false" || result == "null") {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // Filter permissions based on running Android version to comply with API 36 rules
        val permissionsList = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            @Suppress("DEPRECATION")
            permissionsList.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            @Suppress("DEPRECATION")
            permissionsList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val permissionsToRequest = permissionsList.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 100)
        }

        setContent {
            WebViewScreen(
                modifier = Modifier.fillMaxSize(),
                onSetup = { webView ->
                    this@MainActivity.myWebView = webView
                    WebView.setWebContentsDebuggingEnabled(true)
                    webView.clearCache(true)

                    // Core Web Configurations
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.databaseEnabled = true
                    webView.settings.mediaPlaybackRequiresUserGesture = false

                    // API 36 Security Hardening: Turn off direct raw file access flags to satisfy Oplus controllers
                    webView.settings.allowFileAccess = false
                    webView.settings.allowContentAccess = false

                    // Intercept local asset loading and tunnel them safely through a virtual secure domain
                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            url: String
                        ): WebResourceResponse? {
                            val targetPrefix = "https://appassets.androidplatform.net/assets/www/"
                            if (url.startsWith(targetPrefix)) {
                                val assetPath = "www/" + url.substring(targetPrefix.length)
                                // Standardize structural parameter lookups by cleaning query bounds
                                val cleanAssetPath = assetPath.substringBefore("?").substringBefore("#")

                                return try {
                                    val mimeType = when {
                                        cleanAssetPath.endsWith(".html") -> "text/html"
                                        cleanAssetPath.endsWith(".js") -> "application/javascript"
                                        cleanAssetPath.endsWith(".css") -> "text/css"
                                        cleanAssetPath.endsWith(".png") -> "image/png"
                                        cleanAssetPath.endsWith(".jpg") || cleanAssetPath.endsWith(".jpeg") -> "image/jpeg"
                                        cleanAssetPath.endsWith(".svg") -> "image/svg+xml"
                                        else -> "text/plain"
                                    }
                                    WebResourceResponse(mimeType, "UTF-8", assets.open(cleanAssetPath))
                                } catch (e: IOException) {
                                    null
                                }
                            }
                            return super.shouldInterceptRequest(view, url)
                        }
                    }

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            request.grant(request.resources)
                        }
                        override fun onShowFileChooser(
                            webView: WebView,
                            filePathCallback: ValueCallback<Array<Uri>>,
                            fileChooserParams: FileChooserParams
                        ): Boolean {
                            this@MainActivity.filePathCallback?.onReceiveValue(null)
                            this@MainActivity.filePathCallback = filePathCallback
                            val intent = fileChooserParams.createIntent()
                            try {
                                startActivityForResult(intent, fileChooserRequestCode)
                            } catch (e: ActivityNotFoundException) {
                                this@MainActivity.filePathCallback = null
                                return false
                            }
                            return true
                        }
                    }

                    webView.addJavascriptInterface(AndroidBridge(this@MainActivity), "AndroidBridge")

                    CoroutineScope(Dispatchers.Main).launch {
                        // Point directly into the newly constructed, policy-compliant domain space
                        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileChooserRequestCode) {
            if (filePathCallback == null) return
            val results = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }
}

@Composable
fun WebViewScreen(modifier: Modifier = Modifier, onSetup: (WebView) -> Unit) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                onSetup(this)
            }
        }
    )
}
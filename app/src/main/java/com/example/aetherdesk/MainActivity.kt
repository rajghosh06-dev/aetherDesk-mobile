package com.example.aetherdesk

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

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

        // Request permissions for HTML5 webcam and audio recording
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        val permissionsToRequest = permissions.filter {
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
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.allowFileAccess = true
                    webView.settings.allowContentAccess = true
                    webView.settings.mediaPlaybackRequiresUserGesture = false
                    
                    webView.webViewClient = WebViewClient()
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
                    webView.loadUrl("file:///android_asset/www/index.html")
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

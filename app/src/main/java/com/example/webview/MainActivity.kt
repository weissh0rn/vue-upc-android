package com.example.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webview.ui.theme.TestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        var name: String? by remember { mutableStateOf(null) }
                        var password: String? by remember { mutableStateOf(null) }

                        LaunchedEffect(key1 = true) {
                            val dataStoreLocal =
                                DataStoreLocal.getInstance(this@MainActivity.applicationContext)
                            val userInfo = dataStoreLocal.getUserInfo()
                            Log.d(
                                "TagToTest",
                                "Name: ${userInfo?.first} Password: ${userInfo?.second}"
                            )
                            userInfo?.let {
                                name = it.first
                                password = it.second
                            }
                        }

                        WebViewScreen(name, password)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(name: String?, password: String?) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError,
                    ) {
                        Log.e("WebViewError", "Error: ${error.description}")
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        Log.e("WebViewError", "HTTP error: ${errorResponse.reasonPhrase}")
                    }
                }
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.setSupportZoom(true)
                addJavascriptInterface(WebAppInterface(context), "AndroidInterface")
            }
        },
        update = { webView ->
            if (name != null && password != null) {
                webView.loadUrl("http://192.168.3.9:5173/login?name=$name&password=$password")
            } else {
                webView.loadUrl("http://192.168.3.9:5173/login")
            }
        }
    )
}

class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun retrieveData(data: String) {
        val list = data.split("%divider%")
        if (list.size == 2) {
            val dataStoreLocal = DataStoreLocal.getInstance(context)
            CoroutineScope(Dispatchers.Default).launch {
                dataStoreLocal.saveUserInfo(list[0], list[1])
                Log.d(
                    "TagToTest",
                    "Name: ${dataStoreLocal.getUserInfo()?.first} Password: ${dataStoreLocal.getUserInfo()?.second}"
                )
            }
        }
        Log.d("WebAppInterface", "Data received: $data")
    }
}

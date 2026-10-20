/**
 * Spo~z Project (C) 2026
 * Licensed under GPL-3.0
 *
 * Hosts the Spo~z UI. The interface is the bundled HTML (assets/spoz), shown
 * full-screen in a WebView, so the app's whole front end is the Spo~z design
 * and none of the old Compose screens are shown.
 */

package com.jay.glossy

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class SpoZWebActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The UI ships as two asset parts (it is larger than one file we can
        // commit at once); join them and load the whole document.
        val html = runCatching {
            val part1 = assets.open("spoz/index_1.html").bufferedReader().use { it.readText() }
            val part2 = assets.open("spoz/index_2.html").bufferedReader().use { it.readText() }
            part1 + part2
        }.getOrDefault("<html><body style='font-family:sans-serif;padding:24px'>Spo~z UI missing.</body></html>")

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                    }
                },
            )
        }
    }
}

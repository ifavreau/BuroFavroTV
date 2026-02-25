package com.burofavro.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.ProgressBar

class TVMainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val DISPLAY_URL = BuildConfig.DISPLAY_URL

        // Custom CSS to inject - same as mobile app
        private const val CUSTOM_CSS = """
            /* BuroFavro TV Custom Styles */

            /* ALL purple/primary elements to black */
            .btn-primary, .o_btn_primary, button.btn-primary, a.btn-primary,
            .btn-primary:hover, .btn-primary:focus, .btn-primary:active,
            .o_btn_primary:hover, .o_btn_primary:focus, .o_btn_primary:active,
            .bg-primary, .badge-primary, .text-bg-primary,
            .o_kanban_primary, .o_form_button_save,
            [style*="background-color: rgb(113, 75, 103)"],
            [style*="background-color: #714b67"],
            [style*="background: #714b67"],
            .btn-odoo, .bg-odoo {
                background-color: #000000 !important;
                border-color: #000000 !important;
                color: #ffffff !important;
            }

            /* Navbar - black background */
            .o_main_navbar {
                background-color: #000000 !important;
            }

            /* Links and text with purple color */
            a.text-primary, .text-primary, .text-odoo,
            a:not(.btn):hover {
                color: #000000 !important;
            }

            /* Odoo brand color override */
            :root {
                --o-brand-odoo: #000000 !important;
                --o-brand-primary: #000000 !important;
                --primary: #000000 !important;
            }

            /* Target the specific purple color */
            [style*="714b67"], [style*="rgb(113"],
            button[style*="background"], .btn[style*="background"] {
                background-color: #000000 !important;
                border-color: #000000 !important;
                color: #ffffff !important;
            }

            /* TV optimizations - larger fonts for readability */
            body {
                font-size: 18px !important;
            }
        """
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)

        // Immersive full screen
        hideSystemUI()

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        webView.loadUrl(DISPLAY_URL)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString BuroFavroTV/1.0"
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        // Enable cookies
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                injectCSS()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    // Show error and retry
                    progressBar.visibility = View.GONE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
        }
    }

    private fun injectCSS() {
        val css = CUSTOM_CSS.replace("\n", "").replace("\"", "\\\"")
        val js = """
            (function() {
                var style = document.createElement('style');
                style.type = 'text/css';
                style.innerHTML = "$css";
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // D-pad navigation
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                // OK button = refresh
                webView.reload()
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                showOptionsDialog()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                showExitConfirmation()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showOptionsDialog() {
        val options = arrayOf("Recargar página", "Versión: ${BuildConfig.VERSION_NAME}")
        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> webView.reload()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Salir")
            .setMessage("¿Deseas cerrar la aplicación?")
            .setPositiveButton("Sí") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}

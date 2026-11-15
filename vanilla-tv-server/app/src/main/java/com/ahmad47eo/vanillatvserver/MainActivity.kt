package com.ahmad47eo.vanillatvserver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var deviceInfo: TextView
    private lateinit var jarInfo: TextView
    private val prefs by lazy { getSharedPreferences("vanilla_server", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(42, 24, 42, 24)
            setBackgroundColor(0xFF101820.toInt())
            isFocusableInTouchMode = true
        }
        fun text(value: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) =
            TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
                setPadding(0, 7, 0, 7)
            }
        root.addView(text("VANILLA TV SERVER", 30f, 0xFF62D68B.toInt()))
        root.addView(text("Minecraft Java 26.3 • Pure vanilla • No plugins", 18f))
        deviceInfo = text("Detecting Android device…", 16f, 0xFFB8C7D9.toInt())
        root.addView(deviceInfo)

        val choose = Button(this).apply {
            text = "IMPORT VANILLA SERVER JAR"
            textSize = 16f
            setOnClickListener { chooseJar() }
        }
        root.addView(choose, params())
        jarInfo = text("No server JAR selected", 16f)
        root.addView(jarInfo)

        root.addView(text("SERVER CONTROLS", 21f, 0xFF62D68B.toInt()))
        root.addView(Button(this).apply {
            text = "START SERVER"
            isEnabled = false
        }, params())
        root.addView(Button(this).apply {
            text = "STOP SERVER"
            isEnabled = false
        }, params())
        root.addView(text(
            "BUILD 0.1 — Device detection and JAR import are implemented. Server execution stays disabled until a compatible Java 25 runtime is integrated and verified for this TV. A 32-bit-only device cannot run the modern 64-bit Java server runtime.",
            15f, 0xFFB8C7D9.toInt()
        ))
        root.addView(text("Designed for Android TV remotes • No ADB or iSH needed", 14f, 0xFF62D68B.toInt()))
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        choose.requestFocus()
        refreshDevice()
        refreshJar()
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = 7; bottomMargin = 7 }

    private fun refreshDevice() {
        val abis = Build.SUPPORTED_ABIS.joinToString()
        val is64 = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        deviceInfo.text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "CPU ABIs: $abis\n64-bit support: ${if (is64) "YES" else "NO — 32-bit only"}\n" +
            "Required server runtime: Java 25"
    }

    private fun chooseJar() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/java-archive"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/java-archive", "application/octet-stream"))
        }
        startActivityForResult(intent, 42)
    }

    private fun refreshJar() {
        jarInfo.text = prefs.getString("jar_name", null)?.let { "Selected: $it" } ?: "No server JAR selected"
    }

    @Deprecated("Use Activity Result API in a later iteration")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            prefs.edit().putString("jar_uri", uri.toString())
                .putString("jar_name", uri.lastPathSegment ?: "Minecraft server JAR").apply()
            refreshJar()
        }
    }
}

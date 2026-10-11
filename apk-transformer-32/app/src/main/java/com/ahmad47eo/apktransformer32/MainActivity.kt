package com.ahmad47eo.apktransformer32

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.zip.ZipInputStream

class MainActivity : Activity() {
    private lateinit var report: TextView
    private var lastAbis: Set<String> = emptySet()
    private var lastNativeLibCount = 0
    private var lastDexCount = 0
    private var lastManifestFound = false
    private var analyzed = false

    private val bg = Color.rgb(11, 16, 32)
    private val panel = Color.rgb(22, 31, 54)
    private val accent = Color.rgb(91, 192, 255)
    private val white = Color.rgb(245, 247, 255)
    private val muted = Color.rgb(180, 193, 215)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(bg)
        }
        content.addView(TextView(this).apply {
            text = "APK TRANSFORMER 32"
            textSize = 27f
            setTextColor(accent)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(8))
        })
        content.addView(TextView(this).apply {
            text = "Analyze APKs • Check 32-bit support • Explore real porting requirements"
            textSize = 15f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        })
        val choose = Button(this).apply {
            text = "1. Choose APK to analyze"
            textSize = 17f
            isAllCaps = false
            setOnClickListener { chooseApk() }
        }
        content.addView(choose, matchWrap())
        report = TextView(this).apply {
            text = "READY\n\nSelect an APK to inspect its native libraries and CPU architectures."
            textSize = 16f
            setTextColor(white)
            setBackgroundColor(panel)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setTextIsSelectable(true)
        }
        content.addView(report, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        content.addView(Button(this).apply {
            text = "2. Open 32-bit Conversion Lab"
            isAllCaps = false
            setOnClickListener { showConversionLab() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        content.addView(Button(this).apply {
            text = "About this tool"
            isAllCaps = false
            setOnClickListener {
                report.text = "APK TRANSFORMER 32 • Version 0.2.0\n\nThis tool inspects APK ZIP entries locally. It does not upload your APK or translate compiled ARM64 machine code into ARM32 code. It only reports checks we can verify from the package."
            }
        }, matchWrap())
        content.addView(TextView(this).apply {
            text = "HONEST LIMITATION\nA genuine 64-bit-to-32-bit port normally requires source code and a rebuild with 32-bit-compatible dependencies. Repacking or changing an APK label cannot convert native machine code."
            textSize = 14f
            setTextColor(muted)
            setPadding(dp(4), dp(16), dp(4), dp(8))
        })
        scroll.addView(content)
        setContentView(scroll)
        choose.requestFocus()
    }

    private fun chooseApk() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.android.package-archive"
        }
        try {
            startActivityForResult(intent, REQUEST_APK)
        } catch (_: Exception) {
            report.text = "Could not open the file picker. Try a file manager on the TV."
        }
    }

    @Deprecated("Platform result API retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_APK || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        report.text = "Analyzing APK…"
        Thread {
            val result = analyze(uri)
            runOnUiThread { report.text = result }
        }.start()
    }

    private fun analyze(uri: Uri): String {
        val abis = linkedSetOf<String>()
        var nativeLibCount = 0
        var dexCount = 0
        var manifestFound = false
        return try {
            val input = contentResolver.openInputStream(uri)
                ?: return "ERROR\n\nCould not read this file. Check file access and try again."
            input.use { stream ->
                ZipInputStream(stream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name == "AndroidManifest.xml") manifestFound = true
                        if (name.endsWith(".dex")) dexCount++
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val parts = name.split("/")
                            if (parts.size >= 3) {
                                abis.add(parts[1])
                                nativeLibCount++
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            lastAbis = abis.toSet()
            lastNativeLibCount = nativeLibCount
            lastDexCount = dexCount
            lastManifestFound = manifestFound
            analyzed = true

            val has32 = "armeabi-v7a" in abis
            val has64 = "arm64-v8a" in abis
            val lines = mutableListOf<String>()
            lines += "ANALYSIS COMPLETE"
            lines += ""
            lines += "Native libraries: $nativeLibCount"
            lines += "DEX files: $dexCount"
            lines += "Android manifest entry: " + if (manifestFound) "found" else "not found"
            lines += ""
            lines += "ABIs found:"
            lines += if (abis.isEmpty()) "No native .so libraries detected (could be Java/Kotlin-only)." else abis.sorted().joinToString("\n") { "• $it" }
            lines += ""
            lines += "32-bit ARM: " + if (has32) "PRESENT" else "NOT DETECTED"
            lines += "64-bit ARM: " + if (has64) "PRESENT" else "NOT DETECTED"
            lines += ""
            lines += when {
                has32 -> "RESULT: Includes 32-bit ARM libraries. This is a positive sign, but Android version, RAM, graphics and app requirements still matter."
                has64 -> "RESULT: 64-bit ARM libraries detected but 32-bit ARM libraries were not. This APK cannot be made 32-bit by repacking. A real port needs compatible 32-bit libraries and a rebuild."
                abis.isEmpty() -> "RESULT: No native libraries detected. That does not prove compatibility; Android APIs and hardware requirements may still block the app."
                else -> "RESULT: No armeabi-v7a libraries detected. The available ABIs may not match a 32-bit ARM device."
            }
            lines += ""
            lines += "This ZIP-level check is not a full signature, manifest, or installability validation."
            lines.joinToString("\n")
        } catch (e: Exception) {
            "ANALYSIS FAILED\n\n" + e.javaClass.simpleName + ": " + (e.message ?: "Could not read APK")
        }
    }

    private fun showConversionLab() {
        report.text = if (!analyzed) {
            "CONVERSION LAB\n\nFirst choose and analyze an APK. The lab uses the detected ABI list to explain the realistic next step."
        } else {
            val has32 = "armeabi-v7a" in lastAbis
            val has64 = "arm64-v8a" in lastAbis
            when {
                has32 -> "CONVERSION LAB • 32-BIT LIBRARIES FOUND\n\nThis APK already contains armeabi-v7a native libraries. No CPU-architecture conversion may be needed. Installation can still fail because of Android version, device features, RAM, graphics, signatures, or app restrictions.\n\nNext: check the package's Android requirements and test-install only if you trust its source."
                has64 -> "CONVERSION LAB • PORT REQUIRED\n\nDetected arm64-v8a native libraries, with no armeabi-v7a libraries detected.\n\nWhat a real port needs:\n1. App source code or permission to rebuild the app.\n2. The required native dependencies built for armeabi-v7a.\n3. A 32-bit-compatible SDK/NDK and build configuration.\n4. Rebuild, test, and sign the resulting APK.\n\nWhat this APK alone cannot do: recover source code or automatically translate ARM64 native binaries into ARM32. Repacking alone will not make it run on a 32-bit-only TV."
                else -> "CONVERSION LAB • MORE INFORMATION NEEDED\n\nNo armeabi-v7a native libraries were detected. This may be a Java/Kotlin-only app or a package with another layout. A full manifest and dependency inspection is needed before deciding whether a rebuild is possible."
            }
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object { private const val REQUEST_APK = 320 }
}

package com.ahmad47eo.vanillatvserver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var deviceInfo: TextView
    private lateinit var jarInfo: TextView
    private lateinit var statusInfo: TextView
    private lateinit var logView: TextView
    private lateinit var eulaToggle: CheckBox
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var serverProcess: Process? = null
    private val io = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("vanilla_server", MODE_PRIVATE) }
    private val serverDir by lazy { File(filesDir, "server").apply { mkdirs() } }
    private val jarFile by lazy { File(serverDir, "server.jar") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(36, 22, 36, 22)
            setBackgroundColor(0xFF101820.toInt())
            isFocusableInTouchMode = true
        }
        fun label(value: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) =
            TextView(this).apply { text = value; textSize = size; setTextColor(color); setPadding(0, 6, 0, 6) }
        root.addView(label("VANILLA TV SERVER", 30f, 0xFF62D68B.toInt()))
        root.addView(label("Minecraft Java 26.3 • Official vanilla server • No plugins", 17f))
        deviceInfo = label("Checking device…", 15f, 0xFFB8C7D9.toInt()); root.addView(deviceInfo)

        val import = Button(this).apply { text = "IMPORT VANILLA SERVER JAR"; setOnClickListener { chooseJar() } }
        root.addView(import, params())
        jarInfo = label("No server JAR imported", 15f); root.addView(jarInfo)

        root.addView(label("SERVER AGREEMENT", 20f, 0xFF62D68B.toInt()))
        eulaToggle = CheckBox(this).apply {
            text = "I agree to the Minecraft EULA"
            setTextColor(-1); textSize = 16f
            isChecked = File(serverDir, "eula.txt").takeIf { it.exists() }?.readText()?.contains("eula=true") == true
            setOnCheckedChangeListener { _, checked ->
                File(serverDir, "eula.txt").writeText("eula=$checked\n")
                refreshStatus()
            }
        }
        root.addView(eulaToggle, params())

        root.addView(label("AUTHENTICATION", 20f, 0xFF62D68B.toInt()))
        val offline = CheckBox(this).apply {
            text = "Offline Mode (online-mode=false)"
            setTextColor(-1); textSize = 16f
            isChecked = prefs.getBoolean("offline_mode", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("offline_mode", checked).apply()
                writeProperties()
                appendLog(if (checked) "WARNING: Offline Mode enabled. Player identity is not authenticated." else "Online Mode enabled.")
            }
        }
        root.addView(offline, params())
        root.addView(label("Offline Mode permits arbitrary usernames and impersonation. Keep this server on a trusted private LAN; never expose it directly to the internet.", 14f, 0xFFFFC66D.toInt()))

        root.addView(label("SERVER CONTROLS", 20f, 0xFF62D68B.toInt()))
        statusInfo = label("Status: STOPPED", 16f); root.addView(statusInfo)
        startButton = Button(this).apply { text = "START SERVER"; setOnClickListener { startServer() } }
        stopButton = Button(this).apply { text = "STOP SERVER"; isEnabled = false; setOnClickListener { stopServer() } }
        root.addView(startButton, params()); root.addView(stopButton, params())
        root.addView(label("LIVE CONSOLE", 20f, 0xFF62D68B.toInt()))
        logView = label("Logs will appear here.", 13f, 0xFFB8C7D9.toInt()); root.addView(logView)
        root.addView(label("Android normally uses ART, not a desktop Java VM. This app checks for a Java 25 executable named java on PATH. It does not bundle an unverified runtime. A 32-bit-only TV cannot run a 64-bit-only Java runtime.", 14f, 0xFFFFC66D.toInt()))

        val scroll = ScrollView(this); scroll.addView(root); setContentView(scroll)
        import.requestFocus()
        refreshDevice(); refreshJar(); refreshStatus(); writeProperties()
    }

    private fun params() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 5; bottomMargin = 5 }

    private fun refreshDevice() {
        val abis = Build.SUPPORTED_ABIS.joinToString()
        val supports64 = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        deviceInfo.text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nABIs: $abis\n64-bit capable: ${if (supports64) "YES" else "NO"}\nMinecraft 26.3 requires Java 25"
    }

    private fun chooseJar() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/java-archive", "application/octet-stream"))
        }
        startActivityForResult(i, 42)
    }

    @Deprecated("Legacy document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 42 || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        io.execute {
            try {
                contentResolver.openInputStream(uri)?.use { input -> jarFile.outputStream().use { output -> input.copyTo(output) } }
                runOnUiThread { refreshJar(); appendLog("Imported server JAR into app storage.") }
            } catch (e: Exception) { runOnUiThread { appendLog("JAR import failed: ${e.message}") } }
        }
    }

    private fun refreshJar() { jarInfo.text = if (jarFile.exists()) "Imported server.jar (${jarFile.length()} bytes)" else "No server JAR imported" }

    private fun writeProperties() {
        val file = File(serverDir, "server.properties")
        val old = if (file.exists()) file.readLines().filterNot { it.startsWith("online-mode=") || it.startsWith("server-port=") || it.startsWith("motd=") } else emptyList()
        val online = !prefs.getBoolean("offline_mode", false)
        file.writeText((old + "online-mode=$online" + "server-port=25565" + "motd=Vanilla TV Server").joinToString("\n", postfix="\n"))
    }

    private fun refreshStatus() {
        val running = serverProcess?.isAlive == true
        statusInfo.text = "Status: ${if (running) "RUNNING" else "STOPPED"} • Port 25565"
        startButton.isEnabled = !running
        stopButton.isEnabled = running
    }

    private fun appendLog(line: String) { runOnUiThread { logView.text = (logView.text.toString() + "\n" + line).takeLast(12000) } }

    private fun startServer() {
        if (serverProcess?.isAlive == true) return
        if (!jarFile.exists()) { appendLog("Import the official vanilla server JAR first."); return }
        if (!eulaToggle.isChecked) { appendLog("Accept the Minecraft EULA before starting."); return }
        writeProperties()
        io.execute {
            try {
                val check = ProcessBuilder("java", "-version").redirectErrorStream(true).start()
                val versionText = check.inputStream.bufferedReader().readText()
                val exit = check.waitFor()
                if (exit != 0 || !versionText.contains("25")) {
                    appendLog("Java 25 not found. Runtime check output: $versionText")
                    return@execute
                }
                val process = ProcessBuilder("java", "-Xms512M", "-Xmx1024M", "-jar", jarFile.absolutePath, "nogui")
                    .directory(serverDir).redirectErrorStream(true).start()
                serverProcess = process
                runOnUiThread { refreshStatus() }
                appendLog("Minecraft server process started.")
                process.inputStream.bufferedReader().useLines { lines -> lines.forEach { appendLog(it) } }
                appendLog("Server exited with code ${process.waitFor()}")
            } catch (e: Exception) {
                appendLog("Unable to launch Java server: ${e.message}. Android usually does not include a server JVM.")
            } finally { serverProcess = null; runOnUiThread { refreshStatus() } }
        }
    }

    private fun stopServer() {
        val process = serverProcess ?: return
        if (process.isAlive) {
            try { process.outputStream.write("stop\n".toByteArray()); process.outputStream.flush() }
            catch (_: Exception) { process.destroy() }
        }
        appendLog("Stop requested.")
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}

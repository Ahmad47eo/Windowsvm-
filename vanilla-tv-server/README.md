# Vanilla TV Server

Android TV manager for a **pure vanilla Minecraft Java 26.3** server. No Paper, Spigot, Bukkit, or plugins.

## Features
- Android TV launcher and remote-friendly interface
- CPU ABI and 64-bit capability detection
- Import the vanilla server JAR into app-private storage
- EULA acceptance control writes eula.txt
- Offline Mode toggle writes online-mode=false to server.properties (Online Mode is default)
- Default server port 25565 and MOTD
- Start/Stop controls and live process output if a compatible runtime is available
- Checks for Java 25 before attempting to launch

## Important runtime limitation
Android normally runs apps on ART, not a desktop/server Java VM. This app checks for a Java 25 executable named java on PATH; it does **not** bundle a server-compatible JVM. Most Android TV devices will not have one, so Start may report that Java cannot be found. Minecraft 26.3 needs a supported 64-bit Java runtime; a 32-bit-only ARM TV cannot run a 64-bit-only runtime. ABI detection does not prove the TV can host the server.

The current process manager is experimental and runs from the app process. Android may stop it in the background. It is not an always-on production hosting service. A compatible runtime and foreground service still need testing on the actual TV.

## Offline Mode warning
Offline Mode writes online-mode=false. It disables Microsoft/Minecraft account authentication, allowing arbitrary usernames and impersonation. Keep it on a trusted private LAN or VPN. Never expose an offline-mode server directly to the public internet.

## Build without Android Studio
Open **Actions → Vanilla TV Server Android Build**, run the workflow, then download the vanilla-tv-server-debug-apk artifact from a completed run. A push to this project folder automatically triggers the workflow.

Minimum Android version: 6.0 (API 23).

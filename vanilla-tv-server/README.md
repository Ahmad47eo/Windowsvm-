# Vanilla TV Server

Android TV manager starter for a **pure vanilla Minecraft Java 26.3** server.

## Current build
- Android TV / Leanback launcher entry
- Large remote-friendly interface
- On-device CPU ABI and Android version detection
- Import and remember a vanilla server JAR
- Offline Mode toggle that writes `online-mode=true|false` into the app-private server.properties template at `files/server/server.properties`
- Explicit impersonation warning when Offline Mode is enabled

## Offline Mode security
When Offline Mode is enabled, the generated server.properties template contains:

```properties
online-mode=false
```

This disables Mojang/Microsoft account authentication. Players can connect with arbitrary names, including names belonging to other players. Depending on the server version and setup, duplicate-name attempts may also cause conflicts. Anyone who can reach the server may impersonate another player. Keep an offline-mode server restricted to a trusted LAN or private VPN. Do not expose its port directly to the public internet.

The toggle currently saves the setting to an app-private server.properties template. The server process is **not implemented yet**, so this setting is not currently applied to a running Minecraft server.

## Build
GitHub Actions builds the debug APK. Open **Actions → Vanilla TV Server Android Build**, then download the `vanilla-tv-server-debug-apk` artifact from a successful run.

## Important compatibility limitation
Minecraft Java 26.3 requires a compatible Java 25 runtime. Modern Java server builds require a supported 64-bit runtime; a 32-bit-only ARM TV is not expected to run this server. The app detects the TV architecture but does not yet bundle or launch Java. Start/Stop controls remain disabled until a compatible runtime and process supervisor are implemented and tested.

This project does not use Paper, Spigot, Bukkit, or plugins.

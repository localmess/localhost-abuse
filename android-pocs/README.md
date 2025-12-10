# 📱 Android Localhost PoC Apps

Proof-of-concept Android apps that expose localhost endpoints or listeners to receive IDs from companion web payloads (HTTP, WebSocket, UDP/STUN/TURN, and mDNS) when installed on a device.

---

## ⚙️ Requirements

- Android Studio (Koala or newer) with Android SDK/Build-Tools 35
- Java 11+ (bundled JDK from Android Studio is fine)
- ADB with an emulator or device (API level must meet each app's minSdk; see notes below) and USB debugging enabled

---

## 📁 Folder Structure

- `StealthApp/` — NanoHTTPD HTTP server on 13380–13387 that logs headers, query params, and bodies.
- `StealthAppWS/` — WebSocket server on 12380–12387, replies with `200`, and logs text/binary frames.
- `StealthUDP/` — UDP listener on 12580 capturing incoming datagrams.
- `StealthTURN/` — Minimal STUN/TURN responder on UDP 12586 (issues nonces and allocation replies).
- `webrtc_mdns/` — Foreground service sniffing mDNS on 224.0.0.251:5353 to surface WebRTC host candidates; requests Nearby devices permission on Android 14+.
- `XProfilePOC/` — Cross-profile PoC. Opens HTTP servers on 55556/55557 that store Origin/browser metadata in a local history view to show the localhost abuse feasibility across Android profiles.
- `YandexPoC/` — HTTP servers on 29009/29010/30102/30103 logging incoming requests plus origin/browser info.

---

## 🛠️ Build & Install

### Android Studio
1. `File → Open` the desired subfolder (e.g., `StealthApp/`).
2. Let Gradle sync, then click **Run** to deploy to the attached emulator/device.

### CLI (no Studio)
```bash
cd android-pocs/StealthApp
./gradlew assembleDebug     # build APK
./gradlew installDebug      # optional: push to the default ADB device
# APKs land in app/build/outputs/apk/debug/
```
Repeat in each app folder (`StealthAppWS`, `StealthTURN`, `StealthUDP`, `webrtc_mdns`, `XProfilePOC`, `YandexPoC`).

---

## 🚀 Using the PoCs

- Launch the installed app; background services start automatically and the UI shows the bound IP/ports (where applicable).
- Drive traffic from the `poc-web-pages` toward the exposed localhost ports listed above.
- For `webrtc_mdns`, tap **Start** to begin sniffing mDNS, **Stop** to release the multicast lock, and **Clear** to reset the log.
- `XProfilePOC` and `YandexPoC` include a history screen to review stored origins/browsers.

---

## 📂 Output

- HTTP/WebSocket apps write to `server_logs.txt` in app-internal storage and mirror entries in the on-screen log.
- UDP/STUN/TURN apps write to `udp_log.txt` and stream entries to the UI.
- History data for `XProfilePOC`/`YandexPoC` is kept in a local SQLite DB (`logs.db`).

---

## 📌 Notes

- Apps target SDK 35; minSdk varies by project (28 for `YandexPoC`, 31 for `webrtc_mdns`, 33–34 for the others). Use an emulator/device that satisfies the app you are running.
- Ports are hard-coded; ensure they are free before launching.
- Each app runs as a foreground service; you may need to accept the persistent notification.
- `webrtc_mdns` needs the Nearby devices/Wi-Fi permission to capture mDNS traffic on Android 14+.

# 🔓 Android TLS/SSL Unpinning with Frida + mitmweb

This tool allows you to inject Frida-based SSL unpinning hooks to bypasses SSL/TLS
certificate validation in Android apps (meta and otherwise) and transparently
intercept their HTTPS traffic using `mitmproxy`/`mitmweb`.

---

## ⚙️ Requirements

- [Frida](https://frida.re) installed on your machine
- `mitmweb` from [mitmproxy.org](https://mitmproxy.org/)
- Python ≥ 3.7
- Rooted Android device or emulator
- USB debugging enabled


---
## 📁 Folder Structure

```
.
├── inject.py                 # Main Frida injector and mitmproxy runner
├── setup_frida_server.py     # Script to download and start frida-server on Android
├── ssl-unpinning.js          # Universal Frida SSL unpinning script
├── instagram_ssl_bypass.js   # Instagram-specific Frida hooks
├── facebook_ssl_bypass.js    # Facebook-specific Frida hooks
└── mitmdumps/                # Folder where .dump traffic files are saved
```

---

## 🛠️ Setup Instructions

### 1. Install Frida and mitmproxy

```bash
pip install frida-tools mitmproxy
```

Optional: Use a virtualenv to isolate dependencies.


### 2. Start the Frida server on your device

```bash
python3 setup_frida_server.py
```

This script will:

- Detect your device architecture
- Download the correct frida-server binary
- Push it to your device and run it via su

✅ Requires your device to be rooted.


### 🚀 3. Inject frida unpinning hooks

```bash
python3 inject.py com.example.app
```

```
Optional flags:
--port 8081 — Use a custom mitmproxy port
--su — Use su for pushing the certificate (for Magisk-rooted devices)
--mitm_cli — Use mitmdump (headless) instead of mitmweb
--upstream http://127.0.0.1:8888 — Chain to another proxy
```
---

## 🔎 Example
```bash
python3 inject.py com.instagram.android --su --mitm_cli --upstream http://127.0.0.1:8081
```
---

## 📂 Output

* Traffic flows are saved in mitmdumps/com_example_app_vX.Y.Z.dump
* STDOUT/STDERR logs are stored in logs.txt
---

## 📌 Notes

* ✅ This script installs a system-wide mitmproxy certificate on rooted Android devices.
Works on Android 12 and 13, tested on Pixel 3a and Pixel 6a.
Certificate is placed in /system/etc/security/cacerts/ as <hash>.0 using openssl.
* ✅ It automatically sets the device’s Wi-Fi proxy to route all traffic through mitmproxy. If an active network is found, it sets a global proxy for the device.
* ✅ Spins up mitmdump (headless) or mitmweb (GUI) — configurable via --mitm_cli.
* ✅ Intercepted traffic is logged to .dump files under ./mitmdumps/, named by package and version.
* ✅ A full session log is saved to logs.txt for later debugging or replay.

---

## 🙏 Credits

- `facebook_ssl_bypass.js` is adapted from [Skuxblan/Facebook-SSL-Pinning-Bypass](https://github.com/Skuxblan/Facebook-SSL-Pinning-Bypass).
- `instagram_ssl_bypass.js` is adapted from [expectedfailure/Instagram-SSL-Pinning-Bypass-Research](https://github.com/expectedfailure/Instagram-SSL-Pinning-Bypass-Research).

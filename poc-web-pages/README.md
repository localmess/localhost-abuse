# 🌐 Localhost Communication Web PoCs

Web pages and servers to reproduce Meta and Yandex localhost channel methods.

---

## ⚙️ Requirements

- Use the Chrome browser; Brave and Firefox may block certain methods.
- Chrome version 141 and lower does not implement LNA and should work without issues.
- Chrome version 142 implements LNA. Thus, when prompted, allow local network access (Chrome LNA mitigations). Alternatively, disable the `Local Network Access Checks` flag under `chrome://flags`.
- Python packages:
  - flask
  - flask_cors
  - flask_sock
  - aiortc

---

## 📁 Folder Structure

```
.
├── additional-vectors/             # Extra PoC payloads and variants
├── http_https_websocket.py         # HTTP/HTTPS/WebSocket test server
├── localhost_channel_methods.html  # Page demonstrating the localhost channel methods
├── webRTC_turn.py                  # WebRTC TURN test server
├── webrtc.py                       # WebRTC STUN test server
└── README.md
```

---

## 📌 Meta/Yandex Localhost Abuse Methods & Server Mapping

This folder contains `.html` and `.py` files to recreate the techniques used by Meta and Yandex.

- [`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) recreates all five methods. Hosted version: https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html
- The `*.py` server files recreate the app listening behaviour. Find below the Server → method mapping:
  - **HTTP** → `http_https_websocket.py` (`/send`, port 5000). Sends requests to localhost over HTTP. Used by Yandex since February 2017 until June 2025 and by Meta since September 2024 until October 2024.
  - **HTTPS** → `http_https_websocket.py` (`/send`, port 5001 over HTTPS). Sends requests over HTTPS to a domain resolving to 127.0.0.1. Used by Yandex since May 2018 until June 2025.
  - **WebSocket** → `http_https_websocket.py` (`/ws`, port 5000). Sends data via a WebSocket connection with a server on localhost. Used by Meta from November 2024 to January 2025.
  - **WebRTC STUN** → `webrtc.py` (UDP port 10000 for IPv4/IPv6). Sends WebRTC STUN binding requests. Sends by Meta from November 2024 to June 3rd.
  - **WebRTC TURN** → `webRTC_turn.py` (UDP port 10001). Sends WebRTC TURN requests without SDP-munging. Implemented by Meta from May 2025 to June 3rd.

---

## 🛠️ How to Run the PoCs

- `localhost_channel_methods.html` displays a random four-digit number at the top of the page; each method sends this number and the method name.
- Use the hosted page or a local copy together with the matching `*.py` server: `http_https_websocket.py` (HTTP/HTTPS/WebSocket), `webrtc.py` (STUN), `webRTC_turn.py` (TURN).

### HTTP and WebSocket
1. Run `python http_https_websocket.py` (default HTTP port: 5000).
2. Open [`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open the _Network_ tab in Chrome DevTools.
4. Under **HTTP**, click _Send via HTTP_ and verify the request in the _Network_ tab.
5. Confirm in the terminal that the server received the random number.
6. Under **WebSocket**, click _Send via WebSocket_ and verify the `ws` request in the _Network_ tab.
7. Confirm in the terminal that the server received the random number.

### HTTPS
To set up HTTPS, configure a loopback domain and certificate (see **🔐 HTTPS Test Domain Setup** below).
1. After generating and accepting `cert.pem` and `key.pem`, run `python http_https_websocket.py` (default HTTPS port: 5001).
2. Open [`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open the _Network_ tab in Chrome DevTools.
4. Under **HTTPS**, click _Send via HTTPS_ and verify the request in the _Network_ tab.
5. Confirm in the terminal that the server received the random number.

### WebRTC STUN
WebRTC STUN binding requests are not visible in DevTools _Network_; use packet captures (e.g., Wireshark) or `chrome://webrtc-internals/` (entries vanish quickly).
1. Run `python webrtc.py` (default port: 10000).
2. Open [`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open `chrome://webrtc-internals/` in another tab or start packet capture software.
4. Under **WebRTC STUN**, click _Send via STUN_ and verify the request in `chrome://webrtc-internals/` or the capture tool.
5. Confirm in the terminal that the server received the random number.

The _Send with STUN_ button uses the Meta method with the original SDP-munging technique; _Send with Adapted STUN_ uses an alternative SDP-munging variant if the first is blocked.

### WebRTC TURN
WebRTC TURN requests are not visible in DevTools _Network_; use packet captures or `chrome://webrtc-internals/` (entries vanish quickly).
1. Run `python webrtc_turn.py` (default port: 10001).
2. Open [`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open `chrome://webrtc-internals/` in another tab or start packet capture software.
4. Under **WebRTC TURN**, click _Send via TURN_ and verify the request in `chrome://webrtc-internals/` or the capture tool.
5. Confirm in the terminal that the server received the random number.
6. The `webrtc_turn.py` server shuts down after receiving a request and must be restarted for subsequent tests.

---

## 🔐 HTTPS Test Domain Setup

The HTTPS method requires a domain resolving to 127.0.0.1 and matching certificates.

1. Map a loopback domain: edit the `hosts` file (e.g., `C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on Linux/macOS) to bind a chosen domain (e.g., `myapp.local`) to 127.0.0.1.
<p align="center">
<img width="451" height="296" alt="hosts file with added domain" src="https://github.com/user-attachments/assets/8144e54c-5eb4-4728-a007-98309a1205cd" />
</p>
2. Generate the certificate from the folder containing `http_https_websocket.py` (replace `myapp.local` with your domain):

    ```sh
    openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem -days 365 -subj "/CN=myapp.local" -addext "subjectAltName=DNS:myapp.local"
    ```
<p align="center">
<img width="237" height="143" alt="certificate made and added to the folder" src="https://github.com/user-attachments/assets/9d8c6890-be55-4f9e-bfe1-d6dc187a8663" />
</p>
3. Start the HTTPS server: run [`python http_https_websocket.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/http_https_websocket.py) (default HTTPS port: 5001).
4. Visit the domain and port in a browser (e.g., `https://myapp.local:5001`).
5. Click on _Advanced_ and then on _Proceed to myapp.local (unsafe)_ to accept the certificate.
<p align="center">
<img width="600" height="464" alt="Visiting the binded domain with warning" src="https://github.com/user-attachments/assets/b8bf64bd-3664-42d2-89bb-767f92ad9125" />
</p>

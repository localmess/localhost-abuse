# 🛰️ Additional Localhost Attack Vectors

Two extra localhost abuse vectors that demonstrate Chrome LNA bypass techniques (Section 7 of the paper).

---

## ⚙️ Requirements

- Use the Chrome browser (version 142); Brave and Firefox may block certain methods.
- Enable `Local Network Access Checks` and `Local Network Access Checks for WebRTC` under `chrome://flags` to test against Chrome LNA.

---

## 📁 Folder Structure

```
.
├── additional_vectors.html   # Demo page for two additional vectors
├── mdns.py                   # mDNS listener for Section 7.2
└── README.md
```

---

## 📌 Overview

This folder contains the two additional attack vectors from Section 7 of the paper. The `*.py` server file recreates the app listening behaviour.

- [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) hosts both vectors:
  - **WebRTC & IPv6 Global Unicast Addresses** (Section 7.1): WebRTC STUN binding requests to IPv6 Global Unicast Addresses to bypass Chrome LNA.
  - **mDNS lookup** (Section 7.2): mDNS lookup requests to bypass Chrome LNA.

---

## 🛠️ How to Run the PoCs

- `additional_vectors.html` shows a random four-digit number; each method sends this number plus the method name.
- Use the hosted page or a local copy together with the matching server: `webrtc.py` (IPv6 STUN) and `mdns.py` (mDNS).


### WebRTC & IPv6 Global Unicast Addresses (via `webrtc.py`)

WebRTC STUN binding requests are not visible in the DevTools _Network_ tab. Use packet capture tools (e.g., Wireshark) or `chrome://webrtc-internals/` (entries disappear quickly). The [`webrtc.py`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/webrtc.py) file lives in the parent `poc-web-pages` folder.

1. Run `python webrtc.py` (default UDP port: 10000 for IPv4/IPv6).
2. Open [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/b668fa8a4cfdeec75d55911c42ebbc40/raw/e812b2126469aaaa3cdc3b17006757021a9a144a/additional_vectors.html).
3. Open `chrome://webrtc-internals/` in another tab or start packet capture software.
4. Under **WebRTC & IPv6 Global Unicast Addresses**, click _Send via STUN_ and verify the request in `chrome://webrtc-internals/` or your packet capture tool.
5. Confirm in the terminal that `webrtc.py` received the random number.

> **Note:** The _Index of Address_ field and _Candidate addresses_ allow selecting the candidate to use. By default, Chrome places IPv6 addresses last, and addresses may appear as `UUID.local`.

### mDNS lookup (via `mdns.py`)

1. Run `python mdns.py`.
2. Open [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) locally or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/b668fa8a4cfdeec75d55911c42ebbc40/raw/e812b2126469aaaa3cdc3b17006757021a9a144a/additional_vectors.html).
3. Under **mDNS lookup**, click _Send mDNS lookup request_ and verify the request in your packet capture tool goes to `mdns.py`.
4. Confirm in the terminal that `mdns.py` received the random number.

> **Note:** mDNS lookup requests are not visible in the DevTools _Network_ tab; use packet capture tools to observe them.

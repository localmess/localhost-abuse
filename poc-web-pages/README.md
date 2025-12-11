# Reproduction of results
When using the web-pages in this folder, it is best to use the Chrome browser, as both the Brave and Firefox browser may block certain methods.
When prompted, accept the prompt to access the local network. This prompt appears as part of Chrome's LNA mitigations for localhost communication like this. Alternativly, the `Local Network Access Checks` flag can be disabled on `chrome://flags`

## Methods used by Meta and Yandex
This folder contains various .html and .py files allowing for the reproduction of the methods used by Meta and Yandex.
[`localhost_channel_methods.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html) contains recreations of the five methods these companies used. It is also hosted on the following link:
https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html

The `*.py` server files recreate the app listening behaviour.

 - **HTTP**: method using requests send to the localhost over HTTP. Used by Yandex since February 2017 until June 2025 and by Meta since September 2024 until October 2024.
 - **HTTPS**: method using requests sent over HTTPS to a domain resolving to 127.0.0.1. Used by Yandex since May 2018 until June 2025.
 - **WebSocket**: method establishing a WebSocket connection with a server at the localhost. Used by Meta from November 2024 to January 2025.
 - **WebRTC STUN**: method using WebRTC STUN binding requests. Used by Meta from November 2024 to June 3rd.
 - **WebRTC TURN**: method using WebRTC TURN requests that does not rely on SDP-munging. Implemented by Meta from May 2025 to June 3rd.

## How to use
`localhost_channel_methods.html` displays a random four digit number at the top of the page. Each method uses this number as well as the name of the method as data to send.

### HTTP and WebSocket
1. Run `python http_https_websocket.py` in the command line (Default port for HTTP is 5000)
2. Open the https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open the _Network_ tab in the Chrome Devtools panel.
4. On the page, under **HTTP** click the _Send via HTTP_ button and verify a request has been sent in the _Network_ tab.
5. Verify in the command line that the server received the random number.
4. On the page, under **WebSocket** click the _Send via WebSocket_ button and verify a WebSocket (ws) was created sent in the _Network_ tab.
5. Verify in the command line that the server received the random number.

### HTTPS
To set up the test for HTTPS, the server needs a domain that resolves to the loopback address and its certificates. The steps to set up this domain and generate its certificate are explained at the end of this README and need to be run only once.
1. After generating and accepting the `cert.pem` and `key.pem` files, run `python http_https_websocket.py` in the command line (Default port for HTTPS is 5001).
2. Open the https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open the _Network_ tab in the Chrome Devtools panel.
4. On the page, under **HTTPS** click the _Send via HTTPS_ button and verify a request has been sent in the _Network_ tab.
5. Verify in the command line that the server received the random number.

### WebRTC STUN
WebRTC STUN binding request are not visible in the _Network_ tab in the Chrome Devtools panel. Instead, packet capturing software like Wireshark can be used.
Alternativly, `chrome://webrtc-internals/` can be used. However, WebRTC connections only remain visible for a short period of time before disappearing.
1. Run `python webrtc.py` in the command line (Default port for HTTP is 10000)
2. Open the https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open `chrome://webrtc-internals/` in a different tab of the browser or open the packet capturing software.
4. On the page, under **WebRTC STUN** click the _Send via STUN_ button and verify a request has been sent on the `chrome://webrtc-internals/` tab or the packet capturing software.
5. Verify in the command line that the server received the random number.
The _Send with STUN_ button uses the method as used by Meta with the correct SDP-munging technique, while _Send with Adapted STUN_ uses a different version of SDP-munging in case the first method is blocked by the browser.


### WebRTC TURN
WebRTC TURN request are not visible in the _Network_ tab in the Chrome Devtools panel. Instead, packet capturing software like Wireshark can be used.
Alternativly, `chrome://webrtc-internals/` can be used. However, WebRTC connections only remain visible for a short period of time before disappearing.
1. Run `python webrtc_turn.py` in the command line (Default port for HTTP is 10001)
2. Open the https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/localhost_channel_methods.html file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/a57bf1eba6d102aaa378ad0c23a5f2a3/raw/c827400a54bc82b73302c6bebaf65dabbe34bdaf/localhost_channel_methods_page.html).
3. Open `chrome://webrtc-internals/` in a different tab of the browser or open the packet capturing software.
4. On the page, under **WebRTC TURN** click the _Send via TURN_ button and verify a request has been sent on the `chrome://webrtc-internals/` tab or the packet capturing software.
5. Verify in the command line that the server received the random number.
6. The `webrtc_turn.py` server shuts down after receiving a request and needs to be restarted to receive a new request.


## Setting up the HTTPS test domain
The HTTPS method requires a domain resolving to 127.0.0.1 as well as its certificates.

For the domain resolving to 127.0.0.1, the computer's `hosts` file needs to be edited (e.g., `C:\Windows\System32\drivers\etc\hosts` on Windows, `/etc/hosts` on Linux/macOS). Here, add a new entry with a chosen domain (e.g. myapp.local) and bind it to 127.0.0.1

To generate the certificate:
1. Run the following command in the same folder as the `http_https_websocket.py` file (Replace `myapp.local` with the chosen domain). 
```sh
openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem \ -days 365 \ -subj "/CN=myapp.local" \ -addext "subjectAltName=DNS:myapp.local"
```
2. Run [`python http_https_websocket.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/http_https_websocket.py) in a command line (Default port for HTTPS is 5001).
3. Visit the domain at the port it is hosted in a browser (e.g. `https://myapp.local:5001`).
4. Accept the certificate.
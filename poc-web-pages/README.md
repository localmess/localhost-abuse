# Reproduction of results
When using the web-pages in this folder, it is best to use the Chrome browser, as both the Brave and Firefox browser may block certain methods.
When prompted, accept the prompt to access the local network.

## Methods used by Meta and Yandex
This folder contains various .html and .py files allowing for the reproduction of the methods used by Meta and Yandex.
`localhost_channel_methods_page.html` contains recreations of the five methods these companies used.
The `*.py` server files recreate the app listening behaviour.
 - **HTTP**: method using requests send to the localhost over HTTP. Used by Yandex since February 2017 until June 2025 and by Meta since September 2024 until October 2024. Run [`test_server.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/test_server.py) to receive these requests.
 - **HTTPS**: method using requests sent over HTTPS to a domain resolving to 127.0.0.1. Used by Yandex since May 2018 until June 2025. Run [`test_server.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/test_server.py) to receive these requests. To run this method, make sure you use a domain that resolves to 127.0.0.1 (e.g. by rebinding it in your device's host file). The receiving server also needs a certificate for that domain. A certificate can be created in the same folder as the scripts using the following command:
`openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem \ -days 365 \ -subj "/CN=myapp.local" \ -addext "subjectAltName=DNS:myapp.local"`
After the creation of the certificate, run [`test_server.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/test_server.py) and visit the domain at the port it is hosted in a browser (default is `https://myapp.local:5001`). When prompted, accept the certificate. Once these steps are finished, the server should be able to receive the requests send using this method.
 - **WebSocket**: method establishing a WebSocket connection with a server at the localhost. Used by Meta from November 2024 to January 2025. Run [`test_server.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/test_server.py) to receive these requests.
 - **WebRTC STUN**: method using WebRTC STUN binding requests. The _Send with STUN_ button uses the method as used by Meta with the correct SDP-munging technique, while _Send with Adapted STUN_ uses a different version of SDP-munging in case the first method is blocked by the browser. Used by Meta from November 2024 to June 3rd. Run [`webRTC.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/webRTC.py) to receive the binding requests. Note that the Firefox browsers blocks these requests by default.
 - **WebRTC TURN**: method using WebRTC TURN requests that does not rely on SDP-munging. Implemented by Meta from May 2025 to June 3rd. Run [`webRTC_turn.py`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/webRTC_turn.py) to receive the binding requests. The server currently needs to be restarted after each received request.

The `lna-testing` folder contains two additional .html files used for evaluating Chrome LNA.
- [`LNA_testpage.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/lna-testing/LNA_testpage.html) contains the five methods used by Meta and Yandex in addition to a range of other methods to test.
- [`WebRTC_IPv6.html`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/lna-testing/WebRTC_IPv6.html) contains the WebRTC IPv6 method mentioned in Section 7.

- [`mDNS.py`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/lna-testing/mDNS.py) can be used alongside `LNA_testpage.html` to receive mDNS lookup requests as described in Section 7.

## Demos

https://gist.githack.com/TimVlummens/89af087aaaf1d79dfb0261b9c5f21d2e/raw/9109cfb0c958766dde19edbecc6c5ed2ff08a093/WebRTC_IPv6.html


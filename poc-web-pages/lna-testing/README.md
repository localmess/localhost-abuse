# Reproduction of results
When using the web-pages in this folder, it is best to use the Chrome browser, as both the Brave and Firefox browser may block certain methods.
The `Local Network Access Checks` and `Local Network Access Checks for WebRTC` flags can be enabled in the browser on `chrome://flags` to test the methods against Chrome's LNA.

This folder contains two additional files used for the additional attack vectors described in our paper in section 7.
The `*.py` server file recreate the app listening behaviour.

- [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) contains the two additional vectors.
    - **WebRTC & IPv6 Global Unicast Addresses**: method using WebRTC STUN binding requests to IPv6 Global Unicast Addresses to bypass Chrome LNA. Described in Section 7.1.
    - **mDNS lookup**: method using mDNS lookup requests to bypass Chrome LNA. Described in Section 7.2.
- [`mdns.py`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/additional-vectors/mdns.py) can be used alongside `additional_vectors.html` to receive mDNS lookup requests as described in Section 7.2.

## How to use
`additional_vectors.html` displays a random four digit number at the top of the page. Each method uses this number as well as the name of the method as data to send.

### WebRTC & IPv6 Global Unicast Addresses
WebRTC STUN binding request are not visible in the _Network_ tab in the Chrome Devtools panel. Instead, packet capturing software like Wireshark can be used.
Alternativly, `chrome://webrtc-internals/` can be used. However, WebRTC connections only remain visible for a short period of time before disappearing.
The [`webrtc.py`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/webrtc.py) file is the one found in the [`poc-web-pages`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages) folder
1. Run `python webrtc.py` in the command line (Default port for HTTP is 10000)
2. Open the [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/b668fa8a4cfdeec75d55911c42ebbc40/raw/e812b2126469aaaa3cdc3b17006757021a9a144a/additional_vectors.html).
3. Open `chrome://webrtc-internals/` in a different tab of the browser or open the packet capturing software.
4. On the page, under **WebRTC & IPv6 Global Unicast Addresses** click the _Send via STUN_ button and verify a request has been sent on the `chrome://webrtc-internals/` tab or the packet capturing software.
5. Verify in the command line that the server received the random number.

The _Index of Address_ field and _Candidate addresses_ allows for the selection of the candidate to use. By default, Chrome should place the IPv6 addresses last. The addresses may be masked as UUID.local addresses.

### WebRTC & IPv6 Global Unicast Addresses
mDNS lookup request are not visible in the _Network_ tab in the Chrome Devtools panel. Instead, packet capturing software like Wireshark can be used.
1. Run `python mdns.py` in the command line.
2. Open the [`additional_vectors.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/additional-vectors/additional_vectors.html) file in a browser or visit the [hosted version](https://gistcdn.githack.com/TimVlummens/b668fa8a4cfdeec75d55911c42ebbc40/raw/e812b2126469aaaa3cdc3b17006757021a9a144a/additional_vectors.html).
3. On the page, under **mDNS lookup** click the _Send mDNS lookup request_ button and verify a request has been sent in the packet capturing software.
4. Verify in the command line that the server received the random number.

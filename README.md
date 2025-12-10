# Artifacts for Usenix Security Submission #1381 "Bridges to Self: Silent Web-to-App Tracking on Mobile via Localhost"

The repository contains code and data for the USENIX Security Submission #1381. Detailed instructions can be found by following the links:
- [Proof-of-Concept Android apps](https://github.com/localmess/localhost-abuse/tree/main/android-pocs): Contains Proof-of-Concept (PoC) Android apps that demonstrates how apps can receive IDs shared by web script via different protocols and methods (HTTP, WebSocket, WebRTC-STUN, WebRTC-TURN, mDNS).
- [Proof-of-Concept web pages](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages): Contains PoC web pages that demonstrate how scripts can share IDs with apps running on the same device via different methods.
- [Frida scripts](https://github.com/localmess/localhost-abuse/tree/main/frida-scripts): Contains Frida scripts used our dynamic app analyses.
- [Web crawler](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler): Contains the web crawler code used to search for localhost communications on real-world websites. Based on DuckDuckGo's [tracker-radar-collector](https://github.com/duckduckgo/tracker-radar-collector).

# Artifacts for "Bridges to Self: Silent Web-to-App Tracking on Mobile via Localhost" (USENIX Security'26)

The repository contains code and data for the USENIX Security Submission #1381. Detailed instructions can be found by following the links:
- [Proof-of-Concept Android apps](https://github.com/localmess/localhost-abuse/tree/main/android-pocs): Contains Proof-of-Concept (PoC) Android apps that demonstrates how apps can receive IDs shared by web script via different protocols and methods (HTTP, WebSocket, WebRTC-STUN, WebRTC-TURN, mDNS).
- [Proof-of-Concept web pages](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages): Contains PoC web pages that demonstrate how scripts can share IDs with apps running on the same device via different methods.
- [Frida scripts](https://github.com/localmess/localhost-abuse/tree/main/frida-scripts): Contains Frida scripts used for our dynamic app analyses.
- [Web crawler](https://github.com/localmess/localhost-abuse/tree/main/web-analysis-crawler): Contains the web crawler code used to search for localhost communications on real-world websites. Based on DuckDuckGo's [tracker-radar-collector](https://github.com/duckduckgo/tracker-radar-collector).
- [Crawl Data Processing](https://github.com/localmess/localhost-abuse/tree/main/crawl-data-processing): Contains a list of crawls, scripts to process crawl data, and summary .csv files obtained by processing the crawl data.


<img width="3958" height="1412" alt="Figure showing how web scripts can share IDs with mobile apps via different methods. Taken from the localmess.github.io webpage." src="https://github.com/user-attachments/assets/51d161b4-d4e3-4715-a210-417b2fbf05f9" />


## 🙏 Acknowledgements
 We thank all participating browser vendors (Chrome, Mozilla, DuckDuckGo, Brave) for their collaboration. Special thanks to:

- Álvaro Feal, for his help with mobile app testing in the US.
- Tom Van Goethem, for diligently handling our disclosure
- Bart Preneel, for his help with media outreach
- HTTP Archive Project, for their public dataset that enabled our longitudinal analysis
- Schloss Dagstuhl – Leibniz Center for Informatics, for sparking this collaboration
- DuckDuckGo's [tracker-radar-collector](https://github.com/duckduckgo/tracker-radar-collector), which our web crawler is based on.

## 📚 Reference

You can use the following BibTeX to cite our paper:
```
@article{localmess-usenix-sec-26,
    title   = {{Bridges to Self: Silent Web-to-App Tracking on Mobile via Localhost}},
    author  = {Tim Vlummens and Aniketh Girish and Nipuna Weerasekara and Frederik Zuiderveen Borgesius and Gunes Acar and Narseo Vallina Rodriguez},
    booktitle={35th USENIX Security Symposium (USENIX Security 26)},
    year    = {2026}
}
```

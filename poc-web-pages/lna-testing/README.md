# Reproduction of results
When using the web-pages in this folder, it is best to use the Chrome browser, as both the Brave and Firefox browser may block certain methods.
When prompted, accept the prompt to access the local network.

This folder contains two additional files used for evaluating Chrome LNA.
The `*.py` server file recreate the app listening behaviour.

- [`lna_testpage.html`](https://github.com/localmess/localhost-abuse/blob/main/poc-web-pages/lna-testing/lna_testpage.html) contains the five methods used by Meta and Yandex in addition to a range of other methods to test.
- [`mdns.py`](https://github.com/localmess/localhost-abuse/tree/main/poc-web-pages/lna-testing/mdns.py) can be used alongside `lna_testpage.html` to receive mDNS lookup requests as described in Section 7.


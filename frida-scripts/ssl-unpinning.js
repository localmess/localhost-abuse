/*
 * This script combines, fixes & extends a long list of other scripts, most notably including:
 *
 * - https://codeshare.frida.re/@akabe1/frida-multiple-unpinning/
 * - https://codeshare.frida.re/@avltree9798/universal-android-ssl-pinning-bypass/
 * - https://pastebin.com/TVJD63uM
 */

setImmediate(ssl_unpinning);

function logger(message) {
    try {
        send({
            contentType: 'universal-unpinning',
            info: message
        });
    } catch (_) {}
}


function ssl_unpinning() {
    Java.perform(function () {
        try {
            logger("---");
            logger("Unpinning Android app...");

            hookSSLPeerUnverifiedException();
            hookHttpsURLConnection();
            hookSSLContext();
            hookTrustManagerImpl();
            hookOkhttp();
            hookTrustKit();
            hookTitanium();
            hookOpenSSL();
            hookPhoneGap();
            hookIBMworklight();
            hookConscrypt();
            hookcwac();
            hookandroidgapworklight();
            hookNetty();
            hooksquareup();
            hookWebview();
            hookCordovaWebView();
            hookBoye();

            logger("Unpinning setup completed");
        } catch (err) {
            if (err instanceof Error) {
                logger(err.message);
            };
        };
    });
}

var message = {};
message["contentType"] = "ssl-unpinning";

function hookSSLPeerUnverifiedException() {
    /// -- Generic hook to protect against SSLPeerUnverifiedException -- ///

    // In some cases, with unusual cert pinning approaches, or heavy obfuscation, we can't
    // match the real method & package names. This is a problem! Fortunately, we can still
    // always match built-in types, so here we spot all failures that use the built-in cert
    // error type (notably this includes OkHttp), and after the first failure, we dynamically
    // generate & inject a patch to completely disable the method that threw the error.
    try {
        const UnverifiedCertError = Java.use('javax.net.ssl.SSLPeerUnverifiedException');
        UnverifiedCertError.$init.implementation = function (str) {
            logger('  --> Unexpected SSL verification failure, adding dynamic patch...');

            try {
                const stackTrace = Java.use('java.lang.Thread').currentThread().getStackTrace();
                const exceptionStackIndex = stackTrace.findIndex((stack) =>
                    stack.getClassName() === "javax.net.ssl.SSLPeerUnverifiedException"
                );
                const callingFunctionStack = stackTrace[exceptionStackIndex + 1];

                const className = callingFunctionStack.getClassName();
                const methodName = callingFunctionStack.getMethodName();

                logger(`      Thrown by ${className}->${methodName}`);

                const callingClass = Java.use(className);
                const callingMethod = callingClass[methodName];

                if (callingMethod.implementation) return; // Already patched by Frida - skip it

                logger('      Attempting to patch automatically...');
                const returnTypeName = callingMethod.returnType.type;

                callingMethod.implementation = function () {
                    logger(`  --> Bypassing ${className}->${methodName} (automatic exception patch)`);

                    // This is not a perfect fix! Most unknown cases like this are really just
                    // checkCert(cert) methods though, so doing nothing is perfect, and if we
                    // do need an actual return value then this is probably the best we can do,
                    // and at least we're logging the method name so you can patch it manually:

                    if (returnTypeName === 'void') {
                        return;
                    } else {
                        return null;
                    }
                };

                logger('[+] ${className}->${methodName} (automatic exception patch)');
            } catch (e) {
                logger('[ ] Failed to automatically patch failure');
            }

            return this.$init(str);
        };
        logger('[+] SSLPeerUnverifiedException auto-patcher');
        message["info"] = "unpinned: [+] SSLPeerUnverifiedException auto-patcher";
        send(message);
    } catch (err) {
        logger('[ ] SSLPeerUnverifiedException auto-patcher');
    }
}

function hookHttpsURLConnection() {
    try {
        const HttpsURLConnection = Java.use("javax.net.ssl.HttpsURLConnection");
        HttpsURLConnection.setDefaultHostnameVerifier.implementation = function (hostnameVerifier) {
            logger('  --> Bypassing HttpsURLConnection (setDefaultHostnameVerifier)');
            return; // Do nothing, i.e. don't change the hostname verifier
        };
        message["info"] = 'unpinned: [+] HttpsURLConnection (setDefaultHostnameVerifier)'
        send(message);
        logger('[+] HttpsURLConnection (setDefaultHostnameVerifier)');
    } catch (err) {
        logger('[ ] HttpsURLConnection (setDefaultHostnameVerifier)');
    }
    try {
        const HttpsURLConnection = Java.use("javax.net.ssl.HttpsURLConnection");
        HttpsURLConnection.setSSLSocketFactory.implementation = function (SSLSocketFactory) {
            logger('  --> Bypassing HttpsURLConnection (setSSLSocketFactory)');
            return; // Do nothing, i.e. don't change the SSL socket factory
        };
        message["info"] = 'unpinned: [+] HttpsURLConnection (setSSLSocketFactory)';
        send(message);
        logger('[+] HttpsURLConnection (setSSLSocketFactory)');
    } catch (err) {
        logger('[ ] HttpsURLConnection (setSSLSocketFactory)');
    }
    try {
        const HttpsURLConnection = Java.use("javax.net.ssl.HttpsURLConnection");
        HttpsURLConnection.setHostnameVerifier.implementation = function (hostnameVerifier) {
            logger('  --> Bypassing HttpsURLConnection (setHostnameVerifier)');
            return; // Do nothing, i.e. don't change the hostname verifier
        };
        message["info"] = 'unpinned: [+] HttpsURLConnection (setHostnameVerifier)';
        send(message);
        logger('[+] HttpsURLConnection (setHostnameVerifier)');
    } catch (err) {
        logger('[ ] HttpsURLConnection (setHostnameVerifier)');
    }
}

function hookSSLContext() {
    try {
        const X509TrustManager = Java.use('javax.net.ssl.X509TrustManager');
        const SSLContext = Java.use('javax.net.ssl.SSLContext');

        const TrustManager = Java.registerClass({
            // Implement a custom TrustManager
            name: 'dev.asd.test.TrustManager',
            implements: [X509TrustManager],
            methods: {
                checkClientTrusted: function (chain, authType) { },
                checkServerTrusted: function (chain, authType) { },
                getAcceptedIssuers: function () { return []; }
            }
        });

        // Prepare the TrustManager array to pass to SSLContext.init()
        const TrustManagers = [TrustManager.$new()];

        // Get a handle on the init() on the SSLContext class
        const SSLContext_init = SSLContext.init.overload(
            '[Ljavax.net.ssl.KeyManager;', '[Ljavax.net.ssl.TrustManager;', 'java.security.SecureRandom'
        );

        // Override the init method, specifying the custom TrustManager
        SSLContext_init.implementation = function (keyManager, trustManager, secureRandom) {
            logger('  --> Bypassing Trustmanager (Android < 7) request');
            SSLContext_init.call(this, keyManager, trustManager, secureRandom);
        };
        message["info"] = 'unpinned: [+] SSLContext'
        send(message);
        logger('[+] SSLContext');
    } catch (err) {
        logger('[ ] SSLContext');
    }
}

function hookTrustManagerImpl() {
    try {
        const array_list = Java.use("java.util.ArrayList");
        const TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');

        // This step is notably what defeats the most common case: network security config
        TrustManagerImpl.checkTrustedRecursive.implementation = function (a1, a2, a3, a4, a5, a6) {
            logger('  --> Bypassing TrustManagerImpl checkTrusted ');
            return array_list.$new();
        }

        TrustManagerImpl.verifyChain.implementation = function (untrustedChain, trustAnchorChain, host, clientAuth, ocspData, tlsSctData) {
            logger('  --> Bypassing TrustManagerImpl verifyChain: ' + host);
            return untrustedChain;
        };
        message["info"] = 'unpinned: [+] TrustManagerImpl';
        send(message);
        logger('[+] TrustManagerImpl');
    } catch (err) {
        logger('[ ] TrustManagerImpl');
    }
}

function hookOkhttp() {
    // OkHTTPv3 (quadruple bypass)
    try {
        // Bypass OkHTTPv3 {1}
        const okhttp3_Activity_1 = Java.use('okhttp3.CertificatePinner');
        okhttp3_Activity_1.check.overload('java.lang.String', 'java.util.List').implementation = function (a, b) {
            logger('  --> Bypassing OkHTTPv3 (list): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] OkHTTPv3 (list)';
        send(message);
        logger('[+] OkHTTPv3 (list)');
    } catch (err) {
        logger('[ ] OkHTTPv3 (list)');
    }
    try {
        // Bypass OkHTTPv3 {2}
        // This method of CertificatePinner.check could be found in some old Android app
        const okhttp3_Activity_2 = Java.use('okhttp3.CertificatePinner');
        okhttp3_Activity_2.check.overload('java.lang.String', 'java.security.cert.Certificate').implementation = function (a, b) {
            logger('  --> Bypassing OkHTTPv3 (cert): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] OkHTTPv3 (cert)';
        send(message);
        logger('[+] OkHTTPv3 (cert)');
    } catch (err) {
        logger('[ ] OkHTTPv3 (cert)');
    }
    try {
        // Bypass OkHTTPv3 {3}
        const okhttp3_Activity_3 = Java.use('okhttp3.CertificatePinner');
        okhttp3_Activity_3.check.overload('java.lang.String', '[Ljava.security.cert.Certificate;').implementation = function (a, b) {
            logger('  --> Bypassing OkHTTPv3 (cert array): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] OkHTTPv3 (cert array)';
        send(message);
        logger('[+] OkHTTPv3 (cert array)');
    } catch (err) {
        logger('[ ] OkHTTPv3 (cert array)');
    }
    try {
        // Bypass OkHTTPv3 {4}
        const okhttp3_Activity_4 = Java.use('okhttp3.CertificatePinner');
        okhttp3_Activity_4['check$okhttp'].implementation = function (a, b) {
            logger('  --> Bypassing OkHTTPv3 ($okhttp): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] OkHTTPv3 ($okhttp)';
        send(message);
        logger('[+] OkHTTPv3 ($okhttp)');
    } catch (err) {
        logger('[ ] OkHTTPv3 ($okhttp)');
    }
}

function hookTrustKit() {
    try {
        // Bypass Trustkit {1}
        const trustkit_Activity_1 = Java.use('com.datatheorem.android.trustkit.pinning.OkHostnameVerifier');
        trustkit_Activity_1.verify.overload('java.lang.String', 'javax.net.ssl.SSLSession').implementation = function (a, b) {
            logger('  --> Bypassing Trustkit OkHostnameVerifier(SSLSession): ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] Trustkit OkHostnameVerifier(SSLSession)';
        send(message);
        logger('[+] Trustkit OkHostnameVerifier(SSLSession)');
    } catch (err) {
        logger('[ ] Trustkit OkHostnameVerifier(SSLSession)');
    }
    try {
        // Bypass Trustkit {2}
        const trustkit_Activity_2 = Java.use('com.datatheorem.android.trustkit.pinning.OkHostnameVerifier');
        trustkit_Activity_2.verify.overload('java.lang.String', 'java.security.cert.X509Certificate').implementation = function (a, b) {
            logger('  --> Bypassing Trustkit OkHostnameVerifier(cert): ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] Trustkit OkHostnameVerifier(cert)'
        send(message);
        logger('[+] Trustkit OkHostnameVerifier(cert)');
    } catch (err) {
        logger('[ ] Trustkit OkHostnameVerifier(cert)');
    }
    try {
        // Bypass Trustkit {3}
        const trustkit_PinningTrustManager = Java.use('com.datatheorem.android.trustkit.pinning.PinningTrustManager');
        trustkit_PinningTrustManager.checkServerTrusted.implementation = function () {
            logger('  --> Bypassing Trustkit PinningTrustManager');
        };
        message["info"] = 'unpinned: [+] Trustkit PinningTrustManager';
        send(message);
        logger('[+] Trustkit PinningTrustManager');
    } catch (err) {
        logger('[ ] Trustkit PinningTrustManager');
    }
}

function hookTitanium() {
    try {
        const appcelerator_PinningTrustManager = Java.use('appcelerator.https.PinningTrustManager');
        appcelerator_PinningTrustManager.checkServerTrusted.implementation = function () {
            logger('  --> Bypassing Appcelerator PinningTrustManager');
        };
        message["info"] = 'unpinned: [+] Appcelerator PinningTrustManager';
        send(message);
        logger('[+] Appcelerator PinningTrustManager');
    } catch (err) {
        logger('[ ] Appcelerator PinningTrustManager');
    }
}

function hookOpenSSL() {
    try {
        const OpenSSLSocketImpl = Java.use('com.android.org.conscrypt.OpenSSLSocketImpl');
        OpenSSLSocketImpl.verifyCertificateChain.implementation = function (certRefs, JavaObject, authMethod) {
            logger('  --> Bypassing OpenSSLSocketImpl Conscrypt');
        };
        message["info"] = 'unpinned: [+] OpenSSLSocketImpl Conscrypt';
        send(message);
        logger('[+] OpenSSLSocketImpl Conscrypt');
    } catch (err) {
        logger('[ ] OpenSSLSocketImpl Conscrypt');
    }

    // OpenSSLEngineSocketImpl Conscrypt
    try {
        const OpenSSLEngineSocketImpl_Activity = Java.use('com.android.org.conscrypt.OpenSSLEngineSocketImpl');
        OpenSSLEngineSocketImpl_Activity.verifyCertificateChain.overload('[Ljava.lang.Long;', 'java.lang.String').implementation = function (a, b) {
            logger('  --> Bypassing OpenSSLEngineSocketImpl Conscrypt: ' + b);
        };
        message["info"] = 'unpinned: [+] OpenSSLEngineSocketImpl Conscrypt';
        send(message);
        logger('[+] OpenSSLEngineSocketImpl Conscrypt');
    } catch (err) {
        logger('[ ] OpenSSLEngineSocketImpl Conscrypt');
    }

    // OpenSSLSocketImpl Apache Harmony
    try {
        const OpenSSLSocketImpl_Harmony = Java.use('org.apache.harmony.xnet.provider.jsse.OpenSSLSocketImpl');
        OpenSSLSocketImpl_Harmony.verifyCertificateChain.implementation = function (asn1DerEncodedCertificateChain, authMethod) {
            logger('  --> Bypassing OpenSSLSocketImpl Apache Harmony');
        };
        message["info"] = 'unpinned: [+] OpenSSLSocketImpl Apache Harmony';
        send(message);
        logger('[+] OpenSSLSocketImpl Apache Harmony');
    } catch (err) {
        logger('[ ] OpenSSLSocketImpl Apache Harmony');
    }

}

function hookPhoneGap() {
            // PhoneGap sslCertificateChecker (https://github.com/EddyVerbruggen/SSLCertificateChecker-PhoneGap-Plugin)
            try {
                const phonegap_Activity = Java.use('nl.xservices.plugins.sslCertificateChecker');
                phonegap_Activity.execute.overload('java.lang.String', 'org.json.JSONArray', 'org.apache.cordova.CallbackContext').implementation = function (a, b, c) {
                    logger('  --> Bypassing PhoneGap sslCertificateChecker: ' + a);
                    return true;
                };
                message["info"] = 'unpinned: [+] PhoneGap sslCertificateChecker';
                send(message);
                logger('[+] PhoneGap sslCertificateChecker');
            } catch (err) {
                logger('[ ] PhoneGap sslCertificateChecker');
            }
}


function hookIBMworklight() {
            // IBM MobileFirst pinTrustedCertificatePublicKey (double bypass)
            try {
                // Bypass IBM MobileFirst {1}
                const WLClient_Activity_1 = Java.use('com.worklight.wlclient.api.WLClient');
                WLClient_Activity_1.getInstance().pinTrustedCertificatePublicKey.overload('java.lang.String').implementation = function (cert) {
                    logger('  --> Bypassing IBM MobileFirst pinTrustedCertificatePublicKey (string): ' + cert);
                    return;
                };
                message["info"] = 'unpinned: [+] IBM MobileFirst pinTrustedCertificatePublicKey (string)';
                send(message);
                logger('[+] IBM MobileFirst pinTrustedCertificatePublicKey (string)');
            } catch (err) {
                logger('[ ] IBM MobileFirst pinTrustedCertificatePublicKey (string)');
            }
            try {
                // Bypass IBM MobileFirst {2}
                const WLClient_Activity_2 = Java.use('com.worklight.wlclient.api.WLClient');
                WLClient_Activity_2.getInstance().pinTrustedCertificatePublicKey.overload('[Ljava.lang.String;').implementation = function (cert) {
                    logger('  --> Bypassing IBM MobileFirst pinTrustedCertificatePublicKey (string array): ' + cert);
                    return;
                };
                message["info"] = 'unpinned: [+] IBM MobileFirst pinTrustedCertificatePublicKey (string array)'
                send(message);
                logger('[+] IBM MobileFirst pinTrustedCertificatePublicKey (string array)');
            } catch (err) {
                logger('[ ] IBM MobileFirst pinTrustedCertificatePublicKey (string array)');
            }
    
            // IBM WorkLight (ancestor of MobileFirst) HostNameVerifierWithCertificatePinning (quadruple bypass)
            try {
                // Bypass IBM WorkLight {1}
                const worklight_Activity_1 = Java.use('com.worklight.wlclient.certificatepinning.HostNameVerifierWithCertificatePinning');
                worklight_Activity_1.verify.overload('java.lang.String', 'javax.net.ssl.SSLSocket').implementation = function (a, b) {
                    logger('  --> Bypassing IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSocket): ' + a);
                    return;
                };
                message["info"] = 'unpinned: [+] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSocket)';
                send(message);
                logger('[+] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSocket)');
            } catch (err) {
                logger('[ ] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSocket)');
            }
            try {
                // Bypass IBM WorkLight {2}
                const worklight_Activity_2 = Java.use('com.worklight.wlclient.certificatepinning.HostNameVerifierWithCertificatePinning');
                worklight_Activity_2.verify.overload('java.lang.String', 'java.security.cert.X509Certificate').implementation = function (a, b) {
                    logger('  --> Bypassing IBM WorkLight HostNameVerifierWithCertificatePinning (cert): ' + a);
                    return;
                };
                message["info"] = 'unpinned: [+] IBM WorkLight HostNameVerifierWithCertificatePinning (cert)';
                send(message);
                logger('[+] IBM WorkLight HostNameVerifierWithCertificatePinning (cert)');
            } catch (err) {
                logger('[ ] IBM WorkLight HostNameVerifierWithCertificatePinning (cert)');
            }
            try {
                // Bypass IBM WorkLight {3}
                const worklight_Activity_3 = Java.use('com.worklight.wlclient.certificatepinning.HostNameVerifierWithCertificatePinning');
                worklight_Activity_3.verify.overload('java.lang.String', '[Ljava.lang.String;', '[Ljava.lang.String;').implementation = function (a, b) {
                    logger('  --> Bypassing IBM WorkLight HostNameVerifierWithCertificatePinning (string string): ' + a);
                    return;
                };
                message["info"] = 'unpinned: [+] IBM WorkLight HostNameVerifierWithCertificatePinning (string string)';
                send(message);
                logger('[+] IBM WorkLight HostNameVerifierWithCertificatePinning (string string)');
            } catch (err) {
                logger('[ ] IBM WorkLight HostNameVerifierWithCertificatePinning (string string)');
            }
            try {
                // Bypass IBM WorkLight {4}
                const worklight_Activity_4 = Java.use('com.worklight.wlclient.certificatepinning.HostNameVerifierWithCertificatePinning');
                worklight_Activity_4.verify.overload('java.lang.String', 'javax.net.ssl.SSLSession').implementation = function (a, b) {
                    logger('  --> Bypassing IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSession): ' + a);
                    return true;
                };
                message["info"] = 'unpinned: [+] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSession)';
                send(message);
                logger('[+] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSession)');
            } catch (err) {
                logger('[ ] IBM WorkLight HostNameVerifierWithCertificatePinning (SSLSession)');
            }
}

function hookConscrypt() {
            // Conscrypt CertPinManager
            try {
                const conscrypt_CertPinManager_Activity = Java.use('com.android.org.conscrypt.CertPinManager');
                conscrypt_CertPinManager_Activity.isChainValid.overload('java.lang.String', 'java.util.List').implementation = function (a, b) {
                    logger('  --> Bypassing Conscrypt CertPinManager: ' + a);
                    return true;
                };
                message["info"] = 'unpinned: [+] Conscrypt CertPinManager';
                send(message);
                logger('[+] Conscrypt CertPinManager');
            } catch (err) {
                logger('[ ] Conscrypt CertPinManager');
            }
    
}

function hookcwac() {
    try {
        const cwac_CertPinManager_Activity = Java.use('com.commonsware.cwac.netsecurity.conscrypt.CertPinManager');
        cwac_CertPinManager_Activity.isChainValid.overload('java.lang.String', 'java.util.List').implementation = function (a, b) {
            logger('  --> Bypassing CWAC-Netsecurity CertPinManager: ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] CWAC-Netsecurity CertPinManager';
        send(message);
        logger('[+] CWAC-Netsecurity CertPinManager');
    } catch (err) {
        logger('[ ] CWAC-Netsecurity CertPinManager');
    }
}

function hookandroidgapworklight() {
    try {
        const androidgap_WLCertificatePinningPlugin_Activity = Java.use('com.worklight.androidgap.plugin.WLCertificatePinningPlugin');
        androidgap_WLCertificatePinningPlugin_Activity.execute.overload('java.lang.String', 'org.json.JSONArray', 'org.apache.cordova.CallbackContext').implementation = function (a, b, c) {
            logger('  --> Bypassing Worklight Androidgap WLCertificatePinningPlugin: ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] Worklight Androidgap WLCertificatePinningPlugin'
        send(message);
        logger('[+] Worklight Androidgap WLCertificatePinningPlugin');
    } catch (err) {
        logger('[ ] Worklight Androidgap WLCertificatePinningPlugin');
    }

}

function hookNetty() {
            // Netty FingerprintTrustManagerFactory
            try {
                const netty_FingerprintTrustManagerFactory = Java.use('io.netty.handler.ssl.util.FingerprintTrustManagerFactory');
                netty_FingerprintTrustManagerFactory.checkTrusted.implementation = function (type, chain) {
                    logger('  --> Bypassing Netty FingerprintTrustManagerFactory');
                };
                message["info"] = 'unpinned: [+] Netty FingerprintTrustManagerFactory';
                send(message);
                logger('[+] Netty FingerprintTrustManagerFactory');
            } catch (err) {
                logger('[ ] Netty FingerprintTrustManagerFactory');
            }
}


function hooksquareup() {
     // Squareup CertificatePinner [OkHTTP<v3] (double bypass)
     try {
        // Bypass Squareup CertificatePinner {1}
        const Squareup_CertificatePinner_Activity_1 = Java.use('com.squareup.okhttp.CertificatePinner');
        Squareup_CertificatePinner_Activity_1.check.overload('java.lang.String', 'java.security.cert.Certificate').implementation = function (a, b) {
            logger('  --> Bypassing Squareup CertificatePinner (cert): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] Squareup CertificatePinner (cert)';
        send(message);
        logger('[+] Squareup CertificatePinner (cert)');
    } catch (err) {
        logger('[ ] Squareup CertificatePinner (cert)');
    }
    try {
        // Bypass Squareup CertificatePinner {2}
        const Squareup_CertificatePinner_Activity_2 = Java.use('com.squareup.okhttp.CertificatePinner');
        Squareup_CertificatePinner_Activity_2.check.overload('java.lang.String', 'java.util.List').implementation = function (a, b) {
            logger('  --> Bypassing Squareup CertificatePinner (list): ' + a);
            return;
        };
        message["info"] = 'unpinned: [+] Squareup CertificatePinner (list)';
        send(message);
        logger('[+] Squareup CertificatePinner (list)');
    } catch (err) {
        logger('[ ] Squareup CertificatePinner (list)');
    }

    // Squareup OkHostnameVerifier [OkHTTP v3] (double bypass)
    try {
        // Bypass Squareup OkHostnameVerifier {1}
        const Squareup_OkHostnameVerifier_Activity_1 = Java.use('com.squareup.okhttp.internal.tls.OkHostnameVerifier');
        Squareup_OkHostnameVerifier_Activity_1.verify.overload('java.lang.String', 'java.security.cert.X509Certificate').implementation = function (a, b) {
            logger('  --> Bypassing Squareup OkHostnameVerifier (cert): ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] Squareup OkHostnameVerifier (cert)';
        send(message);
        logger('[+] Squareup OkHostnameVerifier (cert)');
    } catch (err) {
        logger('[ ] Squareup OkHostnameVerifier (cert)');
    }
    try {
        // Bypass Squareup OkHostnameVerifier {2}
        const Squareup_OkHostnameVerifier_Activity_2 = Java.use('com.squareup.okhttp.internal.tls.OkHostnameVerifier');
        Squareup_OkHostnameVerifier_Activity_2.verify.overload('java.lang.String', 'javax.net.ssl.SSLSession').implementation = function (a, b) {
            logger('  --> Bypassing Squareup OkHostnameVerifier (SSLSession): ' + a);
            return true;
        };
        message["info"] = 'unpinned: [+] Squareup OkHostnameVerifier (SSLSession)';
        send(message);
        logger('[+] Squareup OkHostnameVerifier (SSLSession)');
    } catch (err) {
        logger('[ ] Squareup OkHostnameVerifier (SSLSession)');
    }

}

function hookWebview() {
    try {
        // Bypass WebViewClient {1} (deprecated from Android 6)
        const AndroidWebViewClient_Activity_1 = Java.use('android.webkit.WebViewClient');
        AndroidWebViewClient_Activity_1.onReceivedSslError.overload('android.webkit.WebView', 'android.webkit.SslErrorHandler', 'android.net.http.SslError').implementation = function (obj1, obj2, obj3) {
            logger('  --> Bypassing Android WebViewClient (SslErrorHandler)');
        };
        message["info"] = 'unpinned: [+] Android WebViewClient (SslErrorHandler)';
        send(message);
        logger('[+] Android WebViewClient (SslErrorHandler)');
    } catch (err) {
        logger('[ ] Android WebViewClient (SslErrorHandler)');
    }
    try {
        // Bypass WebViewClient {2}
        const AndroidWebViewClient_Activity_2 = Java.use('android.webkit.WebViewClient');
        AndroidWebViewClient_Activity_2.onReceivedSslError.overload('android.webkit.WebView', 'android.webkit.WebResourceRequest', 'android.webkit.WebResourceError').implementation = function (obj1, obj2, obj3) {
            logger('  --> Bypassing Android WebViewClient (WebResourceError)');
        };
        message["info"] = 'unpinned: [+] Android WebViewClient (WebResourceError)';
        send(message);
        logger('[+] Android WebViewClient (WebResourceError)');
    } catch (err) {
        logger('[ ] Android WebViewClient (WebResourceError)');
    }
}

function hookCordovaWebView() {
            // Apache Cordova WebViewClient
            try {
                const CordovaWebViewClient_Activity = Java.use('org.apache.cordova.CordovaWebViewClient');
                CordovaWebViewClient_Activity.onReceivedSslError.overload('android.webkit.WebView', 'android.webkit.SslErrorHandler', 'android.net.http.SslError').implementation = function (obj1, obj2, obj3) {
                    logger('  --> Bypassing Apache Cordova WebViewClient');
                    obj3.proceed();
                };
                message["info"] = 'unpinned: [+] Apache Cordova WebViewClient';
                send(message);
                logger('[+] Apache Cordova WebViewClient');
            } catch (err) {
                logger('[ ] Apache Cordova WebViewClient');
            }
}

function hookBoye() {
            // Boye AbstractVerifier
            try {
                const boye_AbstractVerifier = Java.use('ch.boye.httpclientandroidlib.conn.ssl.AbstractVerifier');
                boye_AbstractVerifier.verify.implementation = function (host, ssl) {
                    logger('  --> Bypassing Boye AbstractVerifier: ' + host);
                };
                message["info"] = 'unpinned: [+] Boye AbstractVerifier';
                send(message);
                logger('[+] Boye AbstractVerifier');
            } catch (err) {
                logger('[ ] Boye AbstractVerifier');
            }
}
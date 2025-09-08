'use strict';

var isTigonMNSServiceHolderHooked = false;

function logger(message) {
    try {
        send({
            contentType: 'instagram-unpinning',
            info: message
        });
    } catch (_) {}
}


function waitForModuleAndHook(moduleName) {
    return new Promise(resolve => {
        const interval = setInterval(() => {
            const module = Process.findModuleByName(moduleName);
            if (module != null) {
                clearInterval(interval);
                resolve(module);
            }
        }, 60);
    });
}

function hookTigonMNS() {
    try {
        Java.perform(() => {
            const TigonMNSServiceHolder = Java.use("com.facebook.tigon.tigonmns.TigonMNSServiceHolder");
            TigonMNSServiceHolder.initHybrid
                .overload("com.facebook.tigon.tigonmns.TigonMNSConfig", "java.lang.String", "long", "com.facebook.tigon.tigonhuc.HucClient", "boolean")
                .implementation = function (cfg, str, l, hucClient, z) {
                    cfg.setEnableCertificateVerificationWithProofOfPossession(false);
                    cfg.setTrustSandboxCertificates(true);
                    cfg.setForceHttp2(true);
                    return this.initHybrid(cfg, str, l, hucClient, z);
                };
            logger("[+] Hooked TigonMNSServiceHolder.initHybrid");
        });
    } catch (e) {
        logger("[-] Failed to hook TigonMNSServiceHolder.initHybrid");
    }
}

function hookTrustManagerImpl() {
    Java.perform(() => {
        try {
            const ArrayList = Java.use("java.util.ArrayList");
            const TrustManagerImpl = Java.use("com.android.org.conscrypt.TrustManagerImpl");
            if (TrustManagerImpl.checkTrustedRecursive) {
                TrustManagerImpl.checkTrustedRecursive.implementation = function () {
                    logger("[+] Bypassed TrustManagerImpl.checkTrustedRecursive()");
                    return ArrayList.$new();
                };
                logger("[+] Hooked TrustManagerImpl.checkTrustedRecursive");
            } else {
                logger("[-] TrustManagerImpl.checkTrustedRecursive not found");
            }
        } catch (e) {
            logger("[-] Failed to hook TrustManagerImpl: " + e);
        }
    });
}

function hookSSLContext() {
    Java.perform(() => {
        try {
            const X509TrustManager = Java.use("javax.net.ssl.X509TrustManager");
            const SSLContext = Java.use("javax.net.ssl.SSLContext");

            const CustomTrustManager = Java.registerClass({
                name: "com.leftenter.instagram",
                implements: [X509TrustManager],
                methods: {
                    checkClientTrusted(chain, authType) {},
                    checkServerTrusted(chain, authType) {},
                    getAcceptedIssuers() { return []; }
                }
            });

            const TrustManagers = [CustomTrustManager.$new()];
            const SSLContextInit = SSLContext.init.overload("[Ljavax.net.ssl.KeyManager;", "[Ljavax.net.ssl.TrustManager;", "java.security.SecureRandom");

            SSLContextInit.implementation = function (keyManager, trustManager, secureRandom) {
                logger("[+] Hooked SSLContext.init()");
                return SSLContextInit.call(this, keyManager, TrustManagers, secureRandom);
            };
        } catch (e) {
            logger("[-] Failed to hook SSLContext.init(): " + e);
        }
    });
}

function main() {
    waitForModuleAndHook("libstartup.so").then(() => {
        hookTigonMNS();
    }).catch((err) => {
        logger("[-] Failed to load libstartup.so: " + err);
    });
}

Java.perform(() => {
    setImmediate(main);
});

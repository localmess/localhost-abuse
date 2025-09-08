'use strict';

function logger(message) {
    try {
        send({
            contentType: 'universal-unpinning',
            info: message
        });
    } catch (_) {}
}

const facebookNative = {
    module: "libcoldstart.so",
    functionName: "_ZN8proxygen15SSLVerification17verifyWithMetricsEbP17x509_store_ctx_stRKNSt6__ndk212basic_stringIcNS3_11char_traitsIcEENS3_9allocatorIcEEEEPNS0_31SSLFailureVerificationCallbacksEPNS0_31SSLSuccessVerificationCallbacksERKNS_15TimeUtilGenericINS3_6chrono12steady_clockEEERNS_10TraceEventE"
};

function hookFacebookNativeCertVerification() {
    try {
        const module = Process.findModuleByName(facebookNative.module);
        if (!module) {
            logger(`[-] ${facebookNative.module} not loaded`);
            return;
        }

        const fn = Module.getExportByName(module.name, facebookNative.functionName);
        if (fn) {
            const offset = fn.sub(module.base);
            logger(`[*] Found Facebook cert verify function`);
            logger(`[*] Module base: ${module.base}`);
            logger(`[*] Function offset: 0x${offset.toString(16)}`);

            // Optional: display surrounding instructions
            const returnOffset = 0x8c7e28 - 0x8c7b44;
            const returnInstruction = fn.add(returnOffset);
            logger('[*] Inspecting return instruction region:');
            console.log(hexdump(returnInstruction.sub(16), { length: 32 }));

            Interceptor.attach(fn, {
                onLeave: function (retval) {
                    logger(`[*] Original return value: ${retval}`);
                    retval.replace(1); 
                    logger("[+] Return value changed to 1 — cert check bypassed");
                }
            });

            logger(`[+] Hooked native SSL verification in ${module.name}`);
        } else {
            logger(`[-] Function ${facebookNative.functionName} not found`);
        }
    } catch (err) {
        logger(`[-] Failed to hook Facebook native function: ${err}`);
    }
}


function waitForModuleAndHook(moduleName, callback) {
    const interval = setInterval(() => {
        const module = Process.findModuleByName(moduleName);
        if (module) {
            clearInterval(interval);
            logger(`[+] Module loaded: ${moduleName}`);
            callback();
        }
    }, 50);
}

function main() {
    waitForModuleAndHook(facebookNative.module, hookFacebookNativeCertVerification);
}

setImmediate(main);

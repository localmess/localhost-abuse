package com.localhost_abuse.xprofilepoc;

import java.util.Map;
import java.util.Objects;

public class BrowserDetector {
    public static String detectBrowser(Map<String, String> headers) {
        String secChUa = headers.get("sec-ch-ua");
        if (secChUa != null) {
            String lower = secChUa.toLowerCase();
            if (lower.contains("samsung internet")) return "Samsung Internet";
            if (lower.contains("yandex")) return "Yandex";
            if (lower.contains("opera")) return "Opera";
            if (lower.contains("microsoft edge")) return "Microsoft Edge";
            if (lower.contains("chromium")) return "Chromium";
            return secChUa.split(";")[0].replace("\"", "");
        }
        String ua = headers.get("user-agent");
        if (ua != null && ua.contains("/")) {
            String browser = ua.split("/")[0];
            if (Objects.equals(browser, "Mozilla")) return "Firefox";
            return browser;
        }
        return "Unknown";
    }
}

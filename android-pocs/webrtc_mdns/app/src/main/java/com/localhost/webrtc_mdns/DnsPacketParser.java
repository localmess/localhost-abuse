package com.localhost.webrtc_mdns;


import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class DnsPacketParser {
    private static final Pattern UUID_LOCAL =
            Pattern.compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.local$");

    // RFC 4648 base32 (no padding); input is lower-case in your PoC
    private static final String B32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static String firstLabel(String fqdn) {
        int dot = fqdn.indexOf('.');
        return (dot == -1) ? fqdn : fqdn.substring(0, dot);
    }

    private static String tryDecodeBase32Label(String label) {
        if (label == null || label.isEmpty()) return null;
        String s = label.trim().toUpperCase(java.util.Locale.US);

        // quick charset check: only A-Z and 2-7
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= '2' && c <= '7');
            if (!ok) return null;
        }

        int buffer = 0, bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length() * 5 / 8 + 1);

        for (int i = 0; i < s.length(); i++) {
            int val = B32_ALPHABET.indexOf(s.charAt(i));
            if (val < 0) return null;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            while (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        // ignore leftover bits; PoC deliberately truncates to ~35 chars
        try {
            return out.toString("US-ASCII");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }


    public static List<String> parse(byte[] data, int length) throws IOException {
        List<String> results = new ArrayList<>();
        // wrap+skip header
        ByteBuffer buf = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
        buf.position(4);
        int qdCount = buf.getShort() & 0xFFFF;
        int anCount = buf.getShort() & 0xFFFF;
        buf.getShort(); buf.getShort();
        int offset = 12;

        // skip questions (as before)…
        for (int i = 0; i < qdCount; i++) {
            NameOffset no = readName(data, offset);
            offset = no.offset + 4;
            String name = no.name;
            if (name.endsWith(".local")
                    && !name.endsWith("_tcp.local")
                    && !name.endsWith("_udp.local")) {
                results.add("Q: " + name);

                // NEW: decode first label as Base32(url prefix) for the PoC
                String label = firstLabel(name);
                String decoded = tryDecodeBase32Label(label);
                if (decoded != null && !decoded.isEmpty()) {
                    // Match your PoC narrative: it only packs the first ~35 chars
                    results.add("Q decoded (URL): " + decoded + "…");
                }
            }
        }



        // answers: now read TTL and skip TTL==0 or all-zero RDATA
        for (int i = 0; i < anCount; i++) {
            NameOffset no = readName(data, offset);
            String name = no.name;
            offset = no.offset;

            // parse type, class, ttl, rdlen
            int type  = ((data[offset]   & 0xFF) << 8) | (data[offset+1] & 0xFF);
            //int cls  = ((data[offset+2] & 0xFF) << 8) | (data[offset+3] & 0xFF);
            int ttl   = ((data[offset+4] & 0xFF) << 24)
                    | ((data[offset+5] & 0xFF) << 16)
                    | ((data[offset+6] & 0xFF) << 8)
                    |  (data[offset+7] & 0xFF);
            int rdlen = ((data[offset+8] & 0xFF) << 8) | (data[offset+9] & 0xFF);

            offset += 10;
            // only A-records, positive TTL, right suffix, and non-zero RDATA
            //                     && UUID_LOCAL.matcher(name).matches()
            if (type == 1
                    && ttl > 0
                    && name.endsWith(".local")
                    && !name.endsWith("_tcp.local")
                    && !name.endsWith("_udp.local")
                    && rdlen == 4) {

                byte[] ipBytes = Arrays.copyOfRange(data, offset, offset + 4);
                InetAddress addr = InetAddress.getByAddress(ipBytes);
                String host = addr.getHostAddress();
                if (!"0.0.0.0".equals(host)) {
                    results.add("A: " + name + " → " + host);
                    String label = firstLabel(name);                 // helper you added
                    String decoded = tryDecodeBase32Label(label);    // helper you added
                    if (decoded != null && !decoded.isEmpty()) {
                        results.add("A decoded (URL): " + decoded + "…");
                    }
                }
            }

            offset += rdlen;
        }
        return results;
    }

//    public static List<String> parse(byte[] data, int length) throws IOException {
//        List<String> results = new ArrayList<>();
//        ByteBuffer buf = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN);
//        buf.position(4); // skip ID and flags
//        int qdCount = buf.getShort() & 0xFFFF;
//        int anCount = buf.getShort() & 0xFFFF;
//        // skip NS and AR counts
//        buf.getShort(); buf.getShort();
//        int offset = 12;
//
//        // questions
//        for (int i = 0; i < qdCount; i++) {
//            NameOffset no = readName(data, offset);
//            String name = no.name;
//            offset = no.offset + 4; // skip type+class
//            if (name.endsWith(".local")
//                    && !name.endsWith("_tcp.local")
//                    && !name.endsWith("_udp.local")) {
//                results.add("Q: " + name);
//            }
//
//        }
//        // answers
//        for (int i = 0; i < anCount; i++) {
//            NameOffset no = readName(data, offset);
//            Log.d("MdnsParser", String.format(
//                    "Decoded name=`%s`, nextOffset=%d", no.name, no.offset));
//            String name = no.name;
//            offset = no.offset;
//            int type = ((data[offset] & 0xFF) << 8) | (data[offset+1] & 0xFF);
//            int rdlen = ((data[offset+8] & 0xFF) << 8) | (data[offset+9] & 0xFF);
//            offset += 10;
//            if (type == 1 && rdlen == 4 && name.endsWith(".local") && !name.endsWith("_tcp.local") && !name.endsWith("_udp.local")) {
//                byte[] ipBytes = Arrays.copyOfRange(data, offset, offset + 4);
//                InetAddress addr = InetAddress.getByAddress(ipBytes);
//                results.add("A: " + name + " → " + addr.getHostAddress());
//            }
//            offset += rdlen;
//        }
//        return results;
//    }

    private static NameOffset readName(byte[] data, int offset) {
        StringBuilder sb = new StringBuilder();
        int len = data[offset] & 0xFF;
        while (len > 0) {
            if ((len & 0xC0) == 0xC0) {
                int pointer = ((len & 0x3F) << 8) | (data[offset+1] & 0xFF);
                NameOffset no = readName(data, pointer);
                sb.append(no.name);
                offset += 2;
                return new NameOffset(sb.toString(), offset);
            } else {
                offset++;
                sb.append(new String(data, offset, len));
                offset += len;
                len = data[offset] & 0xFF;
                if (len > 0) sb.append('.');
            }
        }
        offset++;
        return new NameOffset(sb.toString(), offset);
    }

    private static class NameOffset {
        String name;
        int offset;
        NameOffset(String name, int offset) {
            this.name = name;
            this.offset = offset;
        }
    }
}

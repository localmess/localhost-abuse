// UdpServer.java
package com.localhost_abuse.stealthturn;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class UdpServer extends Thread {
    private static final String TAG               = "UdpServer";
    private static final int    PORT              = 12586;
    private static final int    TIMEOUT_MS        = 10_000;
    private static final int    STUN_BIND_REQ     = 0x0001;
    private static final int    STUN_BIND_RESP    = 0x0101;
    private static final int    TURN_ALLOC_REQ    = 0x0003;
    private static final int    TURN_ALLOC_RESP   = 0x0103;
    private static final int    TURN_ALLOC_ERROR  = 0x0113;     // 0x0003 | 0x0110
    private static final int    MAGIC_COOKIE      = 0x2112A442;
    private static final String REALM             = "com.facebook.aid";

    private final DatagramSocket socket;
    private volatile boolean      running;
    private final File            logFile;
    private final String          localIpAddress;
    private final ConcurrentHashMap<SocketAddress, String> nonceMap = new ConcurrentHashMap<>();

    public UdpServer(Context ctx) throws SocketException {
        this.socket         = new DatagramSocket(PORT);
        this.socket.setSoTimeout(TIMEOUT_MS);
        this.logFile        = new File(ctx.getFilesDir(), "udp_log.txt");
        this.localIpAddress = discoverLocalIp();
    }

    @Override
    public void run() {
        running = true;
        byte[] buffer = new byte[512];
        Log.d(TAG, "Listening on UDP " + localIpAddress + ":" + PORT);

        while (running) {
            DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(pkt);
                logRaw("IN", pkt);

                ByteBuffer buf = ByteBuffer.wrap(pkt.getData(), 0, pkt.getLength());
                short msgType  = buf.getShort();

                if (msgType == STUN_BIND_REQ) {
                    sendStunBindingResponse(pkt);
                } else if (msgType == TURN_ALLOC_REQ) {
                    handleTurnAllocate(pkt);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                Log.e(TAG, "Server error", e);
                break;
            }
        }
        socket.close();
    }

    public void stopServer() {
        running = false;
        socket.close();
    }

    public String getLocalIpAddress() { return localIpAddress; }
    public int    getPort()           { return PORT; }

    private void sendStunBindingResponse(DatagramPacket req) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(20);
        b.putShort((short) STUN_BIND_RESP);
        b.putShort((short) 0);
        b.putInt(MAGIC_COOKIE);
        b.put(req.getData(), 4, 12);  // transaction ID
        DatagramPacket resp = new DatagramPacket(b.array(), b.position(),
                req.getAddress(), req.getPort());
        socket.send(resp);
        logRaw("OUT", resp);
    }

    private void handleTurnAllocate(DatagramPacket req) throws IOException {
        SocketAddress client = req.getSocketAddress();
        String nonce        = nonceMap.get(client);

        byte[] txId = new byte[12];
        System.arraycopy(req.getData(), 8, txId, 0, 12);

        if (nonce != null && hasCredentials(req.getData(), req.getLength(), nonce)) {
            DatagramPacket success = buildTurnAllocateSuccess(txId,
                    req.getAddress(),
                    req.getPort());
            socket.send(success);
            logRaw("OUT", success);
            nonceMap.remove(client);

        } else {
            String newNonce = generateNonce();
            nonceMap.put(client, newNonce);
            DatagramPacket err = buildTurnAllocateError(txId,
                    req.getAddress(),
                    req.getPort(),
                    REALM, newNonce);
            socket.send(err);
            logRaw("OUT", err);
        }
    }

    private boolean hasCredentials(byte[] data, int len, String expectedNonce) {
        ByteBuffer attrs = ByteBuffer.wrap(data, 20, len - 20);
        boolean sawUser=false, sawRealm=false, sawNonce=false;
        while (attrs.remaining() >= 4) {
            short t = attrs.getShort(), l = attrs.getShort();
            if (t == 0x0006) { sawUser = true; }
            else if (t == 0x0014) {
                byte[] r = new byte[l]; attrs.get(r);
                if (new String(r, StandardCharsets.US_ASCII).equals(REALM))
                    sawRealm = true;
            }
            else if (t == 0x0015) {
                byte[] n = new byte[l]; attrs.get(n);
                if (new String(n, StandardCharsets.US_ASCII).equals(expectedNonce))
                    sawNonce = true;
            }
            attrs.position(attrs.position() + ((l + 3) & ~3) - l);
        }
        return sawUser && sawRealm && sawNonce;
    }

    private DatagramPacket buildTurnAllocateError(
            byte[] txId, InetAddress dest, int port,
            String realm, String nonce) {

        // 1) ERROR-CODE attr (0x0009): header(4) + reserved(2) + class(1)+number(1)+reason
        String reason = "Unauthenticated";
        int reasonLen = reason.length();
        int errBodyLen = 2 + 1 + 1 + reasonLen;      // reserved(2)+class(1)+num(1)+reason
        int errAttrLen = 4 + errBodyLen;             // header(4)+body
        int errPad    = ((errAttrLen + 3) / 4) * 4;
        ByteBuffer errB = ByteBuffer.allocate(errPad);
        // header
        errB.putShort((short)0x0009);
        errB.putShort((short)errBodyLen);
        // value
        errB.putShort((short)0);               // reserved
        errB.put((byte)4);                     // class
        errB.put((byte)1);                     // number
        errB.put(reason.getBytes(StandardCharsets.US_ASCII));
        while (errB.position() < errPad) errB.put((byte)0);

        // 2) REALM attr (0x0014)
        int realmLen = realm.length();
        int realmAttrLen = 4 + realmLen;
        int realmPad     = ((realmAttrLen + 3) / 4) * 4;
        ByteBuffer realmB = ByteBuffer.allocate(realmPad);
        realmB.putShort((short)0x0014);
        realmB.putShort((short)realmLen);
        realmB.put(realm.getBytes(StandardCharsets.US_ASCII));
        while (realmB.position() < realmPad) realmB.put((byte)0);

        // 3) NONCE attr (0x0015)
        int nonceLen = nonce.length();
        int nonceAttrLen = 4 + nonceLen;
        int noncePad     = ((nonceAttrLen + 3) / 4) * 4;
        ByteBuffer nonceB = ByteBuffer.allocate(noncePad);
        nonceB.putShort((short)0x0015);
        nonceB.putShort((short)nonceLen);
        nonceB.put(nonce.getBytes(StandardCharsets.US_ASCII));
        while (nonceB.position() < noncePad) nonceB.put((byte)0);

        byte[] errBytes   = errB.array();
        byte[] realmBytes = realmB.array();
        byte[] nonceBytes = nonceB.array();
        int totalLen = errBytes.length + realmBytes.length + nonceBytes.length;

        ByteBuffer b = ByteBuffer.allocate(20 + totalLen);
        b.putShort((short) TURN_ALLOC_ERROR);
        b.putShort((short) totalLen);
        b.putInt(MAGIC_COOKIE);
        b.put(txId);
        b.put(errBytes);
        b.put(realmBytes);
        b.put(nonceBytes);

        return new DatagramPacket(b.array(), b.position(), dest, port);
    }

    private DatagramPacket buildTurnAllocateSuccess(
            byte[] txId, InetAddress dest, int port) {

        ByteBuffer a = ByteBuffer.allocate(12);
        a.putShort((short)0x0016);
        a.putShort((short)8);
        a.put((byte)0);
        a.put((byte)1);
        short xPort = (short)(port ^ (MAGIC_COOKIE >>> 16));
        a.putShort(xPort);
        byte[] addr = dest.getAddress();
        byte[] mc   = ByteBuffer.allocate(4).putInt(MAGIC_COOKIE).array();
        for (int i = 0; i < 4; i++) {
            a.put((byte)(addr[i] ^ mc[i]));
        }

        byte[] attrs = a.array();
        ByteBuffer b = ByteBuffer.allocate(20 + attrs.length);
        b.putShort((short) TURN_ALLOC_RESP);
        b.putShort((short) attrs.length);
        b.putInt(MAGIC_COOKIE);
        b.put(txId);
        b.put(attrs);

        return new DatagramPacket(b.array(), b.position(), dest, port);
    }

    private String generateNonce() {
        byte[] rnd = new byte[12];
        new SecureRandom().nextBytes(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
    }

    private void logRaw(String dir, DatagramPacket pkt) {
        byte[] data = pkt.getData();
        int len      = pkt.getLength();
        String ts    = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
        String hdr   = String.format("%s | %s %s:%d → %s:%d",
                ts, dir,
                dir.equals("IN")  ? pkt.getAddress().getHostAddress() : localIpAddress,
                dir.equals("IN")  ? pkt.getPort()                : PORT,
                dir.equals("OUT") ? pkt.getAddress().getHostAddress() : localIpAddress,
                dir.equals("OUT") ? pkt.getPort()                : PORT);
        String hex   = bytesToHex(data, len);
        String ascii = new String(data, 0, len, StandardCharsets.US_ASCII)
                .replaceAll("\\p{C}", ".");

        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(hdr + "\nHEX:   " + hex + "\nASCII: " + ascii + "\n\n");
        } catch (IOException e) {
            Log.e(TAG, "log-write failed", e);
        }
        Log.d(TAG, hdr);
        try {
            MainActivity.appendLog(hdr);
            MainActivity.appendLog("ASCII: " + ascii);
        } catch (Exception ignored) {}
    }

    private static String bytesToHex(byte[] b, int len) {
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X", b[i]));
        }
        return sb.toString();
    }

    private String discoverLocalIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress ia : Collections.list(ni.getInetAddresses())) {
                    if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                        return ia.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "IP discovery failed", e);
        }
        return "0.0.0.0";
    }
}

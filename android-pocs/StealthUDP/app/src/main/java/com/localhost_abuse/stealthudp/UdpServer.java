package com.localhost_abuse.stealthudp;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Objects;

public class UdpServer extends Thread {
    private static final String TAG = "UdpServer";
    private static final int FIXED_PORT = 12580;
    private DatagramSocket socket;
    private boolean running;
    private final File logFile;
    private final String localIpAddress;

    public UdpServer(Context context) {
        this.logFile = new File(context.getFilesDir(), "udp_log.txt");
        this.localIpAddress = getLocalIpAddress();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(FIXED_PORT);
            running = true;
            byte[] buffer = new byte[4096];

            Log.d(TAG, "Server started on " + localIpAddress + ":" + FIXED_PORT);

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                String senderIp = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();

                logMessage(message, senderIp, senderPort);
            }
        } catch (BindException e) {
            Log.e(TAG, "Port " + FIXED_PORT + " is already in use.", e);
        } catch (IOException e) {
            Log.e(TAG, "UDP Server error", e);
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stopServer() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void logMessage(String message, String senderIp, int senderPort) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = timestamp + " | From: " + senderIp + ":" + senderPort + " | Message: " + message;

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logEntry + "\n");
            writer.flush();
            Log.d(TAG, logEntry);

            MainActivity.appendLog(logEntry);

        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    public String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface intf : Collections.list(interfaces)) {
                Enumeration<InetAddress> addresses = intf.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (!addr.isLoopbackAddress() && Objects.requireNonNull(addr.getHostAddress()).indexOf(':') == -1) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting local IP address", e);
        }
        return "Unknown";
    }

    public int getPort() {
        return FIXED_PORT;
    }
}

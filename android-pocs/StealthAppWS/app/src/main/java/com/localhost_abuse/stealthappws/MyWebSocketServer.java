package com.localhost_abuse.stealthappws;

import android.content.Context;
import android.util.Log;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

public class MyWebSocketServer extends WebSocketServer {

    private static final String TAG = "MyWebSocketServer";
    private final Set<WebSocket> connections = Collections.synchronizedSet(new HashSet<>());
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public MyWebSocketServer(int port, Context context) {
        super(new InetSocketAddress("0.0.0.0", port));
        logFile = new File(context.getFilesDir(), "server_logs.txt");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String origin = handshake.getFieldValue("Origin");
        String logEntry = getTimestamp() + " - New connection from: " + conn.getRemoteSocketAddress() + ", Origin: " + origin + "\n";
        writeLogToFile(logEntry);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String logEntry = getTimestamp() + " - Connection closed: " + conn.getRemoteSocketAddress() + ", Reason: " + reason + "\n";
        writeLogToFile(logEntry);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        String logEntry = getTimestamp() + " - Received message from " + conn.getRemoteSocketAddress() + ": " + message + "\n";
        writeLogToFile(logEntry);
        conn.send("200");
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        String logEntry = getTimestamp() + " - Received binary message: " + message.remaining() + " bytes from " + conn.getRemoteSocketAddress() + "\n";
        writeLogToFile(logEntry);
        conn.send("200");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        String logEntry = getTimestamp() + " - WebSocket error: " + ex.getMessage() + "\n";
        writeLogToFile(logEntry);
    }

    @Override
    public void onStart() {
        String logEntry = getTimestamp() + " - WebSocket server started on " + getAddress() + "\n";
        writeLogToFile(logEntry);
    }

    private void writeLogToFile(String logEntry) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(logEntry);
            Log.d(TAG, logEntry);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }

    private String getTimestamp() {
        return dateFormat.format(new Date());
    }
}

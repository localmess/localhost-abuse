package com.localhost_abuse.stealthappws;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class WebServerService extends Service {

    private static final String TAG = "WebServerService";
    private static final int PORT_START = 12380;
    private static final int PORT_END = 12387;
    private final List<MyWebSocketServer> servers = new ArrayList<>();
    private static final String CHANNEL_ID = "StealthAppChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        startWebSocketServers();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stealth WebSocket Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WebSocket Server Running")
                .setContentText("Listening on localhost:12380-12387")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void startWebSocketServers() {
        for (int port = PORT_START; port <= PORT_END; port++) {
            try {
                MyWebSocketServer server = new MyWebSocketServer(port, getApplicationContext());
                server.start();
                servers.add(server);
                Log.d(TAG, "Started WebSocket server on port: " + port);
            } catch (Exception e) {
                Log.e(TAG, "Error starting WebSocket server on port " + port, e);
            }
        }
    }

    private void stopWebSocketServers() throws InterruptedException {
        for (MyWebSocketServer server : servers) {
            server.stop();
        }
        servers.clear();
        Log.d(TAG, "Stopped all WebSocket servers.");
    }

    @Override
    public void onDestroy() {
        try {
            stopWebSocketServers();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

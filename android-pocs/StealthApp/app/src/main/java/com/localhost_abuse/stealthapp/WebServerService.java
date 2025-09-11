package com.localhost_abuse.stealthapp;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebServerService extends Service {

    private static final String TAG = "WebServerService";
    public static final int PORT_START = 13380;
    public static final int PORT_END = 13387;
    private final List<MyWebServer> servers = new ArrayList<>();
    private static final String CHANNEL_ID = "StealthAppChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        startWebServers();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Stealth Web Server",
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
                .setContentTitle("Web Server Running")
                .setContentText("Stealth web servers are running in the background.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void startWebServers() {
        for (int port = PORT_START; port <= PORT_END; port++) {
            try {
                MyWebServer server = new MyWebServer(getApplicationContext(), port);
                server.start();
                servers.add(server);
                Log.d(TAG, "Started server on port: " + port);
            } catch (IOException e) {
                Log.e(TAG, "Error starting server on port " + port, e);
            }
        }
    }

    private void stopWebServers() {
        for (MyWebServer server : servers) {
            server.stop();
        }
        servers.clear();
        Log.d(TAG, "Stopped all web servers.");
    }

    @Override
    public void onDestroy() {
        stopWebServers();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

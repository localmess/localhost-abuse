package com.localhost_abuse.xprofilepoc;

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
import java.util.List;

public class WebServerService extends Service {

    private static final String TAG = "WebServerService";
    private static final List<Integer> PORTS = Config.PORTS;
    private final List<MyWebServer> servers = new java.util.ArrayList<>();
    private static final String CHANNEL_ID = "StealthAppChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        startWebServers();
        Log.d(TAG, "Web server service started.");
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
        for (int port : PORTS) {
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

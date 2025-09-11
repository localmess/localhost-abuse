// UdpService.java
package com.localhost_abuse.stealthturn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class UdpService extends Service {
    private static final String CHANNEL_ID = "UDPServiceChannel";
    private final IBinder binder = new LocalBinder();
    private UdpServer udpServer;

    public class LocalBinder extends Binder {
        UdpService getService() {
            return UdpService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());

        new Thread(() -> {
            try {
                udpServer = new UdpServer(UdpService.this);
                udpServer.start();
            } catch (Exception e) {
                Log.e("UdpService", "Failed to start UdpServer", e);
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (udpServer != null) {
            udpServer.stopServer();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public String getServerIp() {
        return udpServer != null ? udpServer.getLocalIpAddress() : "Unknown";
    }

    public int getServerPort() {
        return udpServer != null ? udpServer.getPort() : 0;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "UDP Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UDP Server Running")
                .setContentText("Listening for UDP messages")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }
}

package com.localhost_abuse.stealthudp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private TextView textViewInfo;
    private TextView textViewLogs;
    private ScrollView scrollView;
    private UdpService udpService;
    private boolean bound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static WeakReference<MainActivity> instanceRef;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UdpService.LocalBinder binder = (UdpService.LocalBinder) service;
            udpService = binder.getService();
            bound = true;
            updateServerInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instanceRef = new WeakReference<>(this);

        scrollView = findViewById(R.id.scrollView);
        textViewInfo = findViewById(R.id.textViewInfo);
        textViewLogs = findViewById(R.id.textViewLogs);

        Intent serviceIntent = new Intent(this, UdpService.class);
        startForegroundService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateServerInfo() {
        handler.postDelayed(() -> {
            if (bound && udpService != null) {
                String ipAddress = udpService.getServerIp();
                int port = udpService.getServerPort();
                textViewInfo.setText("UDP Server running at:\n" + ipAddress + ":" + port);
            } else {
                updateServerInfo();
            }
        }, 500);
    }

    public static void appendLog(String logEntry) {
        MainActivity instance = instanceRef.get();
        if (instance != null) {
            instance.runOnUiThread(() -> {
                instance.textViewLogs.append(logEntry + "\n");
                instance.scrollView.post(() -> instance.scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        instanceRef.clear();
    }
}

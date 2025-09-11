package com.localhost_abuse.yandexpoc;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView serverInfoTextView;
    private TextView logTextView;
    private ScrollView scrollView;
    private static WeakReference<MainActivity> instanceRef;
    private static final List<Integer> PORTS = Arrays.asList(29009, 30102, 29010, 30103);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        instanceRef = new WeakReference<>(this);

        scrollView = findViewById(R.id.scrollView);
        serverInfoTextView = findViewById(R.id.serverInfoTextView);
        logTextView = findViewById(R.id.textView);

        displayServerAddresses();
        RequestLogger.getInstance().getLogLiveData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String logEntry) {
                appendLog(logEntry);
            }
        });

        findViewById(R.id.buttonHistory).setOnClickListener(v->
                startActivity(new Intent(this,HistoryActivity.class))
        );
        startService(new Intent(this, WebServerService.class));
    }

    private void displayServerAddresses() {
        String internalIp = getDeviceInternalIp();
        StringBuilder displayText = new StringBuilder("Web Servers are running:\n");
        for (int port : PORTS) {
            displayText.append("IP: ").append(internalIp).append(":").append(port).append("\n");
        }
        serverInfoTextView.setText(displayText.toString());
    }

    private String getDeviceInternalIp() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Unavailable";
    }

    private void appendLog(String logEntry) {
        runOnUiThread(() -> {
            logTextView.append("\n" + logEntry + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, WebServerService.class));
        instanceRef.clear();
    }

    public static MainActivity getInstance() {
        return instanceRef.get();
    }
}

package com.localhost.webrtc_mdns;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_NEARBY_WIFI = 1001;
    private RecyclerView recyclerView;
    private EntryAdapter adapter;
    private List<String> entries = new ArrayList<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String timestamp = intent.getStringExtra("timestamp");
            String message = intent.getStringExtra("message");
            entries.add(timestamp + "  " + message);
            adapter.notifyItemInserted(entries.size() - 1);
            recyclerView.scrollToPosition(entries.size() - 1);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int ANDROID_16 = 36;
        if (Build.VERSION.SDK_INT >= ANDROID_16) { // API 34
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ "android.permission.NEARBY_WIFI_DEVICES" },
                        REQ_NEARBY_WIFI
                );
            }
        }

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new EntryAdapter(entries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MdnsSnifferService.class);
                ContextCompat.startForegroundService(MainActivity.this, intent);

                Toast.makeText(MainActivity.this,
                                "mDNS sniffer started",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MdnsSnifferService.class);
                stopService(intent);

                Toast.makeText(MainActivity.this,
                                "mDNS sniffer stopped",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        Button btnClear = findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                entries.clear();
                adapter.notifyDataSetChanged();

                Toast.makeText(MainActivity.this,
                                "Log cleared",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQ_NEARBY_WIFI) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // OK—mDNS sniffing will now work under Android 16 restrict mode
            } else {
                // Inform them they need to enable “Nearby devices” in Settings
                new AlertDialog.Builder(this)
                        .setMessage("mDNS sniffing requires Nearby-devices permission; please grant it in Settings.")
                        .setPositiveButton("Open Settings", (d, i) -> {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        })
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {

        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter("MDNS_EVENT"));
        Intent sync = new Intent(this, MdnsSnifferService.class)
                .setAction(MdnsSnifferService.ACTION_SYNC);
        startService(sync);
//        super.onResume();
//        LocalBroadcastManager.getInstance(this)
//                .registerReceiver(receiver, new IntentFilter("MDNS_EVENT"));
//
//        // ask the Service to replay anything buffered
//        Intent sync = new Intent(this, MdnsSnifferService.class)
//                .setAction(MdnsSnifferService.ACTION_SYNC);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            ContextCompat.startForegroundService(this, sync);
//        } else {
//            startService(sync);
//        }
    }


    @Override protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(receiver);
    }
}
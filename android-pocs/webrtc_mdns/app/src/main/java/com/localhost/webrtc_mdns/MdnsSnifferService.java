package com.localhost.webrtc_mdns;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.content.pm.ServiceInfo;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MdnsSnifferService extends Service {
    private WifiManager.MulticastLock multicastLock;
    private MulticastSocket socket;
    private boolean running = false;
    private Thread sniffThread;
    private static final String CHANNEL_ID = "mdns_sniffer_channel";

    public static final String ACTION_SYNC = "SYNC_ENTRIES";
    private final List<Intent> bufferedBroadcasts = new ArrayList<>();

    @Override public void onCreate() {
        super.onCreate();
        WifiManager wifi = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifi.createMulticastLock("mdnsLock");
        multicastLock.setReferenceCounted(true);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 1) Handle sync requests by replaying buffered entries
        if (intent != null && ACTION_SYNC.equals(intent.getAction())) {
            // replay…
            for (Intent b : bufferedBroadcasts) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(b);
            }
            // …then clear, so next resume only gets new events
            bufferedBroadcasts.clear();
            return START_STICKY;
        }

        // 2) Normal startup: create channel & notification
        createNotificationChannel();
        Notification notification = buildNotification();

        // 3) Start as a foreground service with connectedDevice type on Q+
        startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        );

        // 4) Acquire multicast lock and start sniffing
        multicastLock.acquire();
        running = true;
        sniffThread = new Thread(this::sniffLoop);
        sniffThread.start();

        return START_STICKY;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "mDNS Sniffer", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Sniffs mDNS traffic in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("mDNS Sniffer")
                .setContentText("Sniffing mDNS traffic")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void sniffLoop() {
        try {
            socket = new MulticastSocket(5353);

            // Pick an 'up', non-loopback, multicast-capable interface with IPv4
            java.net.NetworkInterface chosen = null;
            java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                java.net.NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                boolean hasV4 = false;
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement() instanceof java.net.Inet4Address) { hasV4 = true; break; }
                }
                if (hasV4) { chosen = ni; break; }
            }
            if (chosen == null) {
                Log.w("MdnsSniffer", "No suitable IPv4 interface for mDNS");
                return;
            }

            InetAddress group = InetAddress.getByName("224.0.0.251");
            socket.joinGroup(new java.net.InetSocketAddress(group, 5353), chosen);

            byte[] buf = new byte[2048];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                try {
                    List<String> results = DnsPacketParser.parse(packet.getData(), packet.getLength());
                    for (String res : results) {
                        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                        Log.d("MdnsSniffer", time + "  " + res);

                        Intent broadcast = new Intent("MDNS_EVENT")
                                .putExtra("timestamp", time)
                                .putExtra("message", res);

                        bufferedBroadcasts.add(new Intent(broadcast));
                        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
                    }
                } catch (Throwable t) {
                    Log.e("MdnsSniffer", "Parse error", t); // don't let the thread die
                }
            }
        } catch (IOException e) {
            Log.e("MdnsSniffer", "IPv4 loop error", e);
        }
    }


//    private void sniffLoop() {
//        try {
//            socket = new MulticastSocket(5353);
//            InetAddress group = InetAddress.getByName("224.0.0.251");
//            socket.joinGroup(group);
//            byte[] buf = new byte[2048];
//            while (running) {
//                DatagramPacket packet = new DatagramPacket(buf, buf.length);
//                socket.receive(packet);
//                List<String> results = DnsPacketParser.parse(packet.getData(), packet.getLength());
//                for (String res : results) {
//                    String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
//                            .format(new Date());
//                    Log.d("MdnsSniffer", time + "  " + res);
//                    Intent broadcast = new Intent("MDNS_EVENT")
//                            .putExtra("timestamp", time)
//                            .putExtra("message", res);
////                    broadcast.putExtra("timestamp", time);
////                    broadcast.putExtra("message", res);
////                    sendBroadcast(broadcast);
//                    bufferedBroadcasts.add(new Intent(broadcast));
//
//                    LocalBroadcastManager.getInstance(this)
//                            .sendBroadcast(broadcast);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override public void onDestroy() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            try {
                socket.leaveGroup(InetAddress.getByName("224.0.0.251"));
            } catch (IOException ignored) {}
            socket.close();
        }
        if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
package com.anlinux.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.anlinux.app.R;
import com.anlinux.app.ui.MainActivity;
import com.anlinux.app.ui.terminal.TerminalActivity;

public class LinuxService extends Service {

    public static final String ACTION_START = "com.anlinux.app.START_SERVICE";
    public static final String ACTION_STOP = "com.anlinux.app.STOP_SERVICE";
    public static final String EXTRA_DISTRO_NAME = "extra_distro_name";
    public static final String EXTRA_DISTRO_ID = "extra_distro_id";

    private static final String CHANNEL_ID = "anlinux_service_channel";
    private static final int NOTIF_ID = 101;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            }

            String distroName = intent.getStringExtra(EXTRA_DISTRO_NAME);
            long distroId = intent.getLongExtra(EXTRA_DISTRO_ID, -1);
            if (distroName == null) distroName = "Linux Environment";

            acquireWakeLock();
            Notification notification = buildNotification(distroName, distroId);
            startForeground(NOTIF_ID, notification);
        }
        return START_STICKY;
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AnLinux::RunningWakeLock");
                wakeLock.acquire(24 * 60 * 60 * 1000L); // Max 24 hours
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private Notification buildNotification(String distroName, long distroId) {
        Intent openIntent = new Intent(this, TerminalActivity.class);
        openIntent.putExtra("distro_id", distroId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, LinuxService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(distroName + " Aktif")
                .setContentText("Sistem Linux sedang berjalan di latar belakang.")
                .setSmallIcon(R.drawable.ic_terminal)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_terminal, "Terminal", pendingIntent)
                .addAction(R.drawable.ic_stop, "Hentikan", stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Layanan Linux Aktif",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifikasi saat sesi Linux sedang berjalan.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

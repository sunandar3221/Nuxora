package com.nuxora.app.service;

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

import com.nuxora.app.R;
import com.nuxora.app.ui.terminal.TerminalActivity;

public class LinuxService extends Service {

    public static final String ACTION_START_TERMINAL = "com.nuxora.app.START_TERMINAL";
    public static final String ACTION_STOP_TERMINAL = "com.nuxora.app.STOP_TERMINAL";
    public static final String ACTION_START = ACTION_START_TERMINAL;
    public static final String ACTION_STOP = ACTION_STOP_TERMINAL;
    public static final String ACTION_START_INSTALL = "com.nuxora.app.START_INSTALL";
    public static final String ACTION_UPDATE_INSTALL = "com.nuxora.app.UPDATE_INSTALL";
    public static final String ACTION_STOP_INSTALL = "com.nuxora.app.STOP_INSTALL";

    public static final String EXTRA_DISTRO_NAME = "extra_distro_name";
    public static final String EXTRA_DISTRO_ID = "extra_distro_id";
    public static final String EXTRA_PROGRESS_TEXT = "extra_progress_text";
    public static final String EXTRA_PROGRESS_PERCENT = "extra_progress_percent";
    public static final String EXTRA_IS_INDETERMINATE = "extra_is_indeterminate";

    private static final String CHANNEL_ID = "nuxora_service_channel";
    private static final int NOTIF_ID_TERMINAL = 101;
    private static final int NOTIF_ID_INSTALL = 102;

    private PowerManager.WakeLock wakeLock;
    private boolean isTerminalActive = false;
    private boolean isInstallActive = false;

    private NotificationManager notificationManager;

    public static void startTerminal(Context context, long distroId, String distroName) {
        Intent intent = new Intent(context, LinuxService.class);
        intent.setAction(ACTION_START_TERMINAL);
        intent.putExtra(EXTRA_DISTRO_ID, distroId);
        intent.putExtra(EXTRA_DISTRO_NAME, distroName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopTerminal(Context context) {
        Intent intent = new Intent(context, LinuxService.class);
        intent.setAction(ACTION_STOP_TERMINAL);
        context.startService(intent);
    }

    public static void startInstall(Context context, String distroName) {
        Intent intent = new Intent(context, LinuxService.class);
        intent.setAction(ACTION_START_INSTALL);
        intent.putExtra(EXTRA_DISTRO_NAME, distroName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void updateInstallProgress(Context context, String distroName, String statusText, int percent, boolean isIndeterminate) {
        Intent intent = new Intent(context, LinuxService.class);
        intent.setAction(ACTION_UPDATE_INSTALL);
        intent.putExtra(EXTRA_DISTRO_NAME, distroName);
        intent.putExtra(EXTRA_PROGRESS_TEXT, statusText);
        intent.putExtra(EXTRA_PROGRESS_PERCENT, percent);
        intent.putExtra(EXTRA_IS_INDETERMINATE, isIndeterminate);
        context.startService(intent);
    }

    public static void stopInstall(Context context) {
        Intent intent = new Intent(context, LinuxService.class);
        intent.setAction(ACTION_STOP_INSTALL);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case ACTION_START_TERMINAL: {
                long distroId = intent.getLongExtra(EXTRA_DISTRO_ID, -1);
                String distroName = intent.getStringExtra(EXTRA_DISTRO_NAME);
                if (distroName == null) distroName = "Linux Environment";

                isTerminalActive = true;
                acquireWakeLock();
                Notification notif = buildTerminalNotification(distroName, distroId);
                startForeground(NOTIF_ID_TERMINAL, notif);
                break;
            }

            case ACTION_STOP_TERMINAL: {
                isTerminalActive = false;
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIF_ID_TERMINAL);
                }
                checkStopSelf();
                break;
            }

            case ACTION_START_INSTALL: {
                String distroName = intent.getStringExtra(EXTRA_DISTRO_NAME);
                if (distroName == null) distroName = "Distro Linux";

                isInstallActive = true;
                acquireWakeLock();
                Notification notif = buildInstallNotification(distroName, "Menyiapkan instalasi...", 0, true);
                if (!isTerminalActive) {
                    startForeground(NOTIF_ID_INSTALL, notif);
                } else {
                    if (notificationManager != null) {
                        notificationManager.notify(NOTIF_ID_INSTALL, notif);
                    }
                }
                break;
            }

            case ACTION_UPDATE_INSTALL: {
                if (isInstallActive) {
                    String distroName = intent.getStringExtra(EXTRA_DISTRO_NAME);
                    if (distroName == null) distroName = "Distro Linux";
                    String statusText = intent.getStringExtra(EXTRA_PROGRESS_TEXT);
                    if (statusText == null) statusText = "Sedang memproses...";
                    int percent = intent.getIntExtra(EXTRA_PROGRESS_PERCENT, 0);
                    boolean isIndeterminate = intent.getBooleanExtra(EXTRA_IS_INDETERMINATE, false);

                    Notification notif = buildInstallNotification(distroName, statusText, percent, isIndeterminate);
                    if (notificationManager != null) {
                        notificationManager.notify(NOTIF_ID_INSTALL, notif);
                    }
                }
                break;
            }

            case ACTION_STOP_INSTALL: {
                isInstallActive = false;
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIF_ID_INSTALL);
                }
                checkStopSelf();
                break;
            }
        }

        return START_STICKY;
    }

    private void checkStopSelf() {
        if (!isTerminalActive && !isInstallActive) {
            releaseWakeLock();
            stopForeground(true);
            stopSelf();
        }
    }

    private synchronized void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nuxora::RunningWakeLock");
                wakeLock.acquire(24 * 60 * 60 * 1000L); // Max 24 hours
            }
        }
    }

    private synchronized void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private Notification buildTerminalNotification(String distroName, long distroId) {
        Intent openIntent = new Intent(this, TerminalActivity.class);
        openIntent.putExtra(TerminalActivity.EXTRA_DISTRO_ID, distroId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Intent stopIntent = new Intent(this, LinuxService.class);
        stopIntent.setAction(ACTION_STOP_TERMINAL);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nuxora - Sesi Terminal Aktif")
                .setContentText(distroName + " sedang berjalan di latar belakang.")
                .setSmallIcon(R.drawable.ic_terminal)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_terminal, "Buka Terminal", pendingIntent)
                .addAction(R.drawable.ic_stop, "Hentikan", stopPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private Notification buildInstallNotification(String distroName, String statusText, int percent, boolean isIndeterminate) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nuxora - Menginstal " + distroName)
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_terminal)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (isIndeterminate) {
            builder.setProgress(0, 0, true);
        } else {
            builder.setProgress(100, Math.max(0, Math.min(100, percent)), false);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Layanan Nuxora",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notifikasi aktif saat sesi Linux atau instalasi sedang berjalan.");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
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

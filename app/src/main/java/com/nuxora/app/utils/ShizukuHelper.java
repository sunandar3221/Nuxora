package com.nuxora.app.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.nuxora.app.NuxoraApplication;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShizukuHelper {

    private static final String TAG = "ShizukuHelper";
    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 2001;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface OperationCallback {
        void onComplete(boolean success, String message);
    }

    public static boolean isShizukuInstalledAndRunning() {
        try {
            return NuxoraApplication.isShizukuConnected() || Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasShizukuPermission() {
        if (!isShizukuInstalledAndRunning()) {
            return false;
        }
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestShizukuPermission() {
        try {
            if (isShizukuInstalledAndRunning()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Executes shell command via Shizuku ADB / root context.
     */
    public static Process execCommand(String cmd) throws Exception {
        Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
        method.setAccessible(true);
        return (Process) method.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
    }

    /**
     * Executes shell command via Shizuku and captures stdout
     */
    public static String execCommandWithOutput(String cmd) {
        try {
            Process process = execCommand(cmd);
            if (process == null) return "";
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "execCommandWithOutput error: " + e.getMessage());
            return "";
        }
    }

    /**
     * Grant MANAGE_EXTERNAL_STORAGE, lift Scoped Storage isolation, and grant permissions asynchronously.
     */
    public static void grantStoragePermissionsViaShizukuAsync(Context context, OperationCallback callback) {
        EXECUTOR.execute(() -> {
            boolean success = grantStoragePermissionsViaShizuku(context);
            if (callback != null) {
                callback.onComplete(success, success ?
                        "Izin penyimpanan dan Scoped Storage bypass berhasil diterapkan via Shizuku!" :
                        "Gagal menerapkan izin via Shizuku.");
            }
        });
    }

    /**
     * Grant MANAGE_EXTERNAL_STORAGE and lift Scoped Storage isolation on Android/data.
     */
    public static boolean grantStoragePermissionsViaShizuku(Context context) {
        if (!hasShizukuPermission()) {
            return false;
        }
        String pkg = context.getPackageName();
        try {
            String[] commands = new String[]{
                // 1. Grant system permissions
                "pm grant " + pkg + " android.permission.READ_EXTERNAL_STORAGE",
                "pm grant " + pkg + " android.permission.WRITE_EXTERNAL_STORAGE",
                "pm grant " + pkg + " android.permission.MANAGE_EXTERNAL_STORAGE",

                // 2. Grant AppOps for All Files Access
                "appops set " + pkg + " MANAGE_EXTERNAL_STORAGE allow",
                "appops set " + pkg + " android:manage_external_storage allow",
                "cmd appops set " + pkg + " MANAGE_EXTERNAL_STORAGE allow",

                // 3. Lift Scoped Storage isolation on Android/data & Android/obb (Android 11 & 12)
                "appops set " + pkg + " NO_ISOLATED_STORAGE allow",
                "appops set " + pkg + " android:no_isolated_storage allow",
                "cmd appops set " + pkg + " android:no_isolated_storage allow",
                "appops set " + pkg + " LEGACY_STORAGE allow",
                "appops set " + pkg + " android:legacy_storage allow",
                "cmd appops set " + pkg + " android:legacy_storage allow",
                "appops set " + pkg + " ACCESS_RESTRICTED_PERMISSIONS allow",
                "cmd appops set " + pkg + " ACCESS_RESTRICTED_PERMISSIONS allow",

                // 4. Try root fix if Shizuku runs with root UID
                "chmod -R 777 /data/media/0/Android/data 2>/dev/null || true",
                "chmod -R 777 /data/media/0/Android/obb 2>/dev/null || true",
                "chmod 755 /data/data/" + pkg + " 2>/dev/null || true"
            };

            for (String cmd : commands) {
                try {
                    Process process = execCommand(cmd);
                    if (process != null) {
                        process.waitFor();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Command failed: " + cmd + " (" + e.getMessage() + ")");
                }
            }
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "grantStoragePermissionsViaShizuku error", t);
            return false;
        }
    }
}

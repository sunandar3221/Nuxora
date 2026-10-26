package com.nuxora.app.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import com.nuxora.app.NuxoraApplication;

import rikka.shizuku.Shizuku;

import java.lang.reflect.Method;

public class ShizukuHelper {

    public static final int SHIZUKU_PERMISSION_REQUEST_CODE = 2001;

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
     * Grant MANAGE_EXTERNAL_STORAGE and storage permissions via Shizuku ADB command.
     */
    public static boolean grantStoragePermissionsViaShizuku(Context context) {
        if (!hasShizukuPermission()) {
            return false;
        }
        String pkg = context.getPackageName();
        try {
            String[] commands = new String[]{
                "appops set " + pkg + " MANAGE_EXTERNAL_STORAGE allow",
                "pm grant " + pkg + " android.permission.READ_EXTERNAL_STORAGE",
                "pm grant " + pkg + " android.permission.WRITE_EXTERNAL_STORAGE"
            };

            for (String cmd : commands) {
                Process process = execCommand(cmd);
                if (process != null) {
                    process.waitFor();
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

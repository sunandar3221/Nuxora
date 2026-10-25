package com.anlinux.app.utils;

import android.os.Build;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;

public class DeviceUtils {

    /**
     * Get normalized architecture string (aarch64, armhf, x86_64, x86)
     */
    public static String getNormalizedArch() {
        String[] supportedAbis = Build.SUPPORTED_ABIS;
        if (supportedAbis != null && supportedAbis.length > 0) {
            String primary = supportedAbis[0].toLowerCase(Locale.ROOT);
            if (primary.contains("arm64") || primary.contains("aarch64")) {
                return "aarch64";
            } else if (primary.contains("arm")) {
                return "armhf";
            } else if (primary.contains("x86_64") || primary.contains("amd64")) {
                return "x86_64";
            } else if (primary.contains("x86") || primary.contains("i686") || primary.contains("i386")) {
                return "i686";
            }
        }
        return "aarch64"; // Default fallback
    }

    /**
     * Check if root access (su binary) is available on this Android device
     */
    public static boolean isRootAvailable() {
        String[] paths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return testSuExecution();
            }
        }
        return testSuExecution();
    }

    private static boolean testSuExecution() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[128];
                int read = in.read(buffer);
                if (read > 0) {
                    String output = new String(buffer, 0, read);
                    return output.contains("uid=0");
                }
            }
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * Format byte count into human-readable string (MB, GB)
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}

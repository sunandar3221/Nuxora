package com.nuxora.app.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StoragePermissionHelper {

    public static final int REQUEST_CODE_STORAGE = 1002;

    public static boolean hasStoragePermission(Context context) {
        if (context == null) return false;

        // 1. Check MANAGE_EXTERNAL_STORAGE on Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (Environment.isExternalStorageManager()) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Check standard READ/WRITE storage permissions (works for targetSdk 28 on all Android versions)
        boolean hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (hasRead || hasWrite) {
            return true;
        }

        // 3. Fallback: Direct filesystem readability test
        try {
            java.io.File sdcard = new java.io.File("/sdcard");
            if (sdcard.exists() && sdcard.canRead()) {
                return true;
            }
            java.io.File emulated = new java.io.File("/storage/emulated/0");
            if (emulated.exists() && emulated.canRead()) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public static void requestStoragePermission(Activity activity) {
        if (activity == null) return;

        // Request standard runtime permissions
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, REQUEST_CODE_STORAGE);

        // On Android 11+, also guide to All Files Access settings if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                }
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(intent);
                } catch (Exception ignored) {}
            }
        }
    }
}

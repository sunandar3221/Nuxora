package com.nuxora.app;

import android.app.Application;
import com.google.android.material.color.DynamicColors;
import rikka.shizuku.Shizuku;

public class NuxoraApplication extends Application {

    private static volatile boolean isShizukuConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // Enables Google Material You dynamic color theming on Android 12+
        DynamicColors.applyToActivitiesIfAvailable(this);

        try {
            Shizuku.addBinderReceivedListenerSticky(() -> isShizukuConnected = true);
            Shizuku.addBinderDeadListener(() -> isShizukuConnected = false);
        } catch (Throwable ignored) {}
    }

    public static boolean isShizukuConnected() {
        try {
            return isShizukuConnected || Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }
}

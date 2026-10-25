package com.anlinux.app;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class AnLinuxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Enables Google Material You dynamic color theming on Android 12+
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}

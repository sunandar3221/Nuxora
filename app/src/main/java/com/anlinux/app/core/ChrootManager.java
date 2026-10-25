package com.anlinux.app.core;

import android.content.Context;
import com.anlinux.app.data.model.InstalledDistro;
import com.anlinux.app.utils.DeviceUtils;

import java.io.File;
import java.io.IOException;

public class ChrootManager {

    private final Context context;
    private final File binDir;

    public ChrootManager(Context context) {
        this.context = context.getApplicationContext();
        this.binDir = new File(this.context.getFilesDir(), "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
    }

    public boolean isRootAvailable() {
        return DeviceUtils.isRootAvailable();
    }

    public String[] buildCommand(InstalledDistro distro) throws IOException {
        File rootfsDir = new File(distro.getRootfsPath());
        File script = ScriptGenerator.createChrootLaunchScript(binDir, rootfsDir, distro);
        // Use su with mount-master flag (-mm) if supported by Magisk/KernelSU, otherwise standard su
        return new String[]{"su", "-c", "/system/bin/sh " + script.getAbsolutePath()};
    }

    public void stopChroot(InstalledDistro distro) {
        try {
            File rootfsDir = new File(distro.getRootfsPath());
            File stopScript = ScriptGenerator.createChrootStopScript(binDir, rootfsDir);
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "/system/bin/sh " + stopScript.getAbsolutePath()});
            p.waitFor();
        } catch (Exception ignored) {}
    }
}

package com.anlinux.app.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import com.anlinux.app.data.model.InstalledDistro;
import com.anlinux.app.utils.DeviceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PRootManager {

    private final Context context;
    private final File binDir;
    private final File prootBin;

    public PRootManager(Context context) {
        this.context = context.getApplicationContext();
        this.binDir = new File(this.context.getFilesDir(), "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
        this.prootBin = new File(binDir, "proot");
    }

    public File getProotBin() {
        return prootBin;
    }

    public File getBinDir() {
        return binDir;
    }

    public boolean isProotInstalled() {
        File loader = new File(binDir, "loader");
        File talloc = new File(binDir, "libtalloc.so.2");
        return prootBin.exists() && prootBin.canExecute() && loader.exists() && talloc.exists();
    }

    /**
     * Extracts bundled PRoot binaries and libraries from APK assets to internal storage.
     * Guaranteed 100% offline and no 404 error!
     */
    public void ensureProotBinary(ExtractionHelper.ProgressListener progressListener) throws Exception {
        if (isProotInstalled()) {
            return;
        }

        String archDir = resolveAssetArchDir();
        if (progressListener != null) {
            progressListener.onProgress("Menyiapkan komponen PRoot (" + archDir + ")…", 1);
        }

        AssetManager assets = context.getAssets();
        String[] files = {"proot", "loader", "libtalloc.so.2"};

        for (String fileName : files) {
            String assetPath = "bin/" + archDir + "/" + fileName;
            File targetFile = new File(binDir, fileName);

            try (InputStream in = assets.open(assetPath);
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            targetFile.setExecutable(true, false);
            targetFile.setReadable(true, false);
        }
    }

    private String resolveAssetArchDir() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.equalsIgnoreCase("arm64-v8a")) return "arm64-v8a";
            if (abi.startsWith("armeabi")) return "armeabi-v7a";
            if (abi.equalsIgnoreCase("x86_64")) return "x86_64";
            if (abi.startsWith("x86")) return "x86";
        }
        return "arm64-v8a"; // Default fallback
    }

    /**
     * Build launch command for PRoot execution
     */
    public String[] buildCommand(InstalledDistro distro) throws IOException {
        File rootfsDir = new File(distro.getRootfsPath());
        File script = ScriptGenerator.createProotLaunchScript(binDir, rootfsDir, distro);
        return new String[]{"/system/bin/sh", script.getAbsolutePath()};
    }
}

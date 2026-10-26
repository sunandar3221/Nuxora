package com.nuxora.app.core;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.utils.DeviceUtils;

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

    public File getBinDir() {
        return binDir;
    }

    /**
     * Resolves the executable PRoot binary.
     * On Android 10+ (API 29+), binaries in nativeLibraryDir (libproot.so) have execute (r-xp)
     * permissions guaranteed by Android SELinux.
     */
    public File getProotExecutable() {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        if (nativeDir != null) {
            File prootSo = new File(nativeDir, "libproot.so");
            if (prootSo.exists()) {
                return prootSo;
            }
        }
        return prootBin;
    }

    public File getLoaderExecutable() {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        if (nativeDir != null) {
            File loaderSo = new File(nativeDir, "libloader.so");
            if (loaderSo.exists()) {
                return loaderSo;
            }
        }
        return new File(binDir, "loader");
    }

    public String getLibraryPath() {
        String nativeDir = context.getApplicationInfo().nativeLibraryDir;
        if (nativeDir != null && new File(nativeDir).exists()) {
            return nativeDir + ":" + binDir.getAbsolutePath();
        }
        return binDir.getAbsolutePath();
    }

    public boolean isProotInstalled() {
        File prootExe = getProotExecutable();
        if (prootExe.exists()) {
            return true;
        }
        File loader = new File(binDir, "loader");
        File talloc = new File(binDir, "libtalloc.so.2");
        return prootBin.exists() && loader.exists();
    }

    /**
     * Ensures all PRoot support binaries and libraries are ready.
     * Extracts from assets as fallback and sets executable permissions with chmod.
     */
    public void ensureProotBinary(ExtractionHelper.ProgressListener progressListener) throws Exception {
        String archDir = resolveAssetArchDir();

        if (progressListener != null) {
            progressListener.onProgress("Menyiapkan komponen PRoot…", 1);
        }

        AssetManager assets = context.getAssets();
        String[] files = {"proot", "loader", "libtalloc.so.2"};

        for (String fileName : files) {
            File targetFile = new File(binDir, fileName);
            if (!targetFile.exists() || targetFile.length() == 0) {
                String assetPath = "bin/" + archDir + "/" + fileName;
                try (InputStream in = assets.open(assetPath);
                     OutputStream out = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }

            try {
                targetFile.setExecutable(true, false);
                targetFile.setReadable(true, false);
                Runtime.getRuntime().exec(new String[]{"chmod", "755", targetFile.getAbsolutePath()}).waitFor();
            } catch (Exception ignored) {}
        }
    }

    private String resolveAssetArchDir() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (abi.equalsIgnoreCase("arm64-v8a")) return "arm64-v8a";
            if (abi.startsWith("armeabi")) return "armeabi-v7a";
            if (abi.equalsIgnoreCase("x86_64")) return "x86_64";
            if (abi.startsWith("x86")) return "x86";
        }
        return "arm64-v8a";
    }

    /**
     * Build launch command for PRoot execution
     */
    public String[] buildCommand(InstalledDistro distro) throws IOException {
        File rootfsDir = new File(distro.getRootfsPath());
        File script = ScriptGenerator.createProotLaunchScript(this, rootfsDir, distro);
        return new String[]{"/system/bin/sh", script.getAbsolutePath()};
    }
}

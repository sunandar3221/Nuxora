package com.anlinux.app.core;

import android.content.Context;
import com.anlinux.app.data.model.InstalledDistro;
import com.anlinux.app.utils.DeviceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public boolean isProotInstalled() {
        return prootBin.exists() && prootBin.canExecute();
    }

    /**
     * Download or prepare PRoot binary for current CPU architecture
     */
    public void ensureProotBinary(ExtractionHelper.ProgressListener progressListener) throws Exception {
        if (isProotInstalled()) {
            return;
        }

        String arch = DeviceUtils.getNormalizedArch();
        // Official Termux / PRoot static binaries mirror
        String downloadUrl = getProotDownloadUrl(arch);

        if (progressListener != null) {
            progressListener.onProgress("Mengunduh komponen PRoot (" + arch + ")…", 1);
        }

        downloadBinary(downloadUrl, prootBin);
        prootBin.setExecutable(true, false);
        prootBin.setReadable(true, false);
    }

    private String getProotDownloadUrl(String arch) {
        switch (arch) {
            case "armhf":
                return "https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Library/PRoot/armhf/proot";
            case "x86_64":
                return "https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Library/PRoot/x86_64/proot";
            case "i686":
                return "https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Library/PRoot/i686/proot";
            case "aarch64":
            default:
                return "https://raw.githubusercontent.com/EXALAB/AnLinux-Resources/master/Library/PRoot/aarch64/proot";
        }
    }

    private void downloadBinary(String urlStr, File targetFile) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "AnLinux-Android/1.0");

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error: " + conn.getResponseCode() + " " + conn.getResponseMessage());
        }

        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }
        tempFile.renameTo(targetFile);
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

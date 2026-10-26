package com.nuxora.app.core;

import android.content.Context;
import android.os.Build;

import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.utils.DeviceUtils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class PRootManager {

    private static final String[] PROOT_RELEASE_URL_BASES = new String[]{
            "https://github.com/wellkops/Nuxora/releases/latest/download/proot-",
            "https://github.com/wellkops/Nuxora/releases/download/v1.3.0/proot-"
    };

    private final Context context;
    private final File binDir;
    private final File prootBin;
    private final File loaderBin;

    public PRootManager(Context context) {
        this.context = context.getApplicationContext();
        this.binDir = new File(this.context.getFilesDir(), "bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }
        this.prootBin = new File(binDir, "proot");
        this.loaderBin = new File(binDir, "loader");
    }

    public File getBinDir() {
        return binDir;
    }

    public File getProotExecutable() {
        return prootBin;
    }

    public File getLoaderExecutable() {
        return loaderBin;
    }

    public String getLibraryPath() {
        return binDir.getAbsolutePath();
    }

    public boolean isProotInstalled() {
        File loader = new File(binDir, "loader");
        File talloc = new File(binDir, "libtalloc.so.2");
        return prootBin.exists() && prootBin.length() > 0 &&
               loader.exists() && loader.length() > 0 &&
               talloc.exists() && talloc.length() > 0;
    }

    /**
     * Downloads and prepares PRoot support binaries and libraries at runtime (Option A).
     * The APK itself contains zero GPL native binaries.
     */
    public void ensureProotBinary(ExtractionHelper.ProgressListener progressListener) throws Exception {
        if (isProotInstalled()) {
            return;
        }

        String arch = resolveAssetArchDir();

        if (progressListener != null) {
            progressListener.onProgress("Mengunduh komponen PRoot (" + arch + ")…", 1);
        }

        File tempTarGz = new File(context.getCacheDir(), "proot_" + arch + ".tar.gz");
        if (tempTarGz.exists()) {
            tempTarGz.delete();
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        boolean downloadSuccess = false;
        String lastError = "";

        for (String urlBase : PROOT_RELEASE_URL_BASES) {
            String downloadUrl = urlBase + arch + ".tar.gz";
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Nuxora/1.3.0 (Android; Mobile)")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        try (InputStream in = body.byteStream();
                             FileOutputStream out = new FileOutputStream(tempTarGz)) {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        }
                        if (tempTarGz.exists() && tempTarGz.length() > 1000) {
                            downloadSuccess = true;
                            break;
                        }
                    }
                } else {
                    lastError = "HTTP " + response.code();
                }
            } catch (Exception e) {
                lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            }
        }

        if (!downloadSuccess) {
            throw new IOException("Gagal mengunduh komponen PRoot (" + lastError + "). Pastikan koneksi internet aktif.");
        }

        if (progressListener != null) {
            progressListener.onProgress("Mengekstrak komponen PRoot…", 1);
        }

        // Extract tar.gz into binDir
        ExtractionHelper.extractArchive(tempTarGz, binDir, (file, count) -> {
            if (progressListener != null) {
                progressListener.onProgress("Menyiapkan " + file, count);
            }
        });

        tempTarGz.delete();

        // Ensure executable permissions
        String[] files = {"proot", "loader", "libtalloc.so.2"};
        for (String fileName : files) {
            File targetFile = new File(binDir, fileName);
            if (targetFile.exists()) {
                targetFile.setExecutable(true, false);
                targetFile.setReadable(true, false);
                try {
                    Runtime.getRuntime().exec(new String[]{"chmod", "755", targetFile.getAbsolutePath()}).waitFor();
                } catch (Exception ignored) {}
            }
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

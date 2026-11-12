package com.nuxora.app.core;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.nuxora.app.data.database.DistroDatabaseHelper;
import com.nuxora.app.data.model.Distro;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.service.LinuxService;
import com.nuxora.app.utils.DeviceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstallManager {

    private static volatile InstallManager instance;

    public static InstallManager getInstance() {
        if (instance == null) {
            synchronized (InstallManager.class) {
                if (instance == null) {
                    instance = new InstallManager();
                }
            }
        }
        return instance;
    }

    public interface InstallListener {
        void onLog(String line);
        void onStatus(String statusText);
        void onProgress(int progress, boolean isIndeterminate);
        void onCompleted(long installedDistroId);
        void onError(String errorMsg);
    }

    private boolean isInstalling = false;
    private boolean isCompleted = false;
    private long completedInstalledDistroId = -1;
    private String errorMsg = null;

    private String distroDisplayName = "";
    private String currentStatus = "";
    private int currentProgress = 0;
    private boolean isIndeterminate = true;

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private InstallListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private InstallManager() {}

    public synchronized boolean isInstalling() {
        return isInstalling;
    }

    public synchronized boolean isCompleted() {
        return isCompleted;
    }

    public synchronized long getCompletedInstalledDistroId() {
        return completedInstalledDistroId;
    }

    public synchronized String getDistroDisplayName() {
        return distroDisplayName;
    }

    public synchronized String getCurrentStatus() {
        return currentStatus;
    }

    public synchronized int getCurrentProgress() {
        return currentProgress;
    }

    public synchronized boolean isCurrentIndeterminate() {
        return isIndeterminate;
    }

    public synchronized String getErrorMessage() {
        return errorMsg;
    }

    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public synchronized void setListener(InstallListener listener) {
        this.listener = listener;
    }

    public synchronized void reset() {
        if (!isInstalling) {
            isCompleted = false;
            completedInstalledDistroId = -1;
            errorMsg = null;
            distroDisplayName = "";
            currentStatus = "";
            currentProgress = 0;
            isIndeterminate = true;
            logs.clear();
        }
    }

    public synchronized void startInstallation(Context context, ExecutionMode executionMode, Distro distro,
                                              String displayName, String localFileUriString, boolean mountSd,
                                              boolean installExtras, boolean setupUser, String newUsername,
                                              String newPassword, String selectedGroups, boolean isDefaultUser) {
        if (isInstalling) return;

        isInstalling = true;
        isCompleted = false;
        completedInstalledDistroId = -1;
        errorMsg = null;
        distroDisplayName = displayName;
        currentStatus = "Memulai instalasi…";
        currentProgress = 0;
        isIndeterminate = true;
        logs.clear();

        final Context appContext = context.getApplicationContext();

        new Thread(() -> performInstall(appContext, executionMode, distro, displayName, localFileUriString,
                mountSd, installExtras, setupUser, newUsername, newPassword, selectedGroups, isDefaultUser)).start();
    }

    private void performInstall(Context context, ExecutionMode executionMode, Distro distro,
                                String displayName, String localFileUriString, boolean mountSd,
                                boolean installExtras, boolean setupUser, String newUsername,
                                String newPassword, String selectedGroups, boolean isDefaultUser) {
        LinuxService.startInstall(context, displayName);
        try {
            log("Memulai proses instalasi untuk " + displayName + "…");
            log("Mode: " + executionMode.getDisplayName());

            // 1. Prepare PRoot if Non-Root
            if (executionMode == ExecutionMode.PROOT) {
                setStatus("Memeriksa komponen PRoot…");
                setProgress(0, true);
                LinuxService.updateInstallProgress(context, displayName, "Menyiapkan komponen PRoot…", 0, true);
                PRootManager prootManager = new PRootManager(context);
                if (!prootManager.isProotInstalled()) {
                    log("Mengunduh komponen PRoot binary…");
                    prootManager.ensureProotBinary((file, count) -> log(file));
                    log("PRoot siap digunakan.");
                }
            }

            // 2. Prepare Rootfs file
            String arch = DeviceUtils.getNormalizedArch();
            String downloadUrl = distro.getUrlForArch(arch);
            File cacheDir = context.getExternalCacheDir() != null ? context.getExternalCacheDir() : context.getCacheDir();

            String fileExt = ".tar.xz";
            if (localFileUriString != null) {
                String lower = localFileUriString.toLowerCase();
                if (lower.contains(".tar.gz") || lower.contains(".tgz")) fileExt = ".tar.gz";
                else if (lower.contains(".tar.bz2")) fileExt = ".tar.bz2";
            } else if (downloadUrl != null) {
                String lower = downloadUrl.toLowerCase();
                if (lower.contains(".tar.gz") || lower.contains(".tgz")) fileExt = ".tar.gz";
                else if (lower.contains(".tar.bz2")) fileExt = ".tar.bz2";
            }
            File archiveFile = new File(cacheDir, "rootfs_" + distro.getId() + fileExt);
            if (archiveFile.exists()) {
                archiveFile.delete();
            }

            if (localFileUriString != null) {
                setStatus("Menyalin berkas arsip lokal…");
                setProgress(0, true);
                LinuxService.updateInstallProgress(context, displayName, "Menyalin berkas arsip lokal…", 0, true);
                log("Membaca berkas lokal…");
                Uri localUri = Uri.parse(localFileUriString);
                try (InputStream in = context.getContentResolver().openInputStream(localUri);
                     FileOutputStream out = new FileOutputStream(archiveFile)) {
                    byte[] buf = new byte[32768];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                }
                log("Berkas arsip lokal berhasil disalin (" + DeviceUtils.formatFileSize(archiveFile.length()) + ").");
            } else {
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                    throw new Exception("URL download tidak tersedia untuk arsitektur: " + arch);
                }
                setStatus("Mengunduh image sistem Linux (RootFS)…");
                log("Mengunduh dari: " + downloadUrl);
                downloadFile(context, downloadUrl, archiveFile, displayName);
                log("Pengunduhan selesai. Ukuran berkas: " + DeviceUtils.formatFileSize(archiveFile.length()));
            }

            if (!archiveFile.exists() || archiveFile.length() < 100000) {
                throw new Exception("Berkas arsip tidak lengkap (" + DeviceUtils.formatFileSize(archiveFile.length()) + "). Silakan coba unduh lagi.");
            }

            // 3. Extract Rootfs
            setStatus("Mengekstrak berkas sistem Linux…");
            setProgress(0, true);
            LinuxService.updateInstallProgress(context, displayName, "Mengekstrak berkas sistem Linux…", 0, true);
            File distrosBaseDir = new File(context.getFilesDir(), "distros");
            if (!distrosBaseDir.exists()) distrosBaseDir.mkdirs();

            String folderName = distro.getId() + "_" + System.currentTimeMillis();
            File targetRootfsDir = new File(distrosBaseDir, folderName);
            targetRootfsDir.mkdirs();

            log("Mengekstrak ke: " + targetRootfsDir.getAbsolutePath());
            ExtractionHelper.extractArchive(archiveFile, targetRootfsDir, (currentFile, count) -> {
                log("[" + count + "] Ekstrak: " + currentFile);
                if (count % 150 == 0) {
                    LinuxService.updateInstallProgress(context, displayName, "Mengekstrak berkas (" + count + " file)...", 0, true);
                }
            });
            log("Ekstraksi RootFS selesai!");

            // 4. Configure DNS & environments & protected rm
            setStatus("Mengkonfigurasi DNS & paket sistem…");
            setProgress(95, false);
            LinuxService.updateInstallProgress(context, displayName, "Mengkonfigurasi DNS & paket sistem…", 95, false);
            ScriptGenerator.setupDns(targetRootfsDir);
            log("DNS resolv.conf disetel ke Google & Cloudflare DNS.");
            ScriptGenerator.setupPackageManagerFixes(targetRootfsDir);
            ScriptGenerator.setupProtectedRm(targetRootfsDir);
            ScriptGenerator.setupExtrasScript(targetRootfsDir, true);

            // 5. Run extras installation if selected
            if (installExtras) {
                setStatus("Memasang paket penting (extras)...");
                setProgress(97, true);
                LinuxService.updateInstallProgress(context, displayName, "Memasang paket penting (extras)…", 97, true);
                log("\n--- Menjalankan Pemasangan Paket Penting (Extras) ---");
                log("Memasang perkakas dasar (nano, curl, wget, sudo, ca-certificates)...");
                runExtrasInstallation(context, executionMode, targetRootfsDir);
                log("--- Pemasangan Paket Penting Selesai! ---\n");
            } else {
                log("Paket penting dilewati. Anda dapat memasangnya kapan saja di terminal dengan: /usr/local/bin/install-extras");
            }

            // 6. Setup custom user if selected
            if (setupUser && !newUsername.isEmpty()) {
                setStatus("Menyiapkan pengguna: " + newUsername + "…");
                setProgress(98, true);
                LinuxService.updateInstallProgress(context, displayName, "Menyiapkan pengguna: " + newUsername + "…", 98, true);
                log("\n--- Menyiapkan Pengguna Baru (User Setup) ---");
                log("Nama Pengguna: " + newUsername);
                log("Keanggotaan Grup: " + (selectedGroups.isEmpty() ? "(default)" : selectedGroups));
                runUserSetupInstallation(context, executionMode, targetRootfsDir, newUsername, newPassword, selectedGroups);
                log("--- Konfigurasi Pengguna Selesai! ---\n");
            }

            // 7. Clean downloaded archive cache
            if (archiveFile.exists()) {
                archiveFile.delete();
            }

            // 8. Save to Database
            InstalledDistro installed = new InstalledDistro(
                    displayName,
                    distro.getId(),
                    targetRootfsDir.getAbsolutePath(),
                    executionMode
            );
            installed.setMountSdcard(mountSd);
            installed.setCustomShell(distro.getDefaultShell());
            installed.setDiskUsage(DeviceUtils.formatFileSize(targetRootfsDir.length()));
            if (isDefaultUser) {
                installed.setDefaultUser(newUsername);
            } else {
                installed.setDefaultUser("root");
            }

            long id = DistroDatabaseHelper.getInstance(context).insertDistro(installed);
            log("Instalasi berhasil disimpan ke database!");

            synchronized (this) {
                isInstalling = false;
                isCompleted = true;
                completedInstalledDistroId = id;
                currentProgress = 100;
                isIndeterminate = false;
                currentStatus = "Instalasi Berhasil!";
            }

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onStatus("Instalasi Berhasil!");
                    listener.onProgress(100, false);
                    listener.onCompleted(id);
                }
            });

        } catch (Exception e) {
            final String err = e.getMessage() != null ? e.getMessage() : e.toString();
            log("ERROR: " + err);
            synchronized (this) {
                isInstalling = false;
                isCompleted = false;
                errorMsg = err;
                currentStatus = "Gagal: " + err;
            }
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onError(err);
                }
            });
        } finally {
            LinuxService.stopInstall(context);
        }
    }

    private void runExtrasInstallation(Context context, ExecutionMode mode, File rootfsDir) {
        File execScript = null;
        try {
            Process process;
            if (mode == ExecutionMode.ROOT) {
                File binDir = new File(context.getFilesDir(), "bin");
                if (!binDir.exists()) binDir.mkdirs();
                execScript = ScriptGenerator.createChrootCommandScript(binDir, rootfsDir, "/usr/local/bin/install-extras");
                ProcessBuilder pb = new ProcessBuilder("su", "-c", "/system/bin/sh " + execScript.getAbsolutePath());
                pb.redirectErrorStream(true);
                process = pb.start();
            } else {
                PRootManager prootManager = new PRootManager(context);
                execScript = ScriptGenerator.createProotCommandScript(prootManager, rootfsDir, "/usr/local/bin/install-extras");
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", execScript.getAbsolutePath());
                pb.redirectErrorStream(true);
                process = pb.start();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains("proot warning") || line.toLowerCase().contains("proot info")) {
                        continue;
                    }
                    log(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log("[Pemberitahuan] Skrip paket selesai dengan status: " + exitCode);
            }
        } catch (Exception e) {
            log("[Peringatan] Kendala saat memasang paket penting: " + e.getMessage());
            log("Anda tetap dapat memasangnya secara manual nanti di terminal dengan: /usr/local/bin/install-extras");
        } finally {
            if (execScript != null && execScript.exists()) {
                execScript.delete();
            }
        }
    }

    private void runUserSetupInstallation(Context context, ExecutionMode mode, File rootfsDir, String username, String password, String groups) {
        File execScript = null;
        try {
            ScriptGenerator.setupUserScript(rootfsDir, username, password, groups);
            Process process;
            if (mode == ExecutionMode.ROOT) {
                File binDir = new File(context.getFilesDir(), "bin");
                if (!binDir.exists()) binDir.mkdirs();
                execScript = ScriptGenerator.createChrootCommandScript(binDir, rootfsDir, "/usr/local/bin/setup-user");
                ProcessBuilder pb = new ProcessBuilder("su", "-c", "/system/bin/sh " + execScript.getAbsolutePath());
                pb.redirectErrorStream(true);
                process = pb.start();
            } else {
                PRootManager prootManager = new PRootManager(context);
                execScript = ScriptGenerator.createProotCommandScript(prootManager, rootfsDir, "/usr/local/bin/setup-user");
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", execScript.getAbsolutePath());
                pb.redirectErrorStream(true);
                process = pb.start();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.toLowerCase().contains("proot warning") || line.toLowerCase().contains("proot info")) {
                        continue;
                    }
                    log(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log("[Pemberitahuan] Konfigurasi user selesai dengan status: " + exitCode);
            }
        } catch (Exception e) {
            log("[Peringatan] Kendala saat menyiapkan pengguna baru: " + e.getMessage());
        } finally {
            if (execScript != null && execScript.exists()) {
                execScript.delete();
            }
        }
    }

    private void downloadFile(Context context, String downloadUrl, File targetFile, String distroDisplayName) throws Exception {
        downloadFileWithRedirects(context, downloadUrl, targetFile, 0, distroDisplayName);
    }

    private void downloadFileWithRedirects(Context context, String downloadUrl, File targetFile, int redirectCount, String distroDisplayName) throws Exception {
        if (redirectCount > 6) {
            throw new Exception("Terlalu banyak pengalihan (Too many redirects) saat mengunduh.");
        }

        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Nuxora/1.5.0 (Android; Mobile)");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setInstanceFollowRedirects(false);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            if (newUrl != null && !newUrl.isEmpty()) {
                if (!newUrl.startsWith("http")) {
                    newUrl = new URL(url, newUrl).toString();
                }
                log("Mengikuti pengalihan URL (" + responseCode + ")…");
                downloadFileWithRedirects(context, newUrl, targetFile, redirectCount + 1, distroDisplayName);
                return;
            }
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("Server mengembalikan kode HTTP: " + responseCode);
        }

        long totalSize = conn.getContentLengthLong();
        long downloaded = 0;

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {

            byte[] buffer = new byte[32768];
            int read;
            long lastUpdateTime = 0;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;

                long now = System.currentTimeMillis();
                if (now - lastUpdateTime > 400) {
                    lastUpdateTime = now;
                    if (totalSize > 0) {
                        int percent = (int) ((downloaded * 100) / totalSize);
                        String status = "Mengunduh RootFS: " + percent + "% (" +
                                DeviceUtils.formatFileSize(downloaded) + " / " +
                                DeviceUtils.formatFileSize(totalSize) + ")";
                        setStatus(status);
                        setProgress(percent, false);
                        LinuxService.updateInstallProgress(context, distroDisplayName, status, percent, false);
                    } else {
                        String status = "Mengunduh RootFS: " + DeviceUtils.formatFileSize(downloaded);
                        setStatus(status);
                        setProgress(0, true);
                        LinuxService.updateInstallProgress(context, distroDisplayName, status, 0, true);
                    }
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    private void log(String line) {
        synchronized (logs) {
            logs.add(line);
            if (logs.size() > 3000) {
                logs.remove(0);
            }
        }
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onLog(line);
            }
        });
    }

    private void setStatus(String status) {
        this.currentStatus = status;
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onStatus(status);
            }
        });
    }

    private void setProgress(int percent, boolean indeterminate) {
        this.currentProgress = percent;
        this.isIndeterminate = indeterminate;
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onProgress(percent, indeterminate);
            }
        });
    }
}

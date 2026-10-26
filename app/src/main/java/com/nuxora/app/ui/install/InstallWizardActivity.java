package com.nuxora.app.ui.install;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nuxora.app.R;
import com.nuxora.app.core.ExtractionHelper;
import com.nuxora.app.core.PRootManager;
import com.nuxora.app.core.ScriptGenerator;
import com.nuxora.app.data.database.DistroDatabaseHelper;
import com.nuxora.app.data.model.Distro;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.service.LinuxService;
import com.nuxora.app.ui.terminal.TerminalActivity;
import com.nuxora.app.utils.DeviceUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class InstallWizardActivity extends AppCompatActivity {

    public static final String EXTRA_DISTRO = "extra_distro";
    public static final String EXTRA_LOCAL_FILE_URI = "extra_local_file_uri";

    private Distro distro;
    private String localFileUriString;

    private RadioButton radioProot;
    private RadioButton radioRoot;
    private MaterialCardView cardModeProot;
    private MaterialCardView cardModeRoot;
    private TextInputEditText etDistroName;
    private MaterialSwitch switchMountSdcard;
    private MaterialSwitch switchInstallExtras;
    private LinearLayout progressSection;
    private TextView tvProgressStatus;
    private LinearProgressIndicator progressBar;
    private TextView tvLogs;
    private ScrollView scrollLogs;
    private MaterialButton btnActionInstall;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isInstallCompleted = false;
    private long installedDistroId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_wizard);

        distro = (Distro) getIntent().getSerializableExtra(EXTRA_DISTRO);
        localFileUriString = getIntent().getStringExtra(EXTRA_LOCAL_FILE_URI);

        if (distro == null) {
            Toast.makeText(this, "Data distro tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.wizardToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView imgDistro = findViewById(R.id.imgDistroSelected);
        TextView tvName = findViewById(R.id.tvDistroSelectedName);
        TextView tvArch = findViewById(R.id.tvDistroArch);

        imgDistro.setImageResource(distro.getIconRes());
        tvName.setText(distro.getName() + " " + distro.getVersion());
        String currentArch = DeviceUtils.getNormalizedArch();
        tvArch.setText("Arsitektur Sistem: " + currentArch);

        radioProot = findViewById(R.id.radioProot);
        radioRoot = findViewById(R.id.radioRoot);
        cardModeProot = findViewById(R.id.cardModeProot);
        cardModeRoot = findViewById(R.id.cardModeRoot);
        etDistroName = findViewById(R.id.etDistroName);
        switchMountSdcard = findViewById(R.id.switchMountSdcard);
        switchMountSdcard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !com.nuxora.app.utils.StoragePermissionHelper.hasStoragePermission(this)) {
                com.nuxora.app.utils.StoragePermissionHelper.requestStoragePermission(this);
            }
        });
        switchInstallExtras = findViewById(R.id.switchInstallExtras);
        progressSection = findViewById(R.id.progressSection);
        tvProgressStatus = findViewById(R.id.tvProgressStatus);
        progressBar = findViewById(R.id.progressBar);
        tvLogs = findViewById(R.id.tvLogs);
        scrollLogs = findViewById(R.id.scrollLogs);
        btnActionInstall = findViewById(R.id.btnActionInstall);

        etDistroName.setText(distro.getName());

        cardModeProot.setOnClickListener(v -> selectMode(true));
        radioProot.setOnClickListener(v -> selectMode(true));

        cardModeRoot.setOnClickListener(v -> selectMode(false));
        radioRoot.setOnClickListener(v -> selectMode(false));

        btnActionInstall.setOnClickListener(v -> {
            if (isInstallCompleted) {
                Intent intent = new Intent(this, TerminalActivity.class);
                intent.putExtra(TerminalActivity.EXTRA_DISTRO_ID, installedDistroId);
                startActivity(intent);
                finish();
            } else {
                startInstallation();
            }
        });
    }

    private void selectMode(boolean prootSelected) {
        if (!prootSelected && !DeviceUtils.isRootAvailable()) {
            Toast.makeText(this, "Perangkat tidak memiliki akses root (su). Gunakan mode Non-Root (PRoot).", Toast.LENGTH_LONG).show();
            prootSelected = true;
        }

        radioProot.setChecked(prootSelected);
        radioRoot.setChecked(!prootSelected);
        cardModeProot.setStrokeColor(prootSelected ? getColor(R.color.primary) : getColor(R.color.outline));
        cardModeRoot.setStrokeColor(!prootSelected ? getColor(R.color.primary) : getColor(R.color.outline));
    }

    private void startInstallation() {
        btnActionInstall.setEnabled(false);
        cardModeProot.setEnabled(false);
        cardModeRoot.setEnabled(false);
        etDistroName.setEnabled(false);
        switchMountSdcard.setEnabled(false);
        progressSection.setVisibility(View.VISIBLE);

        final ExecutionMode executionMode = radioRoot.isChecked() ? ExecutionMode.ROOT : ExecutionMode.PROOT;
        final String distroDisplayName = etDistroName.getText() != null && !etDistroName.getText().toString().trim().isEmpty()
                ? etDistroName.getText().toString().trim()
                : distro.getName();
        final boolean mountSd = switchMountSdcard.isChecked();
        final boolean installExtras = switchInstallExtras != null && switchInstallExtras.isChecked();

        new Thread(() -> performInstall(executionMode, distroDisplayName, mountSd, installExtras)).start();
    }

    private void performInstall(ExecutionMode executionMode, String distroDisplayName, boolean mountSd, boolean installExtras) {
        LinuxService.startInstall(this, distroDisplayName);
        try {
            log("Memulai proses instalasi untuk " + distroDisplayName + "…");
            log("Mode: " + executionMode.getDisplayName());

            // 1. Prepare PRoot if Non-Root
            if (executionMode == ExecutionMode.PROOT) {
                setStatus("Memeriksa komponen PRoot…");
                LinuxService.updateInstallProgress(this, distroDisplayName, "Menyiapkan komponen PRoot…", 0, true);
                PRootManager prootManager = new PRootManager(this);
                if (!prootManager.isProotInstalled()) {
                    log("Mengunduh komponen PRoot binary…");
                    prootManager.ensureProotBinary((file, count) -> log(file));
                    log("PRoot siap digunakan.");
                }
            }

            // 2. Prepare Rootfs file
            String arch = DeviceUtils.getNormalizedArch();
            String downloadUrl = distro.getUrlForArch(arch);
            File cacheDir = getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir();

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
                LinuxService.updateInstallProgress(this, distroDisplayName, "Menyalin berkas arsip lokal…", 0, true);
                log("Membaca berkas lokal…");
                Uri localUri = Uri.parse(localFileUriString);
                try (InputStream in = getContentResolver().openInputStream(localUri);
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
                downloadFile(downloadUrl, archiveFile, distroDisplayName);
                log("Pengunduhan selesai. Ukuran berkas: " + DeviceUtils.formatFileSize(archiveFile.length()));
            }

            if (!archiveFile.exists() || archiveFile.length() < 100000) {
                throw new Exception("Berkas arsip tidak lengkap (" + DeviceUtils.formatFileSize(archiveFile.length()) + "). Silakan coba unduh lagi.");
            }

            // 3. Extract Rootfs
            setStatus("Mengekstrak berkas sistem Linux…");
            LinuxService.updateInstallProgress(this, distroDisplayName, "Mengekstrak berkas sistem Linux…", 0, true);
            File distrosBaseDir = new File(getFilesDir(), "distros");
            if (!distrosBaseDir.exists()) distrosBaseDir.mkdirs();

            String folderName = distro.getId() + "_" + System.currentTimeMillis();
            File targetRootfsDir = new File(distrosBaseDir, folderName);
            targetRootfsDir.mkdirs();

            log("Mengekstrak ke: " + targetRootfsDir.getAbsolutePath());
            ExtractionHelper.extractArchive(archiveFile, targetRootfsDir, (currentFile, count) -> {
                log("[" + count + "] Ekstrak: " + currentFile);
                if (count % 150 == 0) {
                    LinuxService.updateInstallProgress(this, distroDisplayName, "Mengekstrak berkas (" + count + " file)...", 0, true);
                }
            });
            log("Ekstraksi RootFS selesai!");

            // 4. Configure DNS & environments
            setStatus("Mengkonfigurasi DNS & paket sistem…");
            LinuxService.updateInstallProgress(this, distroDisplayName, "Mengkonfigurasi DNS & paket sistem…", 95, false);
            ScriptGenerator.setupDns(targetRootfsDir);
            log("DNS resolv.conf disetel ke Google & Cloudflare DNS.");
            ScriptGenerator.setupPackageManagerFixes(targetRootfsDir);
            ScriptGenerator.setupExtrasScript(targetRootfsDir, installExtras);
            if (installExtras) {
                log("Paket penting (nano, curl, bash, sudo) diatur untuk dipasang otomatis saat sesi pertama.");
            } else {
                log("Skrip paket tambahan siap di /usr/local/bin/install-extras");
            }

            // 5. Clean downloaded archive cache
            if (archiveFile.exists()) {
                archiveFile.delete();
            }

            // 6. Save to Database
            InstalledDistro installed = new InstalledDistro(
                    distroDisplayName,
                    distro.getId(),
                    targetRootfsDir.getAbsolutePath(),
                    executionMode
            );
            installed.setMountSdcard(mountSd);
            installed.setCustomShell(distro.getDefaultShell());
            installed.setDiskUsage(DeviceUtils.formatFileSize(targetRootfsDir.length()));

            installedDistroId = DistroDatabaseHelper.getInstance(this).insertDistro(installed);
            log("Instalasi berhasil disimpan ke database!");

            mainHandler.post(() -> {
                isInstallCompleted = true;
                progressBar.setIndeterminate(false);
                progressBar.setProgress(100);
                tvProgressStatus.setText(R.string.install_success);
                btnActionInstall.setText(R.string.btn_terminal);
                btnActionInstall.setIconResource(R.drawable.ic_terminal);
                btnActionInstall.setEnabled(true);
                Toast.makeText(this, "Instalasi selesai! Tekan Buka Terminal.", Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            final String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            log("ERROR: " + errorMsg);
            mainHandler.post(() -> {
                tvProgressStatus.setText("Gagal: " + errorMsg);
                btnActionInstall.setEnabled(true);
                btnActionInstall.setText("Coba Lagi");
                Toast.makeText(this, "Gagal menginstal: " + errorMsg, Toast.LENGTH_LONG).show();
            });
        } finally {
            LinuxService.stopInstall(this);
        }
    }

    private void downloadFile(String downloadUrl, File targetFile, String distroDisplayName) throws Exception {
        downloadFileWithRedirects(downloadUrl, targetFile, 0, distroDisplayName);
    }

    private void downloadFileWithRedirects(String downloadUrl, File targetFile, int redirectCount, String distroDisplayName) throws Exception {
        if (redirectCount > 6) {
            throw new Exception("Terlalu banyak pengalihan (Too many redirects) saat mengunduh.");
        }

        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36");

        int status = conn.getResponseCode();
        if (status >= 300 && status <= 308) {
            String redirectUrl = conn.getHeaderField("Location");
            if (redirectUrl != null) {
                log("Mengikuti pengalihan URL…");
                conn.disconnect();
                downloadFileWithRedirects(redirectUrl, targetFile, redirectCount + 1, distroDisplayName);
                return;
            }
        }

        if (status != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP " + status + " (" + conn.getResponseMessage() + ") untuk tautan: " + downloadUrl);
        }

        long totalSize = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buf = new byte[32768];
            long downloaded = 0;
            int read;
            long lastLogTime = System.currentTimeMillis();

            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                downloaded += read;
                if (System.currentTimeMillis() - lastLogTime > 800) {
                    lastLogTime = System.currentTimeMillis();
                    if (totalSize > 0) {
                        int percent = (int) ((downloaded * 100) / totalSize);
                        String progressText = "Mengunduh: " + percent + "% (" + DeviceUtils.formatFileSize(downloaded) + " / " + DeviceUtils.formatFileSize(totalSize) + ")";
                        log(progressText);
                        LinuxService.updateInstallProgress(this, distroDisplayName, progressText, percent, false);
                        mainHandler.post(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress(percent);
                        });
                    } else {
                        String progressText = "Mengunduh: " + DeviceUtils.formatFileSize(downloaded);
                        log(progressText);
                        LinuxService.updateInstallProgress(this, distroDisplayName, progressText, 0, true);
                    }
                }
            }

            if (totalSize > 0 && downloaded < totalSize) {
                throw new Exception("Unduhan terputus di tengah jalan (" + DeviceUtils.formatFileSize(downloaded) + " dari " + DeviceUtils.formatFileSize(totalSize) + "). Silakan coba lagi.");
            }
        } finally {
            conn.disconnect();
        }
    }

    private void setStatus(String status) {
        mainHandler.post(() -> tvProgressStatus.setText(status));
    }

    private void log(String message) {
        mainHandler.post(() -> {
            if (tvLogs != null) {
                tvLogs.append(message + "\n");
                if (scrollLogs != null) {
                    scrollLogs.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }
}

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
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class InstallWizardActivity extends AppCompatActivity implements com.nuxora.app.core.InstallManager.InstallListener {

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
    private MaterialSwitch switchSetupUser;
    private LinearLayout layoutUserConfig;
    private TextInputEditText etUsername;
    private TextInputEditText etUserPassword;
    private Chip chipGroupSudo;
    private Chip chipGroupAudio;
    private Chip chipGroupVideo;
    private Chip chipGroupStorage;
    private Chip chipGroupNetdev;
    private Chip chipGroupInput;
    private Chip chipGroupUsers;
    private MaterialSwitch switchDefaultUser;
    private LinearLayout progressSection;
    private TextView tvProgressStatus;
    private LinearProgressIndicator progressBar;
    private TextView tvLogs;
    private ScrollView scrollLogs;
    private MaterialButton btnActionInstall;

    private boolean isInstallCompleted = false;
    private long installedDistroId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_wizard);

        distro = (Distro) getIntent().getSerializableExtra(EXTRA_DISTRO);
        localFileUriString = getIntent().getStringExtra(EXTRA_LOCAL_FILE_URI);

        if (distro == null) {
            com.nuxora.app.core.InstallManager manager = com.nuxora.app.core.InstallManager.getInstance();
            if (manager.isInstalling() || manager.isCompleted()) {
                String dName = manager.getDistroDisplayName();
                if (dName == null || dName.isEmpty()) dName = "Linux Distro";
                distro = new Distro("active", dName, "", "", R.drawable.ic_distro_generic);
            } else {
                Toast.makeText(this, "Data distro tidak ditemukan.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.nuxora.app.core.InstallManager manager = com.nuxora.app.core.InstallManager.getInstance();
        manager.setListener(this);

        if (manager.isInstalling()) {
            btnActionInstall.setEnabled(false);
            btnActionInstall.setText("Menginstal…");
            cardModeProot.setEnabled(false);
            cardModeRoot.setEnabled(false);
            etDistroName.setEnabled(false);
            switchMountSdcard.setEnabled(false);
            if (switchInstallExtras != null) switchInstallExtras.setEnabled(false);
            if (switchSetupUser != null) switchSetupUser.setEnabled(false);
            progressSection.setVisibility(View.VISIBLE);

            StringBuilder sb = new StringBuilder();
            for (String l : manager.getLogs()) {
                sb.append(l).append("\n");
            }
            tvLogs.setText(sb.toString());
            tvProgressStatus.setText(manager.getCurrentStatus());
            progressBar.setProgress(manager.getCurrentProgress());
            progressBar.setIndeterminate(manager.isCurrentIndeterminate());
            if (scrollLogs != null) {
                scrollLogs.post(() -> scrollLogs.fullScroll(ScrollView.FOCUS_DOWN));
            }
        } else if (manager.isCompleted()) {
            isInstallCompleted = true;
            installedDistroId = manager.getCompletedInstalledDistroId();
            progressSection.setVisibility(View.VISIBLE);

            StringBuilder sb = new StringBuilder();
            for (String l : manager.getLogs()) {
                sb.append(l).append("\n");
            }
            tvLogs.setText(sb.toString());
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
            tvProgressStatus.setText(R.string.install_success);
            btnActionInstall.setText(R.string.btn_terminal);
            btnActionInstall.setIconResource(R.drawable.ic_terminal);
            btnActionInstall.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.nuxora.app.core.InstallManager.getInstance().setListener(null);
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
        switchSetupUser = findViewById(R.id.switchSetupUser);
        layoutUserConfig = findViewById(R.id.layoutUserConfig);
        etUsername = findViewById(R.id.etUsername);
        etUserPassword = findViewById(R.id.etUserPassword);
        chipGroupSudo = findViewById(R.id.chipGroupSudo);
        chipGroupAudio = findViewById(R.id.chipGroupAudio);
        chipGroupVideo = findViewById(R.id.chipGroupVideo);
        chipGroupStorage = findViewById(R.id.chipGroupStorage);
        chipGroupNetdev = findViewById(R.id.chipGroupNetdev);
        chipGroupInput = findViewById(R.id.chipGroupInput);
        chipGroupUsers = findViewById(R.id.chipGroupUsers);
        switchDefaultUser = findViewById(R.id.switchDefaultUser);

        switchSetupUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (layoutUserConfig != null) {
                layoutUserConfig.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

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
                checkStorageAndLaunchTerminal();
            } else {
                startInstallation();
            }
        });
    }

    private void checkStorageAndLaunchTerminal() {
        boolean hasPermission = com.nuxora.app.utils.StoragePermissionHelper.hasStoragePermission(this)
                || com.nuxora.app.utils.ShizukuHelper.hasShizukuPermission();
        if (switchMountSdcard.isChecked() && !hasPermission) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Izin Penyimpanan Dibutuhkan")
                    .setMessage("Distro ini mengaktifkan 'Mount SD Card', tetapi izin akses penyimpanan belum diberikan. Folder /sdcard tidak dapat dibaca tanpa izin ini.")
                    .setPositiveButton("Beri Izin", (dialog, which) -> {
                        com.nuxora.app.utils.StoragePermissionHelper.requestStoragePermission(this);
                    })
                    .setNegativeButton("Buka Terminal Saja", (dialog, which) -> {
                        openTerminalAndFinish();
                    })
                    .show();
            return;
        }
        openTerminalAndFinish();
    }

    private void openTerminalAndFinish() {
        Intent intent = new Intent(this, TerminalActivity.class);
        intent.putExtra(TerminalActivity.EXTRA_DISTRO_ID, installedDistroId);
        startActivity(intent);
        finish();
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
        btnActionInstall.setText("Menginstal…");
        cardModeProot.setEnabled(false);
        cardModeRoot.setEnabled(false);
        etDistroName.setEnabled(false);
        switchMountSdcard.setEnabled(false);
        if (switchInstallExtras != null) switchInstallExtras.setEnabled(false);
        if (switchSetupUser != null) switchSetupUser.setEnabled(false);
        progressSection.setVisibility(View.VISIBLE);

        final ExecutionMode executionMode = radioRoot.isChecked() ? ExecutionMode.ROOT : ExecutionMode.PROOT;
        final String distroDisplayName = etDistroName.getText() != null && !etDistroName.getText().toString().trim().isEmpty()
                ? etDistroName.getText().toString().trim()
                : distro.getName();
        final boolean mountSd = switchMountSdcard.isChecked();
        final boolean installExtras = switchInstallExtras != null && switchInstallExtras.isChecked();
        final boolean setupUser = switchSetupUser != null && switchSetupUser.isChecked();
        final String newUsername = (etUsername != null && etUsername.getText() != null)
                ? etUsername.getText().toString().trim().toLowerCase().replaceAll("[^a-z0-9_-]", "")
                : "user";
        final String newPassword = (etUserPassword != null && etUserPassword.getText() != null)
                ? etUserPassword.getText().toString()
                : "";

        StringBuilder groupsBuilder = new StringBuilder();
        if (chipGroupSudo != null && chipGroupSudo.isChecked()) groupsBuilder.append("sudo wheel ");
        if (chipGroupAudio != null && chipGroupAudio.isChecked()) groupsBuilder.append("audio ");
        if (chipGroupVideo != null && chipGroupVideo.isChecked()) groupsBuilder.append("video ");
        if (chipGroupStorage != null && chipGroupStorage.isChecked()) groupsBuilder.append("storage disk ");
        if (chipGroupNetdev != null && chipGroupNetdev.isChecked()) groupsBuilder.append("netdev inet ");
        if (chipGroupInput != null && chipGroupInput.isChecked()) groupsBuilder.append("input ");
        if (chipGroupUsers != null && chipGroupUsers.isChecked()) groupsBuilder.append("users ");
        final String selectedGroups = groupsBuilder.toString().trim();

        final boolean isDefaultUser = switchDefaultUser != null && switchDefaultUser.isChecked() && setupUser && !newUsername.isEmpty();

        com.nuxora.app.core.InstallManager.getInstance().startInstallation(
                this, executionMode, distro, distroDisplayName, localFileUriString,
                mountSd, installExtras, setupUser, newUsername, newPassword, selectedGroups, isDefaultUser
        );
    }

    @Override
    public void onLog(String line) {
        if (tvLogs != null) {
            tvLogs.append(line + "\n");
            if (scrollLogs != null) {
                scrollLogs.post(() -> scrollLogs.fullScroll(ScrollView.FOCUS_DOWN));
            }
        }
    }

    @Override
    public void onStatus(String statusText) {
        if (tvProgressStatus != null) {
            tvProgressStatus.setText(statusText);
        }
    }

    @Override
    public void onProgress(int progress, boolean isIndeterminate) {
        if (progressBar != null) {
            progressBar.setIndeterminate(isIndeterminate);
            if (!isIndeterminate) {
                progressBar.setProgress(progress);
            }
        }
    }

    @Override
    public void onCompleted(long id) {
        isInstallCompleted = true;
        installedDistroId = id;
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);
        }
        if (tvProgressStatus != null) {
            tvProgressStatus.setText(R.string.install_success);
        }
        if (btnActionInstall != null) {
            btnActionInstall.setText(R.string.btn_terminal);
            btnActionInstall.setIconResource(R.drawable.ic_terminal);
            btnActionInstall.setEnabled(true);
        }
        Toast.makeText(this, "Instalasi selesai! Tekan Buka Terminal.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String error) {
        if (tvProgressStatus != null) {
            tvProgressStatus.setText("Gagal: " + error);
        }
        if (btnActionInstall != null) {
            btnActionInstall.setEnabled(true);
            btnActionInstall.setText("Coba Lagi");
        }
        Toast.makeText(this, "Gagal menginstal: " + error, Toast.LENGTH_LONG).show();
    }
}

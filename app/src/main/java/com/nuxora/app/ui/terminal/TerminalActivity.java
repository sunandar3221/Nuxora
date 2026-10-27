package com.nuxora.app.ui.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.nuxora.app.R;
import com.nuxora.app.core.ChrootManager;
import com.nuxora.app.core.PRootManager;
import com.nuxora.app.core.ScriptGenerator;
import com.nuxora.app.data.database.DistroDatabaseHelper;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.service.LinuxService;
import com.nuxora.app.ui.MainActivity;
import com.nuxora.app.utils.ShizukuHelper;
import com.nuxora.app.utils.StoragePermissionHelper;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TerminalActivity extends AppCompatActivity implements TerminalSessionClient, TerminalViewClient {

    private static final String TAG = "NuxoraTerminal";
    public static final String EXTRA_DISTRO_ID = "distro_id";

    // Drawer and Main Views
    private DrawerLayout drawerLayout;
    private TextView tvTerminalTitle;
    private TerminalView terminalView;
    private HorizontalScrollView keyToolbarScrollView;
    private MaterialButton btnOpenDrawer;
    private MaterialButton btnHome;
    private MaterialButton btnToggleKeyboard;
    private MaterialButton btnClearTerminal;
    private MaterialButton btnCloseTerminal;

    // Drawer Views
    private TextView tvDrawerSessionCount;
    private MaterialButton btnCloseDrawer;
    private MaterialButton btnDrawerNewSession;
    private LinearLayout llDrawerSessions;
    private LinearLayout llDrawerDistros;
    private MaterialButton btnFontDecrease;
    private MaterialButton btnFontIncrease;
    private TextView tvFontSizeDisplay;
    private MaterialSwitch switchExtraKeys;
    private MaterialSwitch switchScreenWake;
    private MaterialButton btnDrawerClearScreen;
    private MaterialButton btnDrawerBackHome;
    private MaterialButton btnDrawerStopTerminal;

    // Key Toolbar keys
    private TextView keyCtrl;
    private TextView keyAlt;
    private boolean ctrlKeyActive = false;
    private boolean altKeyActive = false;

    // Font size settings
    private static final int MIN_FONT_SIZE_SP = 8;
    private static final int MAX_FONT_SIZE_SP = 34;
    private static final int DEFAULT_FONT_SIZE_SP = 13;
    private int currentFontSizeSp = DEFAULT_FONT_SIZE_SP;

    // Multi-Session Management
    public static class SessionTab {
        public final int id;
        public final InstalledDistro distro;
        public final TerminalSession session;
        public String title;

        public SessionTab(int id, InstalledDistro distro, TerminalSession session) {
            this.id = id;
            this.distro = distro;
            this.session = session;
            this.title = "Sesi " + id + ": " + distro.getName();
        }
    }

    private final List<SessionTab> activeSessions = new ArrayList<>();
    private int currentSessionIndex = 0;
    private int sessionCounter = 0;

    // Current active session references
    private InstalledDistro distro;
    private TerminalSession terminalSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        long distroId = getIntent().getLongExtra(EXTRA_DISTRO_ID, -1);
        if (distroId != -1) {
            distro = DistroDatabaseHelper.getInstance(this).getDistroById(distroId);
        }

        if (distro == null) {
            List<InstalledDistro> all = DistroDatabaseHelper.getInstance(this).getAllInstalledDistros();
            if (!all.isEmpty()) {
                distro = all.get(0);
            }
        }

        if (distro == null) {
            Toast.makeText(this, "Tidak ada distro Linux yang terpasang.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupKeyToolbar();
        setupDrawerListeners();

        // Start first session
        startNewSession(distro);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        long distroId = intent.getLongExtra(EXTRA_DISTRO_ID, -1);
        if (distroId != -1) {
            InstalledDistro target = DistroDatabaseHelper.getInstance(this).getDistroById(distroId);
            if (target != null) {
                // Check if session for this distro already exists
                int existingIdx = -1;
                for (int i = 0; i < activeSessions.size(); i++) {
                    if (activeSessions.get(i).distro.getId() == target.getId()) {
                        existingIdx = i;
                        break;
                    }
                }
                if (existingIdx != -1) {
                    switchToSession(existingIdx);
                } else {
                    startNewSession(target);
                }
            }
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        tvTerminalTitle = findViewById(R.id.tvTerminalTitle);
        terminalView = findViewById(R.id.terminalView);
        keyToolbarScrollView = findViewById(R.id.keyToolbarScrollView);
        btnOpenDrawer = findViewById(R.id.btnOpenDrawer);
        btnHome = findViewById(R.id.btnHome);
        btnToggleKeyboard = findViewById(R.id.btnToggleKeyboard);
        btnClearTerminal = findViewById(R.id.btnClearTerminal);
        btnCloseTerminal = findViewById(R.id.btnCloseTerminal);

        // Drawer elements
        tvDrawerSessionCount = findViewById(R.id.tvDrawerSessionCount);
        btnCloseDrawer = findViewById(R.id.btnCloseDrawer);
        btnDrawerNewSession = findViewById(R.id.btnDrawerNewSession);
        llDrawerSessions = findViewById(R.id.llDrawerSessions);
        llDrawerDistros = findViewById(R.id.llDrawerDistros);
        btnFontDecrease = findViewById(R.id.btnFontDecrease);
        btnFontIncrease = findViewById(R.id.btnFontIncrease);
        tvFontSizeDisplay = findViewById(R.id.tvFontSizeDisplay);
        switchExtraKeys = findViewById(R.id.switchExtraKeys);
        switchScreenWake = findViewById(R.id.switchScreenWake);
        btnDrawerClearScreen = findViewById(R.id.btnDrawerClearScreen);
        btnDrawerBackHome = findViewById(R.id.btnDrawerBackHome);
        btnDrawerStopTerminal = findViewById(R.id.btnDrawerStopTerminal);

        currentFontSizeSp = getSharedPreferences("nuxora_prefs", MODE_PRIVATE)
                .getInt("terminal_font_size", DEFAULT_FONT_SIZE_SP);
        tvFontSizeDisplay.setText(currentFontSizeSp + " sp");

        // Top bar listeners
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnHome.setOnClickListener(v -> navigateToHome());
        btnToggleKeyboard.setOnClickListener(v -> toggleKeyboard());
        btnClearTerminal.setOnClickListener(v -> resetCurrentTerminal());
        btnCloseTerminal.setOnClickListener(v -> showExitConfirmationDialog());
    }

    private void setupDrawerListeners() {
        btnCloseDrawer.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));
        btnDrawerNewSession.setOnClickListener(v -> showNewSessionDistroPicker());

        btnFontDecrease.setOnClickListener(v -> changeFontSize(false));
        btnFontIncrease.setOnClickListener(v -> changeFontSize(true));

        switchExtraKeys.setOnCheckedChangeListener((buttonView, isChecked) -> {
            keyToolbarScrollView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        switchScreenWake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        btnDrawerClearScreen.setOnClickListener(v -> {
            resetCurrentTerminal();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        btnDrawerBackHome.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateToHome();
        });

        btnDrawerStopTerminal.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            showExitConfirmationDialog();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void resetCurrentTerminal() {
        if (terminalSession != null && terminalSession.getEmulator() != null) {
            terminalSession.reset();
            Toast.makeText(this, R.string.terminal_cleared, Toast.LENGTH_SHORT).show();
        }
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sesi Terminal Linux")
                .setMessage("Apakah Anda ingin kembali ke beranda dan membiarkan Linux tetap berjalan di latar belakang, atau menghentikan sesi?")
                .setPositiveButton("Latar Belakang", (dialog, which) -> {
                    navigateToHome();
                })
                .setNegativeButton("Hentikan Sesi Ini", (dialog, which) -> {
                    closeSession(currentSessionIndex);
                })
                .setNeutralButton("Batal", null)
                .show();
    }

    private void setupKeyToolbar() {
        TextView keyEsc = findViewById(R.id.keyEsc);
        TextView keyTab = findViewById(R.id.keyTab);
        keyCtrl = findViewById(R.id.keyCtrl);
        keyAlt = findViewById(R.id.keyAlt);
        TextView keyUp = findViewById(R.id.keyUp);
        TextView keyDown = findViewById(R.id.keyDown);
        TextView keyLeft = findViewById(R.id.keyLeft);
        TextView keyRight = findViewById(R.id.keyRight);
        TextView keyCtrlC = findViewById(R.id.keyCtrlC);
        TextView keyCtrlD = findViewById(R.id.keyCtrlD);
        TextView keySlash = findViewById(R.id.keySlash);
        TextView keyMinus = findViewById(R.id.keyMinus);
        TextView keyPipe = findViewById(R.id.keyPipe);
        TextView keyTilde = findViewById(R.id.keyTilde);

        keyEsc.setOnClickListener(v -> sendBytes(new byte[]{27}));
        keyTab.setOnClickListener(v -> sendBytes(new byte[]{9}));

        keyCtrl.setOnClickListener(v -> {
            ctrlKeyActive = !ctrlKeyActive;
            updateKeyBarUi();
        });

        keyAlt.setOnClickListener(v -> {
            altKeyActive = !altKeyActive;
            updateKeyBarUi();
        });

        // VT100 / ANSI Cursor key sequences
        keyUp.setOnClickListener(v -> sendBytes(new byte[]{27, '[', 'A'}));
        keyDown.setOnClickListener(v -> sendBytes(new byte[]{27, '[', 'B'}));
        keyRight.setOnClickListener(v -> sendBytes(new byte[]{27, '[', 'C'}));
        keyLeft.setOnClickListener(v -> sendBytes(new byte[]{27, '[', 'D'}));

        keyCtrlC.setOnClickListener(v -> sendBytes(new byte[]{3}));
        keyCtrlD.setOnClickListener(v -> sendBytes(new byte[]{4}));

        keySlash.setOnClickListener(v -> sendBytes(new byte[]{'/'}));
        keyMinus.setOnClickListener(v -> sendBytes(new byte[]{'-'}));
        keyPipe.setOnClickListener(v -> sendBytes(new byte[]{'|'}));
        keyTilde.setOnClickListener(v -> sendBytes(new byte[]{'~'}));
    }

    private void updateKeyBarUi() {
        if (keyCtrl != null) {
            keyCtrl.setBackgroundResource(ctrlKeyActive ? R.drawable.bg_terminal_key_active : R.drawable.bg_terminal_key);
            keyCtrl.setTextColor(ctrlKeyActive ? Color.BLACK : Color.parseColor("#F0F6FC"));
        }
        if (keyAlt != null) {
            keyAlt.setBackgroundResource(altKeyActive ? R.drawable.bg_terminal_key_active : R.drawable.bg_terminal_key);
            keyAlt.setTextColor(altKeyActive ? Color.BLACK : Color.parseColor("#F0F6FC"));
        }
    }

    private void changeFontSize(boolean increase) {
        if (increase) {
            if (currentFontSizeSp < MAX_FONT_SIZE_SP) currentFontSizeSp++;
        } else {
            if (currentFontSizeSp > MIN_FONT_SIZE_SP) currentFontSizeSp--;
        }
        getSharedPreferences("nuxora_prefs", MODE_PRIVATE)
                .edit()
                .putInt("terminal_font_size", currentFontSizeSp)
                .apply();
        tvFontSizeDisplay.setText(currentFontSizeSp + " sp");
        if (terminalView != null) {
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp, getResources().getDisplayMetrics());
            terminalView.setTextSize(px);
        }
    }

    private void sendBytes(byte[] bytes) {
        if (terminalSession != null) {
            terminalSession.write(bytes, 0, bytes.length);
        }
    }

    private void toggleKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && terminalView != null) {
            terminalView.requestFocus();
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    // ==================== Multi-Session Management ====================

    private void startNewSession(InstalledDistro targetDistro) {
        try {
            // If storage permission or Shizuku permission is granted, ensure distro reflects it
            boolean hasStorage = StoragePermissionHelper.hasStoragePermission(this) ||
                    ShizukuHelper.hasShizukuPermission();
            if (hasStorage && !targetDistro.isMountSdcard()) {
                targetDistro.setMountSdcard(true);
                DistroDatabaseHelper.getInstance(this).updateMountSdcard(targetDistro.getId(), true);
            }
            if (ShizukuHelper.hasShizukuPermission()) {
                ShizukuHelper.grantStoragePermissionsViaShizukuAsync(this, null);
            }

            String shellPath;
            String cwd = targetDistro.getRootfsPath();
            String[] args;
            String[] env;

            if (targetDistro.getExecutionMode() == ExecutionMode.ROOT) {
                ChrootManager chrootManager = new ChrootManager(this);
                File internalBin = new File(getFilesDir(), "bin");
                File script = ScriptGenerator.createChrootLaunchScript(internalBin, new File(targetDistro.getRootfsPath()), targetDistro);
                shellPath = "su";
                args = new String[]{"su", "-c", "/system/bin/sh " + script.getAbsolutePath()};
                env = new String[]{
                        "TERM=xterm-256color",
                        "HOME=/root",
                        "USER=root",
                        "LOGNAME=root",
                        "LANG=C.UTF-8"
                };
            } else {
                PRootManager prootManager = new PRootManager(this);
                File script = ScriptGenerator.createProotLaunchScript(prootManager, new File(targetDistro.getRootfsPath()), targetDistro);
                shellPath = "/system/bin/sh";
                args = new String[]{"sh", script.getAbsolutePath()};
                env = new String[]{
                        "TERM=xterm-256color",
                        "HOME=/root",
                        "USER=root",
                        "LOGNAME=root",
                        "LANG=C.UTF-8",
                        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
                };
            }

            TerminalSession session = new TerminalSession(shellPath, cwd, args, env, 2000, this);
            sessionCounter++;
            SessionTab tab = new SessionTab(sessionCounter, targetDistro, session);
            activeSessions.add(tab);

            DistroDatabaseHelper.getInstance(this).updateRunningStatus(targetDistro.getId(), true);
            LinuxService.startTerminal(this, targetDistro.getId(), targetDistro.getName());

            switchToSession(activeSessions.size() - 1);

        } catch (Exception e) {
            Log.e(TAG, "Gagal memulai sesi Linux", e);
            Toast.makeText(this, "Gagal memulai sesi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void switchToSession(int index) {
        if (index < 0 || index >= activeSessions.size()) return;
        currentSessionIndex = index;
        SessionTab current = activeSessions.get(index);
        this.distro = current.distro;
        this.terminalSession = current.session;

        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp, getResources().getDisplayMetrics());
        terminalView.setTextSize(px);
        terminalView.setTypeface(Typeface.MONOSPACE);
        terminalView.setTerminalViewClient(this);
        terminalView.attachSession(terminalSession);

        updateTopBarTitle();
        updateDrawerSessionsList();
        updateDrawerDistrosList();

        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        terminalView.post(() -> {
            terminalView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void closeSession(int index) {
        if (index < 0 || index >= activeSessions.size()) return;
        SessionTab closing = activeSessions.get(index);
        if (closing.session != null) {
            closing.session.finishIfRunning();
        }
        activeSessions.remove(index);

        // Check if distro still used in other sessions
        boolean distroStillUsed = false;
        for (SessionTab tab : activeSessions) {
            if (tab.distro.getId() == closing.distro.getId()) {
                distroStillUsed = true;
                break;
            }
        }
        if (!distroStillUsed) {
            if (closing.distro.getExecutionMode() == ExecutionMode.ROOT) {
                new ChrootManager(this).stopChroot(closing.distro);
            }
            DistroDatabaseHelper.getInstance(this).updateRunningStatus(closing.distro.getId(), false);
        }

        if (activeSessions.isEmpty()) {
            LinuxService.stopTerminal(this);
            finish();
        } else {
            if (currentSessionIndex >= activeSessions.size()) {
                currentSessionIndex = activeSessions.size() - 1;
            }
            switchToSession(currentSessionIndex);
        }
    }

    private void showNewSessionDistroPicker() {
        List<InstalledDistro> allDistros = DistroDatabaseHelper.getInstance(this).getAllInstalledDistros();
        if (allDistros.isEmpty()) {
            Toast.makeText(this, "Tidak ada distro terpasang.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (allDistros.size() == 1) {
            startNewSession(allDistros.get(0));
            return;
        }

        String[] items = new String[allDistros.size()];
        for (int i = 0; i < allDistros.size(); i++) {
            InstalledDistro d = allDistros.get(i);
            items[i] = d.getName() + " (" + d.getExecutionMode().getDisplayName() + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Pilih Distro untuk Sesi Baru")
                .setItems(items, (dialog, which) -> {
                    startNewSession(allDistros.get(which));
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateTopBarTitle() {
        if (activeSessions.isEmpty()) {
            tvTerminalTitle.setText("Nuxora Terminal");
            return;
        }
        SessionTab current = activeSessions.get(currentSessionIndex);
        String title = "[" + (currentSessionIndex + 1) + "/" + activeSessions.size() + "] "
                + current.distro.getName() + " (" + current.distro.getExecutionMode().getDisplayName() + ")";
        tvTerminalTitle.setText(title);
        tvDrawerSessionCount.setText(activeSessions.size() + " sesi aktif");
    }

    private void updateDrawerSessionsList() {
        if (llDrawerSessions == null) return;
        llDrawerSessions.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < activeSessions.size(); i++) {
            final int index = i;
            SessionTab tab = activeSessions.get(i);

            View itemView = inflater.inflate(R.layout.item_terminal_session, llDrawerSessions, false);
            View sessionContainer = itemView.findViewById(R.id.sessionContainer);
            ImageView ivStatus = itemView.findViewById(R.id.ivSessionStatus);
            TextView tvName = itemView.findViewById(R.id.tvSessionName);
            TextView tvMode = itemView.findViewById(R.id.tvSessionMode);
            MaterialButton btnClose = itemView.findViewById(R.id.btnCloseSessionItem);

            tvName.setText("#" + tab.id + " • " + tab.distro.getName());

            boolean isCurrent = (i == currentSessionIndex);
            if (isCurrent) {
                sessionContainer.setBackgroundResource(R.drawable.bg_session_item_active);
                ivStatus.setColorFilter(Color.parseColor("#38BDF8"));
                tvMode.setText(tab.distro.getExecutionMode().getDisplayName() + " • Sedang Dibuka");
                tvMode.setTextColor(Color.parseColor("#38BDF8"));
            } else {
                sessionContainer.setBackgroundResource(R.drawable.bg_session_item);
                ivStatus.setColorFilter(Color.parseColor("#64748B"));
                tvMode.setText(tab.distro.getExecutionMode().getDisplayName() + " • Latar Belakang");
                tvMode.setTextColor(Color.parseColor("#94A3B8"));
            }

            sessionContainer.setOnClickListener(v -> switchToSession(index));
            btnClose.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Tutup Sesi")
                        .setMessage("Apakah Anda yakin ingin menutup sesi #" + tab.id + " (" + tab.distro.getName() + ")?")
                        .setPositiveButton("Tutup", (dialog, which) -> closeSession(index))
                        .setNegativeButton("Batal", null)
                        .show();
            });

            llDrawerSessions.addView(itemView);
        }
    }

    private void updateDrawerDistrosList() {
        if (llDrawerDistros == null) return;
        llDrawerDistros.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        List<InstalledDistro> allDistros = DistroDatabaseHelper.getInstance(this).getAllInstalledDistros();
        if (allDistros.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("Belum ada distro lain terpasang.");
            tvEmpty.setTextColor(Color.parseColor("#64748B"));
            tvEmpty.setTextSize(12);
            llDrawerDistros.addView(tvEmpty);
            return;
        }

        for (InstalledDistro item : allDistros) {
            View view = inflater.inflate(R.layout.item_drawer_distro, llDrawerDistros, false);
            TextView tvName = view.findViewById(R.id.tvDrawerDistroName);
            TextView tvDetails = view.findViewById(R.id.tvDrawerDistroDetails);
            MaterialButton btnOpen = view.findViewById(R.id.btnOpenDrawerDistro);

            tvName.setText(item.getName());
            tvDetails.setText(item.getExecutionMode().getDisplayName() + " • " + item.getDiskUsage());

            View.OnClickListener clickListener = v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startNewSession(item);
            };

            view.setOnClickListener(clickListener);
            btnOpen.setOnClickListener(clickListener);

            llDrawerDistros.addView(view);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Keep terminal running in background and smoothly return to MainActivity
            navigateToHome();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            for (SessionTab tab : activeSessions) {
                if (tab.session != null) {
                    tab.session.finishIfRunning();
                }
                if (tab.distro != null) {
                    if (tab.distro.getExecutionMode() == ExecutionMode.ROOT) {
                        new ChrootManager(this).stopChroot(tab.distro);
                    }
                    DistroDatabaseHelper.getInstance(this).updateRunningStatus(tab.distro.getId(), false);
                }
            }
            activeSessions.clear();
            LinuxService.stopTerminal(this);
        }
    }

    // ==================== TerminalSessionClient Callbacks ====================

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        if (changedSession == terminalSession) {
            terminalView.onScreenUpdated();
        }
    }

    @Override
    public void onTitleChanged(TerminalSession changedSession) {
        runOnUiThread(() -> {
            if (changedSession == terminalSession && changedSession.getTitle() != null && !changedSession.getTitle().isEmpty()) {
                tvTerminalTitle.setText("[" + (currentSessionIndex + 1) + "/" + activeSessions.size() + "] "
                        + distro.getName() + " - " + changedSession.getTitle());
            }
        });
    }

    @Override
    public void onSessionFinished(TerminalSession finishedSession) {
        runOnUiThread(() -> {
            int finishedIndex = -1;
            for (int i = 0; i < activeSessions.size(); i++) {
                if (activeSessions.get(i).session == finishedSession) {
                    finishedIndex = i;
                    break;
                }
            }
            if (finishedIndex != -1) {
                SessionTab finishedTab = activeSessions.get(finishedIndex);
                Toast.makeText(this, "Sesi #" + finishedTab.id + " (" + finishedTab.distro.getName() + ") telah berakhir.", Toast.LENGTH_SHORT).show();
                closeSession(finishedIndex);
            } else if (activeSessions.isEmpty()) {
                finish();
            }
        });
    }

    @Override
    public void onCopyTextToClipboard(TerminalSession session, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && text != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal", text));
            Toast.makeText(this, "Teks disalin", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPasteTextFromClipboard(TerminalSession session) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
            CharSequence text = clipboard.getPrimaryClip().getItemAt(0).getText();
            if (text != null) {
                session.write(text.toString());
            }
        }
    }

    @Override
    public void onBell(TerminalSession session) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(50);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onColorsChanged(TerminalSession session) {}

    @Override
    public void onTerminalCursorStateChange(boolean state) {}

    @Override
    public Integer getTerminalCursorStyle() {
        return null;
    }

    // ==================== TerminalViewClient Callbacks ====================

    @Override
    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            boolean increase = scale > 1.0f;
            changeFontSize(increase);
            return 1.0f;
        }
        return scale;
    }

    @Override
    public void onSingleTapUp(MotionEvent e) {
        toggleKeyboard();
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return false;
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return true;
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return false;
    }

    @Override
    public boolean isTerminalViewSelected() {
        return true;
    }

    @Override
    public void copyModeChanged(boolean copyMode) {}

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return false;
    }

    @Override
    public boolean onLongPress(MotionEvent event) {
        return false;
    }

    @Override
    public boolean readControlKey() {
        boolean ctrl = ctrlKeyActive;
        ctrlKeyActive = false;
        updateKeyBarUi();
        return ctrl;
    }

    @Override
    public boolean readAltKey() {
        boolean alt = altKeyActive;
        altKeyActive = false;
        updateKeyBarUi();
        return alt;
    }

    @Override
    public boolean readShiftKey() {
        return false;
    }

    @Override
    public boolean readFnKey() {
        return false;
    }

    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession currentSession) {
        return false;
    }

    @Override
    public void onEmulatorSet() {}

    // ==================== Logging Callbacks ====================

    @Override
    public void logError(String tag, String message) {
        Log.e(tag, message);
    }

    @Override
    public void logWarn(String tag, String message) {
        Log.w(tag, message);
    }

    @Override
    public void logInfo(String tag, String message) {
        Log.i(tag, message);
    }

    @Override
    public void logDebug(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void logVerbose(String tag, String message) {
        Log.v(tag, message);
    }

    @Override
    public void logStackTraceWithMessage(String tag, String message, Exception e) {
        Log.e(tag, message, e);
    }

    @Override
    public void logStackTrace(String tag, Exception e) {
        Log.e(tag, "Stack trace:", e);
    }
}

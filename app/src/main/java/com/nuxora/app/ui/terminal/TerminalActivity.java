package com.nuxora.app.ui.terminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nuxora.app.R;
import com.nuxora.app.core.ChrootManager;
import com.nuxora.app.core.PRootManager;
import com.nuxora.app.core.ScriptGenerator;
import com.nuxora.app.service.LinuxService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nuxora.app.data.database.DistroDatabaseHelper;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.google.android.material.button.MaterialButton;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;

import java.io.File;

public class TerminalActivity extends AppCompatActivity implements TerminalSessionClient, TerminalViewClient {

    private static final String TAG = "NuxoraTerminal";
    public static final String EXTRA_DISTRO_ID = "distro_id";

    private TextView tvTerminalTitle;
    private TerminalView terminalView;
    private TerminalSession terminalSession;

    private MaterialButton btnToggleKeyboard;
    private MaterialButton btnClearTerminal;
    private MaterialButton btnCloseTerminal;

    private TextView keyCtrl;
    private TextView keyAlt;
    private boolean ctrlKeyActive = false;
    private boolean altKeyActive = false;

    private static final int MIN_FONT_SIZE_SP = 8;
    private static final int MAX_FONT_SIZE_SP = 34;
    private static final int DEFAULT_FONT_SIZE_SP = 13;
    private int currentFontSizeSp = DEFAULT_FONT_SIZE_SP;

    private InstalledDistro distro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        long distroId = getIntent().getLongExtra(EXTRA_DISTRO_ID, -1);
        if (distroId != -1) {
            distro = DistroDatabaseHelper.getInstance(this).getDistroById(distroId);
        }

        if (distro == null) {
            Toast.makeText(this, "Distro tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupKeyToolbar();
        startLinuxSession();
    }

    private void initViews() {
        tvTerminalTitle = findViewById(R.id.tvTerminalTitle);
        terminalView = findViewById(R.id.terminalView);
        btnToggleKeyboard = findViewById(R.id.btnToggleKeyboard);
        btnClearTerminal = findViewById(R.id.btnClearTerminal);
        btnCloseTerminal = findViewById(R.id.btnCloseTerminal);

        tvTerminalTitle.setText(distro.getName() + " (" + distro.getExecutionMode().getDisplayName() + ")");

        btnToggleKeyboard.setOnClickListener(v -> toggleKeyboard());

        btnClearTerminal.setOnClickListener(v -> {
            if (terminalSession != null && terminalSession.getEmulator() != null) {
                terminalSession.reset();
                Toast.makeText(this, R.string.terminal_cleared, Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseTerminal.setOnClickListener(v -> showExitConfirmationDialog());
    }

    private void showExitConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sesi Terminal")
                .setMessage("Apakah Anda ingin menutup sesi Linux atau membiarkannya tetap berjalan di latar belakang?")
                .setPositiveButton("Latar Belakang", (dialog, which) -> {
                    moveTaskToBack(true);
                })
                .setNegativeButton("Hentikan Sesi", (dialog, which) -> {
                    if (terminalSession != null) {
                        terminalSession.finishIfRunning();
                    }
                    LinuxService.stopTerminal(this);
                    finish();
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

    private void startLinuxSession() {
        try {
            // If storage permission or Shizuku permission is granted, ensure distro reflects it
            boolean hasStorage = com.nuxora.app.utils.StoragePermissionHelper.hasStoragePermission(this) ||
                    com.nuxora.app.utils.ShizukuHelper.hasShizukuPermission();
            if (hasStorage && !distro.isMountSdcard()) {
                distro.setMountSdcard(true);
                DistroDatabaseHelper.getInstance(this).updateMountSdcard(distro.getId(), true);
            }
            if (com.nuxora.app.utils.ShizukuHelper.hasShizukuPermission()) {
                com.nuxora.app.utils.ShizukuHelper.grantStoragePermissionsViaShizukuAsync(this, null);
            }

            String shellPath;
            String cwd = distro.getRootfsPath();
            String[] args;
            String[] env;

            if (distro.getExecutionMode() == ExecutionMode.ROOT) {
                ChrootManager chrootManager = new ChrootManager(this);
                File internalBin = new File(getFilesDir(), "bin");
                File script = ScriptGenerator.createChrootLaunchScript(internalBin, new File(distro.getRootfsPath()), distro);
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
                File script = ScriptGenerator.createProotLaunchScript(prootManager, new File(distro.getRootfsPath()), distro);
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

            terminalSession = new TerminalSession(shellPath, cwd, args, env, 2000, this);
            currentFontSizeSp = getSharedPreferences("nuxora_prefs", MODE_PRIVATE)
                    .getInt("terminal_font_size", DEFAULT_FONT_SIZE_SP);
            terminalView.setTextSize((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp, getResources().getDisplayMetrics()));
            terminalView.setTypeface(Typeface.MONOSPACE);
            terminalView.setTerminalViewClient(this);
            terminalView.attachSession(terminalSession);

            DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), true);
            LinuxService.startTerminal(this, distro.getId(), distro.getName());

            terminalView.post(() -> {
                terminalView.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Gagal memulai sesi Linux", e);
            Toast.makeText(this, "Gagal memulai sesi: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Keep terminal running in background when pressing back, like Termux
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            if (terminalSession != null) {
                terminalSession.finishIfRunning();
            }
            if (distro != null) {
                if (distro.getExecutionMode() == ExecutionMode.ROOT) {
                    new ChrootManager(this).stopChroot(distro);
                }
                DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), false);
            }
            LinuxService.stopTerminal(this);
        }
    }

    // ==================== TerminalSessionClient Callbacks ====================

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        terminalView.onScreenUpdated();
    }

    @Override
    public void onTitleChanged(TerminalSession changedSession) {
        runOnUiThread(() -> {
            if (changedSession.getTitle() != null && !changedSession.getTitle().isEmpty()) {
                tvTerminalTitle.setText(distro.getName() + " - " + changedSession.getTitle());
            }
        });
    }

    @Override
    public void onSessionFinished(TerminalSession finishedSession) {
        runOnUiThread(() -> {
            int exitCode = finishedSession.getExitStatus();
            Log.i(TAG, "Sesi Linux selesai dengan kode exit: " + exitCode);
            if (distro != null) {
                if (distro.getExecutionMode() == ExecutionMode.ROOT) {
                    new ChrootManager(this).stopChroot(distro);
                }
                DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), false);
            }
            LinuxService.stopTerminal(this);
            Toast.makeText(this, "Sesi Linux telah berakhir (exit: " + exitCode + ")", Toast.LENGTH_SHORT).show();
            finish();
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

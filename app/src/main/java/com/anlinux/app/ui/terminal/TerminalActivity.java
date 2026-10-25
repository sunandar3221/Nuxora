package com.anlinux.app.ui.terminal;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anlinux.app.R;
import com.anlinux.app.core.ChrootManager;
import com.anlinux.app.core.PRootManager;
import com.anlinux.app.core.ProcessRunner;
import com.anlinux.app.data.database.DistroDatabaseHelper;
import com.anlinux.app.data.model.ExecutionMode;
import com.anlinux.app.data.model.InstalledDistro;
import com.anlinux.app.utils.AnsiColorParser;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class TerminalActivity extends AppCompatActivity implements ProcessRunner.ProcessListener {

    public static final String EXTRA_DISTRO_ID = "distro_id";

    private TextView tvTerminalTitle;
    private TextView tvTerminalOutput;
    private ScrollView terminalScrollView;
    private EditText etTerminalInput;
    private MaterialButton btnTerminalSend;
    private MaterialButton btnClearTerminal;
    private MaterialButton btnCloseTerminal;

    private InstalledDistro distro;
    private ProcessRunner processRunner;
    private final StringBuilder outputBuffer = new StringBuilder();

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
        tvTerminalOutput = findViewById(R.id.tvTerminalOutput);
        terminalScrollView = findViewById(R.id.terminalScrollView);
        etTerminalInput = findViewById(R.id.etTerminalInput);
        btnTerminalSend = findViewById(R.id.btnTerminalSend);
        btnClearTerminal = findViewById(R.id.btnClearTerminal);
        btnCloseTerminal = findViewById(R.id.btnCloseTerminal);

        tvTerminalTitle.setText(distro.getName() + " (" + distro.getExecutionMode().getDisplayName() + ")");

        btnTerminalSend.setOnClickListener(v -> sendCommandFromInput());

        etTerminalInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendCommandFromInput();
                return true;
            }
            return false;
        });

        btnClearTerminal.setOnClickListener(v -> {
            outputBuffer.setLength(0);
            tvTerminalOutput.setText("");
            Toast.makeText(this, R.string.terminal_cleared, Toast.LENGTH_SHORT).show();
        });

        btnCloseTerminal.setOnClickListener(v -> {
            if (processRunner != null) {
                processRunner.sendCtrlD();
                processRunner.destroy();
            }
            finish();
        });
    }

    private void setupKeyToolbar() {
        Button keyEsc = findViewById(R.id.keyEsc);
        Button keyTab = findViewById(R.id.keyTab);
        Button keyCtrlC = findViewById(R.id.keyCtrlC);
        Button keyCtrlD = findViewById(R.id.keyCtrlD);
        Button keyUp = findViewById(R.id.keyUp);
        Button keyDown = findViewById(R.id.keyDown);
        Button keyLeft = findViewById(R.id.keyLeft);
        Button keyRight = findViewById(R.id.keyRight);
        Button keyPipe = findViewById(R.id.keyPipe);
        Button keySlash = findViewById(R.id.keySlash);
        Button keyTilde = findViewById(R.id.keyTilde);

        keyEsc.setOnClickListener(v -> { if (processRunner != null) processRunner.sendEsc(); });
        keyTab.setOnClickListener(v -> { if (processRunner != null) processRunner.sendTab(); });
        keyCtrlC.setOnClickListener(v -> { if (processRunner != null) processRunner.sendCtrlC(); });
        keyCtrlD.setOnClickListener(v -> { if (processRunner != null) processRunner.sendCtrlD(); });
        keyUp.setOnClickListener(v -> { if (processRunner != null) processRunner.sendUpArrow(); });
        keyDown.setOnClickListener(v -> { if (processRunner != null) processRunner.sendDownArrow(); });
        keyLeft.setOnClickListener(v -> { if (processRunner != null) processRunner.sendLeftArrow(); });
        keyRight.setOnClickListener(v -> { if (processRunner != null) processRunner.sendRightArrow(); });
        keyPipe.setOnClickListener(v -> insertTextToInput("|"));
        keySlash.setOnClickListener(v -> insertTextToInput("/"));
        keyTilde.setOnClickListener(v -> insertTextToInput("~"));
    }

    private void insertTextToInput(String text) {
        if (etTerminalInput != null) {
            int start = Math.max(etTerminalInput.getSelectionStart(), 0);
            int end = Math.max(etTerminalInput.getSelectionEnd(), 0);
            etTerminalInput.getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
        }
    }

    private void sendCommandFromInput() {
        if (etTerminalInput == null || processRunner == null) return;
        String cmd = etTerminalInput.getText().toString();
        processRunner.sendInput(cmd + "\n");
        etTerminalInput.setText("");
    }

    private void startLinuxSession() {
        processRunner = new ProcessRunner(this);
        outputBuffer.append("Menghubungkan ke sesi Linux ").append(distro.getName()).append("…\n\n");
        tvTerminalOutput.setText(outputBuffer.toString());

        new Thread(() -> {
            try {
                String[] command;
                if (distro.getExecutionMode() == ExecutionMode.ROOT) {
                    ChrootManager chrootManager = new ChrootManager(this);
                    command = chrootManager.buildCommand(distro);
                } else {
                    PRootManager prootManager = new PRootManager(this);
                    command = prootManager.buildCommand(distro);
                }

                File workingDir = new File(distro.getRootfsPath());
                processRunner.start(command, workingDir, null);
                DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), true);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    onOutput("\n[ERROR GAGAL MENJALANKAN SESI]: " + e.getMessage() + "\n");
                });
            }
        }).start();
    }

    @Override
    public void onOutput(String text) {
        if (outputBuffer.length() > 50000) {
            outputBuffer.delete(0, 10000);
        }
        outputBuffer.append(text);
        CharSequence colored = AnsiColorParser.parse(outputBuffer.toString());
        tvTerminalOutput.setText(colored);
        terminalScrollView.post(() -> terminalScrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onProcessExit(int exitCode) {
        onOutput("\n\n[Sesi Linux dihentikan dengan kode: " + exitCode + "]\n");
        DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), false);
    }

    @Override
    protected void onDestroy() {
        if (processRunner != null) {
            processRunner.destroy();
        }
        if (distro != null) {
            DistroDatabaseHelper.getInstance(this).updateRunningStatus(distro.getId(), false);
        }
        super.onDestroy();
    }
}

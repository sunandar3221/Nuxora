package com.anlinux.app.core;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessRunner {

    public interface ProcessListener {
        void onOutput(String text);
        void onProcessExit(int exitCode);
    }

    private Process process;
    private OutputStream outputStream;
    private final ProcessListener listener;
    private final Handler mainHandler;
    private final ExecutorService executorService;
    private volatile boolean isRunning = false;

    public ProcessRunner(ProcessListener listener) {
        this.listener = listener;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public synchronized void start(String[] command, File workingDir, Map<String, String> extraEnv) throws Exception {
        if (isRunning) {
            destroy();
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null && workingDir.exists()) {
            pb.directory(workingDir);
        }
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.put("TERM", "xterm-256color");
        env.put("LANG", "C.UTF-8");
        if (extraEnv != null) {
            env.putAll(extraEnv);
        }

        process = pb.start();
        outputStream = process.getOutputStream();
        isRunning = true;

        // Read stdout and stderr combined
        executorService.execute(() -> {
            byte[] buffer = new byte[4096];
            try (InputStream in = process.getInputStream()) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    postOutput(text);
                }
            } catch (Exception ignored) {
            } finally {
                int exitVal = -1;
                try {
                    exitVal = process.waitFor();
                } catch (InterruptedException ignored) {}
                isRunning = false;
                postExit(exitVal);
            }
        });
    }

    public synchronized void sendInput(String input) {
        if (outputStream != null && isRunning) {
            executorService.execute(() -> {
                try {
                    outputStream.write(input.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {}
            });
        }
    }

    public synchronized void sendBytes(byte[] bytes) {
        if (outputStream != null && isRunning) {
            executorService.execute(() -> {
                try {
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (Exception ignored) {}
            });
        }
    }

    public void sendEnter() {
        sendInput("\n");
    }

    public void sendTab() {
        sendInput("\t");
    }

    public void sendEsc() {
        sendBytes(new byte[]{0x1b});
    }

    public void sendCtrlC() {
        sendBytes(new byte[]{0x03}); // ASCII ETX (Ctrl+C)
    }

    public void sendCtrlD() {
        sendBytes(new byte[]{0x04}); // ASCII EOT (Ctrl+D)
    }

    public void sendCtrlZ() {
        sendBytes(new byte[]{0x1a}); // ASCII SUB (Ctrl+Z)
    }

    public void sendUpArrow() {
        sendBytes(new byte[]{0x1b, '[', 'A'});
    }

    public void sendDownArrow() {
        sendBytes(new byte[]{0x1b, '[', 'B'});
    }

    public void sendRightArrow() {
        sendBytes(new byte[]{0x1b, '[', 'C'});
    }

    public void sendLeftArrow() {
        sendBytes(new byte[]{0x1b, '[', 'D'});
    }

    public synchronized void destroy() {
        isRunning = false;
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignored) {}
            process = null;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void postOutput(String text) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onOutput(text);
            }
        });
    }

    private void postExit(int exitCode) {
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onProcessExit(exitCode);
            }
        });
    }
}

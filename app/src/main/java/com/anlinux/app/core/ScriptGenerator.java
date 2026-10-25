package com.anlinux.app.core;

import com.anlinux.app.data.model.InstalledDistro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ScriptGenerator {

    /**
     * Fix DNS in rootfs /etc/resolv.conf
     */
    public static void setupDns(File rootfsDir) {
        File etcDir = new File(rootfsDir, "etc");
        if (!etcDir.exists()) {
            etcDir.mkdirs();
        }
        File resolvConf = new File(etcDir, "resolv.conf");
        try {
            if (resolvConf.exists()) {
                resolvConf.delete();
            }
            String dnsContent = "nameserver 8.8.8.8\nnameserver 1.1.1.1\nnameserver 8.8.4.4\n";
            try (FileOutputStream fos = new FileOutputStream(resolvConf)) {
                fos.write(dnsContent.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {}
    }

    /**
     * Prepares helper scripts and welcome profile inside the distro
     */
    public static void setupExtrasScript(File rootfsDir, boolean enableFirstRun) {
        setupDns(rootfsDir);

        File binDir = new File(rootfsDir, "usr/local/bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }

        // Script to easily install essential packages (nano, curl, wget, bash, sudo)
        File extrasScript = new File(binDir, "install-extras");
        String content = "#!/bin/sh\n" +
                "echo \"[AnLinux] Memeriksa koneksi & memasang paket tambahan...\"\n" +
                "if command -v apk >/dev/null 2>&1; then\n" +
                "    echo \"[AnLinux] Terdeteksi Alpine Linux (APK)...\"\n" +
                "    apk update && apk add bash nano curl wget ca-certificates sudo\n" +
                "elif command -v apt-get >/dev/null 2>&1; then\n" +
                "    echo \"[AnLinux] Terdeteksi Debian/Ubuntu (APT)...\"\n" +
                "    apt-get update && apt-get install -y nano curl wget sudo ca-certificates\n" +
                "elif command -v pacman >/dev/null 2>&1; then\n" +
                "    echo \"[AnLinux] Terdeteksi Arch Linux (Pacman)...\"\n" +
                "    pacman -Syu --noconfirm nano curl wget sudo ca-certificates\n" +
                "elif command -v dnf >/dev/null 2>&1; then\n" +
                "    echo \"[AnLinux] Terdeteksi Fedora (DNF)...\"\n" +
                "    dnf install -y nano curl wget sudo ca-certificates\n" +
                "fi\n" +
                "echo \"[AnLinux] Paket penting (nano, curl, bash, sudo) selesai disiapkan!\"\n";

        try (FileOutputStream fos = new FileOutputStream(extrasScript)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
        extrasScript.setExecutable(true, false);

        // Backup in /root/install-extras.sh
        File rootExtras = new File(rootfsDir, "root/install-extras.sh");
        try (FileOutputStream fos = new FileOutputStream(rootExtras)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
        rootExtras.setExecutable(true, false);

        // Create /etc/profile.d/00-anlinux.sh for prompt and first-run
        File profileD = new File(rootfsDir, "etc/profile.d");
        if (!profileD.exists()) {
            profileD.mkdirs();
        }
        File anlinuxProfile = new File(profileD, "00-anlinux.sh");
        StringBuilder sb = new StringBuilder();
        sb.append("# AnLinux Shell Environment\n");
        sb.append("export PS1='\\[\\033[01;32m\\]\\u@\\h\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]# '\n");
        sb.append("export TERM=xterm-256color\n");
        sb.append("export HOME=/root\n");
        sb.append("export USER=root\n");
        sb.append("export LOGNAME=root\n");

        if (enableFirstRun) {
            sb.append("if [ -f /root/.first_run ]; then\n");
            sb.append("    rm -f /root/.first_run\n");
            sb.append("    echo \"=====================================================\"\n");
            sb.append("    echo \"  Selamat Datang di Linux (AnLinux)!\"\n");
            sb.append("    echo \"  Distro siap digunakan dengan hak akses ROOT.\"\n");
            sb.append("    echo \"=====================================================\"\n");
            sb.append("    /usr/local/bin/install-extras\n");
            sb.append("fi\n");
        }

        try (FileOutputStream fos = new FileOutputStream(anlinuxProfile)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}

        if (enableFirstRun) {
            File firstRunFile = new File(rootfsDir, "root/.first_run");
            try {
                firstRunFile.createNewFile();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Generate PRoot startup bash script with nativeLibraryDir support
     */
    public static File createProotLaunchScript(PRootManager prootManager, File rootfsDir, InstalledDistro distro) throws IOException {
        setupDns(rootfsDir);
        setupExtrasScript(rootfsDir, false);
        File internalBinDir = prootManager.getBinDir();
        File script = new File(internalBinDir, "start_" + distro.getId() + ".sh");

        File prootExe = prootManager.getProotExecutable();
        File loaderExe = prootManager.getLoaderExecutable();
        String libPath = prootManager.getLibraryPath();

        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("unset LD_PRELOAD\n");
        sb.append("PROOT=\"").append(prootExe.getAbsolutePath()).append("\"\n");
        sb.append("LOADER=\"").append(loaderExe.getAbsolutePath()).append("\"\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");

        sb.append("export LD_LIBRARY_PATH=\"").append(libPath).append(":$LD_LIBRARY_PATH\"\n");
        sb.append("export PROOT_LOADER=\"$LOADER\"\n");
        sb.append("export PROOT_TMP_DIR=\"$ROOTFS/tmp\"\n");

        sb.append("mkdir -p \"$ROOTFS/tmp\"\n");
        sb.append("chmod 1777 \"$ROOTFS/tmp\" 2>/dev/null\n");

        sb.append("if [ ! -x \"$PROOT\" ]; then\n");
        sb.append("    chmod 755 \"$PROOT\" 2>/dev/null\n");
        sb.append("fi\n");
        sb.append("if [ -f \"$LOADER\" ] && [ ! -x \"$LOADER\" ]; then\n");
        sb.append("    chmod 755 \"$LOADER\" 2>/dev/null\n");
        sb.append("fi\n");

        // Shell detection (e.g. Alpine has /bin/sh, Ubuntu has /bin/bash)
        sb.append("TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("if [ -x \"$ROOTFS/bin/bash\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/bin/bash\"\n");
        sb.append("elif [ -x \"$ROOTFS/usr/bin/bash\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/usr/bin/bash\"\n");
        sb.append("elif [ -x \"$ROOTFS/bin/sh\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("elif [ -x \"$ROOTFS/usr/bin/sh\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/usr/bin/sh\"\n");
        sb.append("fi\n");

        sb.append("exec \"$PROOT\" \\\n");
        sb.append("  --kill-on-exit \\\n");
        sb.append("  --link2symlink \\\n");
        sb.append("  -0 \\\n");
        sb.append("  -r \"$ROOTFS\" \\\n");
        sb.append("  -b /dev \\\n");
        sb.append("  -b /proc \\\n");
        sb.append("  -b /sys \\\n");
        sb.append("  -b /dev/urandom:/dev/random \\\n");

        if (distro.isMountSdcard()) {
            sb.append("  -b /sdcard \\\n");
            sb.append("  -b /storage \\\n");
        }

        sb.append("  -w /root \\\n");
        sb.append("  /usr/bin/env -i \\\n");
        sb.append("  HOME=/root \\\n");
        sb.append("  USER=root \\\n");
        sb.append("  LOGNAME=root \\\n");
        sb.append("  SHELL=\"$TARGET_SHELL\" \\\n");
        sb.append("  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \\\n");
        sb.append("  TERM=xterm-256color \\\n");
        sb.append("  LANG=C.UTF-8 \\\n");
        sb.append("  \"$TARGET_SHELL\" --login -i\n");

        try (FileOutputStream fos = new FileOutputStream(script)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        script.setExecutable(true, false);
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "755", script.getAbsolutePath()}).waitFor();
        } catch (Exception ignored) {}
        return script;
    }

    /**
     * Generate Chroot (Root mode) startup bash script with robust mounts
     */
    public static File createChrootLaunchScript(File internalBinDir, File rootfsDir, InstalledDistro distro) throws IOException {
        setupDns(rootfsDir);
        setupExtrasScript(rootfsDir, false);
        File script = new File(internalBinDir, "start_root_" + distro.getId() + ".sh");

        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");

        sb.append("# Robust mount checks using /proc/mounts\n");
        sb.append("grep -F \"$ROOTFS/proc\" /proc/mounts >/dev/null 2>&1 || mount -t proc proc \"$ROOTFS/proc\"\n");
        sb.append("grep -F \"$ROOTFS/sys\" /proc/mounts >/dev/null 2>&1 || mount -t sysfs sys \"$ROOTFS/sys\"\n");
        sb.append("grep -F \"$ROOTFS/dev\" /proc/mounts >/dev/null 2>&1 || mount -o bind /dev \"$ROOTFS/dev\"\n");
        sb.append("mkdir -p \"$ROOTFS/dev/pts\"\n");
        sb.append("grep -F \"$ROOTFS/dev/pts\" /proc/mounts >/dev/null 2>&1 || mount -t devpts devpts \"$ROOTFS/dev/pts\"\n");

        if (distro.isMountSdcard()) {
            sb.append("mkdir -p \"$ROOTFS/sdcard\"\n");
            sb.append("grep -F \"$ROOTFS/sdcard\" /proc/mounts >/dev/null 2>&1 || mount -o bind /sdcard \"$ROOTFS/sdcard\"\n");
        }

        sb.append("mkdir -p \"$ROOTFS/tmp\"\n");
        sb.append("chmod 1777 \"$ROOTFS/tmp\" 2>/dev/null\n");

        sb.append("TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("if [ -x \"$ROOTFS/bin/bash\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/bin/bash\"\n");
        sb.append("elif [ -x \"$ROOTFS/usr/bin/bash\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/usr/bin/bash\"\n");
        sb.append("elif [ -x \"$ROOTFS/bin/sh\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("elif [ -x \"$ROOTFS/usr/bin/sh\" ]; then\n");
        sb.append("    TARGET_SHELL=\"/usr/bin/sh\"\n");
        sb.append("fi\n");

        sb.append("chroot \"$ROOTFS\" /usr/bin/env -i \\\n");
        sb.append("  HOME=/root \\\n");
        sb.append("  USER=root \\\n");
        sb.append("  LOGNAME=root \\\n");
        sb.append("  SHELL=\"$TARGET_SHELL\" \\\n");
        sb.append("  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \\\n");
        sb.append("  TERM=xterm-256color \\\n");
        sb.append("  LANG=C.UTF-8 \\\n");
        sb.append("  \"$TARGET_SHELL\" --login -i\n");
        sb.append("EXIT_CODE=$?\n");
        sb.append("umount -l \"$ROOTFS/sdcard\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/dev/pts\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/dev\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/sys\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/proc\" 2>/dev/null\n");
        sb.append("exit $EXIT_CODE\n");

        try (FileOutputStream fos = new FileOutputStream(script)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        script.setExecutable(true, false);
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "755", script.getAbsolutePath()}).waitFor();
        } catch (Exception ignored) {}
        return script;
    }

    /**
     * Generate Chroot stop script to cleanly unmount
     */
    public static File createChrootStopScript(File internalBinDir, File rootfsDir) throws IOException {
        File script = new File(internalBinDir, "stop_root.sh");
        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");
        sb.append("umount -l \"$ROOTFS/sdcard\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/dev/pts\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/dev\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/sys\" 2>/dev/null\n");
        sb.append("umount -l \"$ROOTFS/proc\" 2>/dev/null\n");

        try (FileOutputStream fos = new FileOutputStream(script)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        script.setExecutable(true, false);
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "755", script.getAbsolutePath()}).waitFor();
        } catch (Exception ignored) {}
        return script;
    }
}

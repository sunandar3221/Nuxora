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
        String dnsContent = "nameserver 8.8.8.8\nnameserver 1.1.1.1\nnameserver 8.8.4.4\n";
        try (FileOutputStream fos = new FileOutputStream(resolvConf)) {
            fos.write(dnsContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
    }

    /**
     * Generate PRoot startup bash script with nativeLibraryDir support
     */
    public static File createProotLaunchScript(PRootManager prootManager, File rootfsDir, InstalledDistro distro) throws IOException {
        setupDns(rootfsDir);
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
        sb.append("TARGET_SHELL=\"").append(distro.getCustomShell()).append("\"\n");
        sb.append("if [ ! -f \"$ROOTFS$TARGET_SHELL\" ]; then\n");
        sb.append("    if [ -f \"$ROOTFS/bin/bash\" ]; then\n");
        sb.append("        TARGET_SHELL=\"/bin/bash\"\n");
        sb.append("    elif [ -f \"$ROOTFS/bin/sh\" ]; then\n");
        sb.append("        TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("    fi\n");
        sb.append("fi\n");

        sb.append("\"$PROOT\" \\\n");
        sb.append("  --kill-on-exit \\\n");
        sb.append("  --link2symlink \\\n");
        sb.append("  -0 \\\n");
        sb.append("  -r \"$ROOTFS\" \\\n");
        sb.append("  -b /dev \\\n");
        sb.append("  -b /proc \\\n");
        sb.append("  -b /sys \\\n");

        if (distro.isMountSdcard()) {
            sb.append("  -b /sdcard \\\n");
            sb.append("  -b /storage \\\n");
        }

        sb.append("  -w /root \\\n");
        sb.append("  /usr/bin/env -i \\\n");
        sb.append("  HOME=/root \\\n");
        sb.append("  USER=root \\\n");
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

        sb.append("TARGET_SHELL=\"").append(distro.getCustomShell()).append("\"\n");
        sb.append("if [ ! -f \"$ROOTFS$TARGET_SHELL\" ]; then\n");
        sb.append("    if [ -f \"$ROOTFS/bin/bash\" ]; then\n");
        sb.append("        TARGET_SHELL=\"/bin/bash\"\n");
        sb.append("    elif [ -f \"$ROOTFS/bin/sh\" ]; then\n");
        sb.append("        TARGET_SHELL=\"/bin/sh\"\n");
        sb.append("    fi\n");
        sb.append("fi\n");

        sb.append("chroot \"$ROOTFS\" /usr/bin/env -i \\\n");
        sb.append("  HOME=/root \\\n");
        sb.append("  USER=root \\\n");
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

    public static String getXfceInstallCommand() {
        return "apt update && apt install -y xfce4 xfce4-goodies tightvncserver dbus-x11";
    }

    public static String getVncStartCommand() {
        return "export USER=root; export HOME=/root; vncserver :1 -geometry 1280x720 -depth 24";
    }

    public static String getVncStopCommand() {
        return "vncserver -kill :1";
    }

    public static String getPulseAudioCommand() {
        return "pulseaudio --start --load=\"module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1\" --exit-idle-time=-1";
    }

    public static String getDnsFixCommand() {
        return "echo -e \"nameserver 8.8.8.8\\nnameserver 1.1.1.1\" > /etc/resolv.conf";
    }
}

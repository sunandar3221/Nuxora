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
     * Generate PRoot startup bash script
     */
    public static File createProotLaunchScript(File internalBinDir, File rootfsDir, InstalledDistro distro) throws IOException {
        File script = new File(internalBinDir, "start_" + distro.getId() + ".sh");
        File prootBin = new File(internalBinDir, "proot");

        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("unset LD_PRELOAD\n");
        sb.append("PROOT=\"").append(prootBin.getAbsolutePath()).append("\"\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");

        sb.append("if [ ! -x \"$PROOT\" ]; then\n");
        sb.append("    chmod 755 \"$PROOT\"\n");
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
        sb.append("  ").append(distro.getCustomShell()).append(" --login\n");

        try (FileOutputStream fos = new FileOutputStream(script)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        script.setExecutable(true, false);
        return script;
    }

    /**
     * Generate Chroot (Root mode) startup bash script
     */
    public static File createChrootLaunchScript(File internalBinDir, File rootfsDir, InstalledDistro distro) throws IOException {
        File script = new File(internalBinDir, "start_root_" + distro.getId() + ".sh");

        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");

        sb.append("# Mount kernel filesystems\n");
        sb.append("mountpoint -q \"$ROOTFS/proc\" || mount -t proc proc \"$ROOTFS/proc\"\n");
        sb.append("mountpoint -q \"$ROOTFS/sys\" || mount -t sysfs sys \"$ROOTFS/sys\"\n");
        sb.append("mountpoint -q \"$ROOTFS/dev\" || mount -o bind /dev \"$ROOTFS/dev\"\n");
        sb.append("mkdir -p \"$ROOTFS/dev/pts\"\n");
        sb.append("mountpoint -q \"$ROOTFS/dev/pts\" || mount -t devpts devpts \"$ROOTFS/dev/pts\"\n");

        if (distro.isMountSdcard()) {
            sb.append("mkdir -p \"$ROOTFS/sdcard\"\n");
            sb.append("mountpoint -q \"$ROOTFS/sdcard\" || mount -o bind /sdcard \"$ROOTFS/sdcard\"\n");
        }

        sb.append("mkdir -p \"$ROOTFS/tmp\"\n");
        sb.append("chmod 1777 \"$ROOTFS/tmp\"\n");

        sb.append("chroot \"$ROOTFS\" /usr/bin/env -i \\\n");
        sb.append("  HOME=/root \\\n");
        sb.append("  USER=root \\\n");
        sb.append("  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \\\n");
        sb.append("  TERM=xterm-256color \\\n");
        sb.append("  LANG=C.UTF-8 \\\n");
        sb.append("  ").append(distro.getCustomShell()).append(" --login\n");

        try (FileOutputStream fos = new FileOutputStream(script)) {
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        script.setExecutable(true, false);
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
        return script;
    }

    /**
     * Scripts for desktop environment, VNC, audio tools
     */
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

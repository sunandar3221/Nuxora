package com.nuxora.app.core;

import com.nuxora.app.data.model.InstalledDistro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
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
     * Fix common package manager issues in Android PRoot environments
     * (dpkg unsafe-io, apt sandbox user root, policy-rc.d exit 101, Android AID_INET group)
     */
    public static void setupPackageManagerFixes(File rootfsDir) {
        setupDns(rootfsDir);

        try {
            // 0. Ensure .l2s and tmp directories exist for link2symlink hardlink emulation
            File l2sDir = new File(rootfsDir, ".l2s");
            if (!l2sDir.exists()) {
                l2sDir.mkdirs();
            }
            File tmpDir = new File(rootfsDir, "tmp");
            if (!tmpDir.exists()) {
                tmpDir.mkdirs();
            }
            File emptySelinux = new File(rootfsDir, "tmp/.empty_selinux");
            if (!emptySelinux.exists()) {
                emptySelinux.mkdirs();
            }

            // 1. DPKG unsafe-io to prevent fsync/sync and hardlink failures on Android storage
            File dpkgConfDir = new File(rootfsDir, "etc/dpkg/dpkg.cfg.d");
            if (!dpkgConfDir.exists()) {
                dpkgConfDir.mkdirs();
            }
            File unsafeIo = new File(dpkgConfDir, "01unsafe-io");
            String dpkgConf = "force-unsafe-io\n" +
                    "force-bad-path\n" +
                    "no-debsig\n" +
                    "path-exclude=/usr/share/man/*\n" +
                    "path-exclude=/usr/share/doc/*\n" +
                    "path-include=/usr/share/doc/*/copyright\n";
            try (FileOutputStream fos = new FileOutputStream(unsafeIo)) {
                fos.write(dpkgConf.getBytes(StandardCharsets.UTF_8));
            }

            // 2. APT sandbox and dpkg option fixes for Android PRoot
            File aptConfDir = new File(rootfsDir, "etc/apt/apt.conf.d");
            if (!aptConfDir.exists()) {
                aptConfDir.mkdirs();
            }
            File aptSandbox = new File(aptConfDir, "01sandbox");
            String aptConf = "APT::Sandbox::User \"root\";\n" +
                    "APT::Sandbox::Seccomp \"false\";\n" +
                    "Dir::Cache::pkgcache \"\";\n" +
                    "Dir::Cache::srcpkgcache \"\";\n" +
                    "Acquire::http::Pipeline-Depth \"0\";\n" +
                    "Acquire::http::No-Cache \"true\";\n" +
                    "Acquire::BrokenProxy \"true\";\n" +
                    "DPkg::Options {\n" +
                    "   \"--force-unsafe-io\";\n" +
                    "   \"--force-bad-path\";\n" +
                    "   \"--no-debsig\";\n" +
                    "};\n";
            try (FileOutputStream fos = new FileOutputStream(aptSandbox)) {
                fos.write(aptConf.getBytes(StandardCharsets.UTF_8));
            }

            // 3. /usr/sbin/policy-rc.d to prevent service startup failures during dpkg configure
            File sbinDir = new File(rootfsDir, "usr/sbin");
            if (!sbinDir.exists()) {
                sbinDir.mkdirs();
            }
            File policyRc = new File(sbinDir, "policy-rc.d");
            String policyContent = "#!/bin/sh\nexit 101\n";
            try (FileOutputStream fos = new FileOutputStream(policyRc)) {
                fos.write(policyContent.getBytes(StandardCharsets.UTF_8));
            }
            policyRc.setExecutable(true, false);

            // 4. Ensure Android AID_INET (3003) exists in /etc/group
            File groupFile = new File(rootfsDir, "etc/group");
            if (groupFile.exists()) {
                boolean hasAidInet = false;
                StringBuilder groupContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(groupFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(":3003:")) {
                            hasAidInet = true;
                        }
                        groupContent.append(line).append("\n");
                    }
                }
                if (!hasAidInet) {
                    groupContent.append("aid_inet:x:3003:root,_apt\n");
                    groupContent.append("aid_net_raw:x:3004:root\n");
                    groupContent.append("aid_admin:x:3005:root\n");
                    try (FileOutputStream fos = new FileOutputStream(groupFile)) {
                        fos.write(groupContent.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }

            // 5. Clean stale dpkg locks if any
            File dpkgLock = new File(rootfsDir, "var/lib/dpkg/lock");
            if (dpkgLock.exists()) dpkgLock.delete();
            File dpkgLockFrontend = new File(rootfsDir, "var/lib/dpkg/lock-frontend");
            if (dpkgLockFrontend.exists()) dpkgLockFrontend.delete();
            File aptListsLock = new File(rootfsDir, "var/lib/apt/lists/lock");
            if (aptListsLock.exists()) aptListsLock.delete();
            File aptArchiveLock = new File(rootfsDir, "var/cache/apt/archives/lock");
            if (aptArchiveLock.exists()) aptArchiveLock.delete();

        } catch (Exception ignored) {}
    }

    /**
     * Prepares helper scripts and welcome profile inside the distro
     */
    public static void setupExtrasScript(File rootfsDir, boolean enableFirstRun) {
        setupPackageManagerFixes(rootfsDir);

        File binDir = new File(rootfsDir, "usr/local/bin");
        if (!binDir.exists()) {
            binDir.mkdirs();
        }

        // Script to easily install essential packages (nano, curl, wget, bash, sudo)
        File extrasScript = new File(binDir, "install-extras");
        String content = "#!/bin/sh\n" +
                "echo \"[Nuxora] Memeriksa koneksi & memasang paket tambahan...\"\n" +
                "if command -v apk >/dev/null 2>&1; then\n" +
                "    echo \"[Nuxora] Terdeteksi Alpine Linux (APK)...\"\n" +
                "    apk update && apk add --no-cache --no-chown bash nano curl wget ca-certificates sudo\n" +
                "elif command -v apt-get >/dev/null 2>&1; then\n" +
                "    echo \"[Nuxora] Terdeteksi Debian/Ubuntu (APT)...\"\n" +
                "    DEBIAN_FRONTEND=noninteractive apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Dpkg::Options::=\"--force-unsafe-io\" -o Dpkg::Options::=\"--force-bad-path\" nano curl wget sudo ca-certificates\n" +
                "elif command -v pacman >/dev/null 2>&1; then\n" +
                "    echo \"[Nuxora] Terdeteksi Arch Linux (Pacman)...\"\n" +
                "    pacman -Syu --noconfirm nano curl wget sudo ca-certificates\n" +
                "elif command -v dnf >/dev/null 2>&1; then\n" +
                "    echo \"[Nuxora] Terdeteksi Fedora (DNF)...\"\n" +
                "    dnf install -y nano curl wget sudo ca-certificates\n" +
                "fi\n" +
                "echo \"[Nuxora] Paket penting (nano, curl, bash, sudo) selesai disiapkan!\"\n";

        try (FileOutputStream fos = new FileOutputStream(extrasScript)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
        extrasScript.setExecutable(true, false);

        // Alpine apk wrapper to always bypass chown failures
        File apkBinary = new File(rootfsDir, "sbin/apk");
        if (apkBinary.exists()) {
            File apkWrapper = new File(binDir, "apk");
            String apkWrapperContent = "#!/bin/sh\n" +
                    "# Nuxora APK wrapper for Alpine PRoot compatibility\n" +
                    "has_action=0\n" +
                    "has_no_chown=0\n" +
                    "for arg in \"$@\"; do\n" +
                    "    case \"$arg\" in\n" +
                    "        add|del|upgrade|fix) has_action=1 ;;\n" +
                    "        --no-chown) has_no_chown=1 ;;\n" +
                    "    esac\n" +
                    "done\n" +
                    "if [ \"$has_action\" -eq 1 ] && [ \"$has_no_chown\" -eq 0 ]; then\n" +
                    "    exec /sbin/apk --no-chown \"$@\"\n" +
                    "else\n" +
                    "    exec /sbin/apk \"$@\"\n" +
                    "fi\n";
            try (FileOutputStream fos = new FileOutputStream(apkWrapper)) {
                fos.write(apkWrapperContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {}
            apkWrapper.setExecutable(true, false);
        }

        // Backup in /root/install-extras.sh
        File rootExtras = new File(rootfsDir, "root/install-extras.sh");
        try (FileOutputStream fos = new FileOutputStream(rootExtras)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {}
        rootExtras.setExecutable(true, false);

        // Create /etc/profile.d/00-linuxdroid.sh for prompt and first-run
        File profileD = new File(rootfsDir, "etc/profile.d");
        if (!profileD.exists()) {
            profileD.mkdirs();
        }
        File linuxdroidProfile = new File(profileD, "00-linuxdroid.sh");
        StringBuilder sb = new StringBuilder();
        sb.append("# Nuxora Shell Environment\n");
        sb.append("export PS1='\\[\\033[01;32m\\]\\u@\\h\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]# '\n");
        sb.append("export TERM=xterm-256color\n");
        sb.append("export HOME=/root\n");
        sb.append("export USER=root\n");
        sb.append("export LOGNAME=root\n");

        if (enableFirstRun) {
            sb.append("if [ -f /root/.first_run ]; then\n");
            sb.append("    rm -f /root/.first_run\n");
            sb.append("    echo \"=====================================================\"\n");
            sb.append("    echo \"  Selamat Datang di Linux (Nuxora)!\"\n");
            sb.append("    echo \"  Distro siap digunakan dengan hak akses ROOT.\"\n");
            sb.append("    echo \"=====================================================\"\n");
            sb.append("    /usr/local/bin/install-extras\n");
            sb.append("fi\n");
        }

        try (FileOutputStream fos = new FileOutputStream(linuxdroidProfile)) {
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
        setupPackageManagerFixes(rootfsDir);
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
        sb.append("export PROOT_L2S_DIR=\"$ROOTFS/.l2s\"\n");

        sb.append("mkdir -p \"$ROOTFS/tmp\"\n");
        sb.append("mkdir -p \"$ROOTFS/.l2s\"\n");
        sb.append("mkdir -p \"$ROOTFS/tmp/.empty_selinux\"\n");
        sb.append("mkdir -p \"$ROOTFS/dev/pts\" \"$ROOTFS/dev/shm\" 2>/dev/null\n");
        sb.append("chmod 1777 \"$ROOTFS/tmp\" 2>/dev/null\n");

        sb.append("# Clear any stale package manager locks\n");
        sb.append("rm -f \"$ROOTFS/var/lib/dpkg/lock\"* 2>/dev/null\n");
        sb.append("rm -f \"$ROOTFS/var/lib/apt/lists/lock\"* 2>/dev/null\n");
        sb.append("rm -f \"$ROOTFS/var/cache/apt/archives/lock\"* 2>/dev/null\n");

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
        sb.append("  --sysvipc \\\n");
        sb.append("  -L \\\n");
        sb.append("  --kernel-release=6.1.0-Nuxora \\\n");
        sb.append("  --fix-selinux-xattr \\\n");
        sb.append("  -0 \\\n");
        sb.append("  -r \"$ROOTFS\" \\\n");
        sb.append("  -b /dev \\\n");
        sb.append("  -b /proc \\\n");
        sb.append("  -b /sys \\\n");
        sb.append("  -b \"$ROOTFS/tmp/.empty_selinux:/sys/fs/selinux\" \\\n");
        sb.append("  -b /proc/self/fd:/dev/fd \\\n");
        sb.append("  -b /proc/self/fd/0:/dev/stdin \\\n");
        sb.append("  -b /proc/self/fd/1:/dev/stdout \\\n");
        sb.append("  -b /proc/self/fd/2:/dev/stderr \\\n");
        sb.append("  -b /dev/urandom:/dev/random \\\n");
        sb.append("  -b \"$ROOTFS/dev/shm:/dev/shm\" \\\n");

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
        setupPackageManagerFixes(rootfsDir);
        setupExtrasScript(rootfsDir, false);
        File script = new File(internalBinDir, "start_root_" + distro.getId() + ".sh");

        StringBuilder sb = new StringBuilder();
        sb.append("#!/system/bin/sh\n");
        sb.append("ROOTFS=\"").append(rootfsDir.getAbsolutePath()).append("\"\n");

        sb.append("# Clear any stale package manager locks\n");
        sb.append("rm -f \"$ROOTFS/var/lib/dpkg/lock\"* 2>/dev/null\n");
        sb.append("rm -f \"$ROOTFS/var/lib/apt/lists/lock\"* 2>/dev/null\n");
        sb.append("rm -f \"$ROOTFS/var/cache/apt/archives/lock\"* 2>/dev/null\n");

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

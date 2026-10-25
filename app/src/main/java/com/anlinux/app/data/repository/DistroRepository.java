package com.anlinux.app.data.repository;

import com.anlinux.app.R;
import com.anlinux.app.data.model.Distro;

import java.util.ArrayList;
import java.util.List;

public class DistroRepository {

    public static List<Distro> getBuiltInDistros() {
        List<Distro> list = new ArrayList<>();

        // 1. Ubuntu
        Distro ubuntu = new Distro(
                "ubuntu",
                "Ubuntu",
                "24.04 LTS (Noble)",
                "Distro paling populer di dunia, sangat ramah pemula, dukungan paket software APT terlengkap.",
                R.drawable.ic_distro_generic
        );
        ubuntu.addRootfsUrl("aarch64", "https://cloud-images.ubuntu.com/minimal/releases/noble/release/ubuntu-24.04-minimal-cloudimg-arm64-root.tar.xz");
        ubuntu.addRootfsUrl("armhf", "https://cloud-images.ubuntu.com/minimal/releases/noble/release/ubuntu-24.04-minimal-cloudimg-armhf-root.tar.xz");
        ubuntu.addRootfsUrl("x86_64", "https://cloud-images.ubuntu.com/minimal/releases/noble/release/ubuntu-24.04-minimal-cloudimg-amd64-root.tar.xz");
        list.add(ubuntu);

        // 2. Debian
        Distro debian = new Distro(
                "debian",
                "Debian",
                "12 (Bookworm)",
                "Batu fondasi dari banyak distro Linux, terkenal sangat stabil, ringan, dan andal.",
                R.drawable.ic_distro_generic
        );
        debian.addRootfsUrl("aarch64", "https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-arm64v8/bookworm/rootfs.tar.xz");
        debian.addRootfsUrl("armhf", "https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-arm32v7/bookworm/rootfs.tar.xz");
        debian.addRootfsUrl("x86_64", "https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-amd64/bookworm/rootfs.tar.xz");
        debian.addRootfsUrl("i686", "https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-i386/bookworm/rootfs.tar.xz");
        list.add(debian);

        // 3. Kali Linux
        Distro kali = new Distro(
                "kali",
                "Kali Linux",
                "Rolling",
                "Distro khusus ethical hacking, cybersecurity, penetration testing, dan digital forensics.",
                R.drawable.ic_distro_generic
        );
        kali.addRootfsUrl("aarch64", "https://images.linuxcontainers.org/images/kali/current/arm64/default/rootfs.tar.xz");
        kali.addRootfsUrl("armhf", "https://images.linuxcontainers.org/images/kali/current/armhf/default/rootfs.tar.xz");
        kali.addRootfsUrl("x86_64", "https://images.linuxcontainers.org/images/kali/current/amd64/default/rootfs.tar.xz");
        list.add(kali);

        // 4. Alpine Linux
        Distro alpine = new Distro(
                "alpine",
                "Alpine Linux",
                "3.19",
                "Ukuran ekstra mungil (~5MB), menggunakan musl libc & BusyBox, konsumsi RAM super hemat.",
                R.drawable.ic_distro_generic
        );
        alpine.setDefaultShell("/bin/sh");
        alpine.addRootfsUrl("aarch64", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz");
        alpine.addRootfsUrl("armhf", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/armv7/alpine-minirootfs-3.19.1-armv7.tar.gz");
        alpine.addRootfsUrl("x86_64", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/x86_64/alpine-minirootfs-3.19.1-x86_64.tar.gz");
        alpine.addRootfsUrl("i686", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/x86/alpine-minirootfs-3.19.1-x86.tar.gz");
        list.add(alpine);

        // 5. Arch Linux
        Distro arch = new Distro(
                "arch",
                "Arch Linux",
                "Latest",
                "Distro rolling release dengan paket perangkat lunak paling mutakhir dan manajer paket pacman.",
                R.drawable.ic_distro_generic
        );
        arch.addRootfsUrl("aarch64", "http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz");
        arch.addRootfsUrl("armhf", "http://os.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz");
        arch.addRootfsUrl("x86_64", "https://geo.mirror.pkgbuild.com/iso/latest/archlinux-bootstrap-x86_64.tar.zst");
        list.add(arch);

        // 6. Fedora
        Distro fedora = new Distro(
                "fedora",
                "Fedora Linux",
                "40 Rawhide/Container",
                "Distro dari Red Hat dengan teknologi Linux terbaru dan manajer paket DNF.",
                R.drawable.ic_distro_generic
        );
        fedora.addRootfsUrl("aarch64", "https://images.linuxcontainers.org/images/fedora/40/arm64/default/rootfs.tar.xz");
        fedora.addRootfsUrl("x86_64", "https://images.linuxcontainers.org/images/fedora/40/amd64/default/rootfs.tar.xz");
        list.add(fedora);

        // 7. Void Linux
        Distro voidLinux = new Distro(
                "void",
                "Void Linux",
                "Current",
                "Distro independen cepat tanpa systemd, menggunakan runit dan paket xbps.",
                R.drawable.ic_distro_generic
        );
        voidLinux.addRootfsUrl("aarch64", "https://repo-default.voidlinux.org/live/current/void-aarch64-ROOTFS.tar.xz");
        voidLinux.addRootfsUrl("x86_64", "https://repo-default.voidlinux.org/live/current/void-x86_64-ROOTFS.tar.xz");
        list.add(voidLinux);

        return list;
    }

    public static Distro findById(String id) {
        for (Distro d : getBuiltInDistros()) {
            if (d.getId().equalsIgnoreCase(id)) {
                return d;
            }
        }
        return null;
    }
}

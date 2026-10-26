package com.nuxora.app.data.repository;

import com.nuxora.app.R;
import com.nuxora.app.data.model.Distro;

import java.util.ArrayList;
import java.util.List;

public class DistroRepository {

    public static List<Distro> getBuiltInDistros() {
        List<Distro> list = new ArrayList<>();

        // 1. Ubuntu
        Distro ubuntu = new Distro(
                "ubuntu",
                "Ubuntu",
                "22.04 / 24.04 LTS",
                "Distro paling populer, sangat ramah pemula, dukungan paket software APT terlengkap.",
                R.drawable.ic_distro_generic
        );
        ubuntu.addRootfsUrl("arm64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Ubuntu/arm64/ubuntu-rootfs-arm64.tar.xz");
        ubuntu.addRootfsUrl("armhf", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Ubuntu/armhf/ubuntu-rootfs-armhf.tar.xz");
        ubuntu.addRootfsUrl("amd64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Ubuntu/amd64/ubuntu-rootfs-amd64.tar.xz");
        ubuntu.addRootfsUrl("i386", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Ubuntu/i386/ubuntu-rootfs-i386.tar.xz");
        list.add(ubuntu);

        // 2. Debian
        Distro debian = new Distro(
                "debian",
                "Debian",
                "12 (Bookworm)",
                "Batu fondasi dari banyak distro Linux, terkenal sangat stabil, ringan, dan andal.",
                R.drawable.ic_distro_generic
        );
        debian.addRootfsUrl("arm64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Debian/arm64/debian-rootfs-arm64.tar.xz");
        debian.addRootfsUrl("armhf", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Debian/armhf/debian-rootfs-armhf.tar.xz");
        debian.addRootfsUrl("amd64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Debian/amd64/debian-rootfs-amd64.tar.xz");
        debian.addRootfsUrl("i386", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Debian/i386/debian-rootfs-i386.tar.xz");
        list.add(debian);

        // 3. Kali Linux
        Distro kali = new Distro(
                "kali",
                "Kali Linux",
                "Rolling",
                "Distro khusus ethical hacking, cybersecurity, penetration testing, dan digital forensics.",
                R.drawable.ic_distro_generic
        );
        kali.addRootfsUrl("arm64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/arm64/kali-rootfs-arm64.tar.xz");
        kali.addRootfsUrl("armhf", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/armhf/kali-rootfs-armhf.tar.xz");
        kali.addRootfsUrl("amd64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/amd64/kali-rootfs-amd64.tar.xz");
        kali.addRootfsUrl("i386", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Kali/i386/kali-rootfs-i386.tar.xz");
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
        alpine.addRootfsUrl("arm64", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz");
        alpine.addRootfsUrl("armhf", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/armv7/alpine-minirootfs-3.19.1-armv7.tar.gz");
        alpine.addRootfsUrl("amd64", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/x86_64/alpine-minirootfs-3.19.1-x86_64.tar.gz");
        alpine.addRootfsUrl("i386", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/x86/alpine-minirootfs-3.19.1-x86.tar.gz");
        list.add(alpine);

        // 5. Arch Linux
        Distro arch = new Distro(
                "arch",
                "Arch Linux",
                "Latest",
                "Distro rolling release dengan paket perangkat lunak paling mutakhir dan manajer paket pacman.",
                R.drawable.ic_distro_generic
        );
        arch.addRootfsUrl("arm64", "https://ca.us.mirror.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz");
        arch.addRootfsUrl("armhf", "https://ca.us.mirror.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz");
        list.add(arch);

        // 6. Fedora
        Distro fedora = new Distro(
                "fedora",
                "Fedora Linux",
                "Rawhide/Cloud",
                "Distro dari Red Hat dengan teknologi Linux terbaru dan manajer paket DNF.",
                R.drawable.ic_distro_generic
        );
        fedora.addRootfsUrl("arm64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Fedora/arm64/fedora-rootfs-arm64.tar.xz");
        fedora.addRootfsUrl("armhf", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Fedora/armhf/fedora-rootfs-armhf.tar.xz");
        fedora.addRootfsUrl("amd64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/Fedora/amd64/fedora-rootfs-amd64.tar.xz");
        list.add(fedora);

        // 7. openSUSE
        Distro opensuse = new Distro(
                "opensuse",
                "openSUSE Leap",
                "Current",
                "Distro tangguh enterprise dengan manajer paket Zypper dan YaST.",
                R.drawable.ic_distro_generic
        );
        opensuse.addRootfsUrl("arm64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/openSUSE/arm64/opensuse-rootfs-arm64.tar.xz");
        opensuse.addRootfsUrl("armhf", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/openSUSE/armhf/opensuse-rootfs-armhf.tar.xz");
        opensuse.addRootfsUrl("amd64", "https://raw.githubusercontent.com/EXALAB/Anlinux-Resources/master/Rootfs/openSUSE/amd64/opensuse-rootfs-amd64.tar.xz");
        list.add(opensuse);

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

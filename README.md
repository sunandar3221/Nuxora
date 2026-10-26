# LinuxDroid

**LinuxDroid** adalah aplikasi Android open-source berbasis Java murni yang dirancang untuk menjalankan berbagai distribusi Linux secara mulus di perangkat Android, baik dalam mode **Non-Root (PRoot)** maupun **Root (Chroot)**, dengan antarmuka modern yang ramah pemula bergaya **Google Material You (Material 3)**.

---

## ✨ Fitur Utama

- 🎨 **Desain Google Material You (Material 3)**: Tampilan modern, bersih, intuitif, ramah untuk pemula, dan mendukung Dynamic Colors.
- 🐧 **Katalog Distro Populer**:
  - **Ubuntu** (LTS)
  - **Debian** (Stable)
  - **Kali Linux** (Rolling)
  - **Alpine Linux** (Ultra-ringan ~5MB)
  - **Arch Linux** (Rolling)
  - **Fedora**
  - **openSUSE**
- 📦 **Dukungan Custom Distro**: Pasang distro apa saja dari arsip lokal (`.tar.gz`, `.tar.xz`, `.tar.bz2`) atau direct download URL.
- ⚡ **Dual Execution Engine**:
  - **Mode Non-Root (PRoot)**: Berjalan tanpa memerlukan akses root di semua HP Android modern (Android 7.0 s/d Android 14+). Komponen binary PRoot dipaketkan langsung dalam APK native libraries (`jniLibs`).
  - **Mode Root (Chroot)**: Memanfaatkan kernel Android secara langsung untuk performa maksimal (mendukung Magisk dan KernelSU) dengan auto-mount & auto-clean unmount.
- 🔧 **Perbaikan Package Manager Lengkap (Anti-Error Dpkg/APT)**:
  - Otomatis mengatasi error `dpkg` dan `apt update/upgrade` di semua merk ponsel (MIUI/HyperOS, Samsung OneUI, ColorOS, dll.).
  - Bypass seccomp restrictions (`PROOT_NO_SECCOMP=1`).
  - DPKG `force-unsafe-io` untuk mencegah freeze & fsync error pada storage Android.
  - Patch APT Sandbox User (`APT::Sandbox::User "root"`) & injeksi Android `AID_INET` (GID 3003) agar paket download tidak ditolak kernel.
  - Implementasi `/usr/sbin/policy-rc.d` (`exit 101`) agar instalasi daemon tidak gagal pada post-installation dpkg.
- 🔔 **Layanan Foreground & Notifikasi Aktif**:
  - **Sesi Terminal Tidak Mati**: Saat terminal dibuka, Foreground Service berjalan dengan notifikasi permanen ("LinuxDroid - Sesi Terminal Aktif") dan WakeLock CPU sehingga proses Linux tetap berjalan lancar saat Anda beralih aplikasi atau layar dimatikan.
  - **Progress Bar Notifikasi Instalasi**: Pengunduhan dan ekstraksi distro dipantau langsung di bar notifikasi dengan progress bar real-time tanpa risiko proses dimatikan oleh sistem Android.
- 💻 **Terminal Interaktif Ala Termux (True PTY)**:
  - Mengintegrasikan engine emulator terminal resmi dari **Termux** (`TerminalView` & `TerminalSession` via `libtermux.so` PTY).
  - Dukungan penuh aplikasi terminal interaktif CLI seperti `nano`, `vim`, `htop`, `top`, `tmux`, `less`, dan auto-completion Tab di Bash/Zsh.
  - **Pinch-to-Zoom**: Cubit layar untuk memperbesar atau memperkecil ukuran font terminal secara halus (8sp - 34sp).
  - **Extra Keys Toolbar**: Tombol bantu keyboard ala Termux (`ESC`, `TAB`, `CTRL`, `ALT`, `▲`, `▼`, `◀`, `▶`, `Ctrl+C`, `Ctrl+D`, `/`, `-`, `|`, `~`).
  - Tombol pintas toggle keyboard on-screen di bar navigasi atas.
- 🚀 **Auto-Setup Paket Penting**:
  - Otomatis menyediakan DNS Google & Cloudflare (`/etc/resolv.conf`).
  - Opsi pasang paket penting otomatis (`nano`, `curl`, `wget`, `bash`, `sudo`) saat pertama kali meluncurkan terminal.
- 🔒 **100% Offline & Mandiri**: Komponen biner PRoot dan terminal terintegrasi langsung di dalam aplikasi.

---

## 📱 Arsitektur CPU yang Didukung

Aplikasi mendukung 4 arsitektur prosesor utama:
* `arm64-v8a` (AArch64 - 64-bit Android modern)
* `armeabi-v7a` (ARM 32-bit)
* `x86_64` (Emulator Android 64-bit / Intel Chromebook)
* `x86` (Emulator Android 32-bit)

---

## 🛠️ Kompilasi dari Source

### Prasyarat:
* JDK 17 (OpenJDK 17)
* Android SDK Platform 34 & Build-Tools 34.0.0
* Gradle 8.7

### Perintah Build:
```bash
git clone https://github.com/wellkops/AnLinux.git LinuxDroid
cd LinuxDroid
./gradlew assembleDebug
```
File APK hasil build akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📥 Unduh APK

Unduh versi rilis `.apk` terbaru langsung dari halaman [Releases](https://github.com/wellkops/AnLinux/releases).

Pasang via terminal ADB:
```bash
adb install -r LinuxDroid-v1.0.0.apk
```

---

## 🤝 Ucapan Terima Kasih & Kredit (Credits)

Proyek ini dibangun dengan memanfaatkan dan menghormati kontribusi luar biasa dari komunitas open source:

1. **[Termux](https://github.com/termux/termux-app)**:
   - Komponen `terminal-view` dan `terminal-emulator` (dilisensikan di bawah GPLv3) yang menjadi motor utama tampilan dan emulasi pseudo-terminal (PTY) interaktif aplikasi ini.
2. **[PRoot](https://proot-me.github.io/)**:
   - Emulator `chroot`, `mount --bind`, dan virtualisasi root (UID 0) di user-space Android tanpa hak akses root.
3. **[EXALAB AnLinux-Resources](https://github.com/EXALAB/Anlinux-Resources)**:
   - Referensi skrip dan arsip image RootFS distribusi Linux.
4. **[Alpine Linux](https://alpinelinux.org/)**:
   - Penyedia image Minirootfs ultra-ringan berbasis musl libc & busybox.

---

## 📄 Lisensi (License)

Proyek ini dilisensikan di bawah **GNU General Public License v3.0 (GPLv3)**.  
Lihat berkas [LICENSE](LICENSE) untuk informasi lebih detail.

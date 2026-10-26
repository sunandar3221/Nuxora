<div align="center">

# Nuxora

### Run Linux on Android — Seamlessly, Privately, and Without Root

[![Latest Release](https://img.shields.io/github/v/release/wellkops/Nuxora?color=blue&label=Latest%20Version)](https://github.com/wellkops/Nuxora/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-green.svg)](https://github.com/wellkops/Nuxora)
[![Architecture](https://img.shields.io/badge/Architecture-ARM64%20%7C%20ARMv7%20%7C%20x86__64-orange.svg)](https://github.com/wellkops/Nuxora)

[**Download APK (v1.3.0)**](https://github.com/wellkops/Nuxora/releases/download/v1.3.0/Nuxora-v1.3.0.apk) • [**Semua Rilis**](https://github.com/wellkops/Nuxora/releases)

---

</div>

## 📖 Tentang Nuxora

**Nuxora** adalah aplikasi Android canggih yang memungkinkan Anda menginstal dan menjalankan distribusi Linux lengkap (Ubuntu, Debian, Kali Linux, Alpine Linux, Arch Linux, Fedora, dan lainnya) langsung di perangkat Android Anda tanpa memerlukan akses **ROOT**.

Mulai dari versi **v1.3.0**, Nuxora beralih ke arsitektur **Closed Source & Decoupled Container**:
- File paket APK dibuat bersih, ringan, dan mandiri tanpa membundel binary sistem pihak ketiga.
- Komponen backend (PRoot & Rootfs) diunduh secara dinamis saat instalasi distro pertama kali.
- Terminal bawaan berbasis emulator Termux berkinerja tinggi dengan dukungan penuh aplikasi CLI (nano, vim, bash, top, neofetch, gcc, python, nodejs).

---

## ✨ Fitur Utama

- 🚀 **Tanpa Akses Root (PRoot)**: Jalankan distro Linux di Android 7.0 hingga Android 14+ dengan hak akses root tersimulasi secara penuh.
- ⚡ **Dukungan Root Mode (Chroot)**: Untuk perangkat yang sudah di-root, tersedia mode Chroot native berkecepatan tinggi.
- 🖥️ **Terminal Ala Termux**:
  - Dukungan Pinch-to-Zoom (ubah ukuran teks terminal dengan cubitan jari).
  - Tombol keyboard tambahan (ESC, TAB, CTRL, ALT, Tombol Panah).
  - PTY native untuk menjalankan editor dan aplikasi CLI interaktif tanpa glitch.
- 🔔 **Background Service & Notifikasi**: Sesi terminal dan proses instalasi tetap berjalan di latar belakang tanpa terbunuh oleh sistem Android.
- 🛠️ **Distro Support Luas**: Ubuntu, Debian, Alpine Linux, Kali Linux, Arch Linux, Fedora, Void Linux, dan Manjaro.
- 🌐 **Fix Package Manager Otomatis**: Dilengkapi konfigurasi otomatis untuk apt, dpkg, dan apk guna mengatasi error `dpkg unsafe-io`, `fchdir`, dan `Function not implemented`.

---

## 📥 Unduh & Instalasi

Unduh rilis APK terbaru langsung melalui tautan di bawah:

| Berkas | Versi | Deskripsi | Tautan Unduh |
| :--- | :--- | :--- | :--- |
| **Nuxora-v1.3.0.apk** | `1.3.0` | Versi Closed Source terbaru (Decoupled, Anti-GPL bundling) | [Download](https://github.com/wellkops/Nuxora/releases/download/v1.3.0/Nuxora-v1.3.0.apk) |
| **Nuxora-v1.0.0.apk** | `1.0.0` (v1.3.0 Core) | Standalone release build | [Download](https://github.com/wellkops/Nuxora/releases/download/v1.3.0/Nuxora-v1.0.0.apk) |

### Cara Pasang:
1. Unduh file `.apk` di atas.
2. Buka berkas APK di HP Android Anda dan izinkan instalasi dari sumber tidak dikenal (*Unknown Sources*).
3. Buka **Nuxora**, pilih distro favorit Anda (misalnya Ubuntu atau Alpine), lalu klik **Pasang Distro**.
4. Komponen backend akan disiapkan secara otomatis dan terminal siap digunakan!

---

## 🛡️ Lisensi & Keterangan Hak Cipta

- **Aplikasi Utama Nuxora**: Hak Cipta © Nuxora Team. Seluruh kode aplikasi Android, antarmuka, dan layanan latar belakang berlisensi **Proprietary / Closed Source**.
- **Terminal View & Emulator Engine**: Berlisensi di bawah **Apache License 2.0** (Kredit: Termux Project).
- **Backend Emulation (PRoot)**: Komponen emulasi diunduh secara independen pada saat runtime di bawah ketentuan lisensi **GNU General Public License v2 (GPL-2.0)**. Komponen source code PRoot upstream dapat diakses secara publik sesuai ketentuan GPL.

---

<div align="center">
<sub>Dikembangkan dengan ❤️ untuk komunitas Linux & Android.</sub>
</div>

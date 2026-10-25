# AnLinux - Linux Runner untuk Android (Root & Non-Root)

**AnLinux** adalah aplikasi Android berbasis **Java** dengan desain modern **Google Material You (Material 3)** yang memungkinkan Anda menjalankan berbagai distribusi Linux secara langsung di Android, baik pada perangkat yang sudah **di-root** maupun **tanpa root (non-root)**.

---

## ✨ Fitur Utama

### 1. Dual Mode Eksekusi: Non-Root & Root
- **Mode Non-Root (PRoot)**:
  - Berjalan di **semua perangkat Android** tanpa perlu unlock bootloader atau modifikasi sistem.
  - Memanfaatkan teknologi `PRoot` untuk emulasi `chroot`, uid=0 (fake root), dan bind mount direktori Android.
  - Otomatis mengunduh komponen binary `proot` sesuai arsitektur CPU perangkat (`arm64/aarch64`, `armhf`, `x86_64`, `x86`).
- **Mode Root (Chroot)**:
  - Untuk perangkat yang memiliki akses root (Magisk, KernelSU, APatch, SuperSU).
  - Menggunakan native Linux `chroot` dengan real kernel mount (`/proc`, `/sys`, `/dev`, `/dev/pts`, `/sdcard`).
  - Performa 100% native mendekati bare-metal tanpa overhead emulasi syscall ptrace.

### 2. Dukungan Distro Lengkap & Custom Rootfs
- **Katalog Populer Siap Pakai**:
  - **Ubuntu 24.04 LTS (Noble Numbat)**: Ramah pemula, ekosistem paket APT terbesar.
  - **Debian 12 (Bookworm)**: Sangat stabil, ringan, dan andal.
  - **Kali Linux (Rolling)**: Khusus cybersecurity, penetration testing & tools forensik.
  - **Alpine Linux 3.19**: Ukuran ekstra mini (~5MB), konsumsi RAM sangat minim.
  - **Arch Linux**: Rolling release dengan manajer paket `pacman`.
  - **Fedora 40**: Distro modern dengan manajer paket `dnf`.
  - **Void Linux**: Ringan dan cepat tanpa systemd (`xbps`).
- **Distro Kustom (Custom Rootfs)**:
  - Input direct URL rootfs archive (`.tar.xz`, `.tar.gz`, `.tar.bz2`).
  - Atau pilih berkas arsip lokal dari penyimpanan internal HP.
  - Bebas menentukan shell default (`/bin/bash`, `/bin/sh`, `/bin/zsh`).

### 3. Antarmuka Modern Ala Google Material You
- Mendukung dynamic color Android 12+ (`DynamicColors.applyToActivitiesIfAvailable`).
- Tampilan kartu elegan, chip indikator status real-time (*Berjalan* / *Berhenti*, *Root* / *Non-Root*).
- Wizard instalasi bertahap:
  1. Pilih Distro (Katalog / Custom)
  2. Pilih Mode (PRoot vs Chroot)
  3. Konfigurasi direktori & mount penyimpanan internal (`/sdcard`)
  4. Unduh & ekstrak otomatis dengan progress bar & live log streaming.

### 4. Terminal Shell Bawaan Interaktif
- Terminal terintegrasi langsung di aplikasi tanpa ketergantungan aplikasi pihak ketiga.
- Pewarnaan teks **ANSI Color** (folder biru, berkas hijau, peringatan kuning, dsb.).
- **Keyboard Toolbar Khusus**:
  - Tombol `ESC`, `TAB`, `CTRL+C`, `CTRL+D`, `▲`, `▼`, `◀`, `▶`, `|`, `/`, `~`.
- Fitur Clear Screen, Input Command Bar, dan copy/paste.

### 5. Panduan & Alat GUI Desktop (XFCE, VNC, Audio)
- Perintah instan 1-klik untuk memasang antarmuka grafis (XFCE4 Desktop).
- Pengaturan VNC Server (TigerVNC / TightVNC) dan tombol langsung untuk membuka aplikasi VNC Viewer.
- Integrasi server audio (PulseAudio) agar suara dari Linux dapat diputar di speaker HP.
- Script perbaikan DNS otomatis (`8.8.8.8` & `1.1.1.1` pada `/etc/resolv.conf`).

### 6. Layanan Latar Belakang (Foreground Service & WakeLock)
- Menjalankan distro Linux di latar belakang meskipun aplikasi diminimalkan (misalnya untuk web server, SSH server, bot Python, atau torrent).
- Notifikasi status persisten dengan tombol pintas "Terminal" dan "Hentikan".

---

## 📁 Struktur Proyek

```
AnLinux/
├── app/
│   ├── build.gradle                          # Konfigurasi dependensi Android & Java 17
│   ├── src/main/
│   │   ├── AndroidManifest.xml               # Deklarasi izin & komponen aplikasi
│   │   ├── java/com/anlinux/app/
│   │   │   ├── AnLinuxApplication.java       # Inisialisasi Material You dynamic colors
│   │   │   ├── data/
│   │   │   │   ├── model/                    # Model data: Distro, InstalledDistro, ExecutionMode
│   │   │   │   ├── repository/               # Katalog distro built-in (Ubuntu, Debian, Kali, dll.)
│   │   │   │   └── database/                 # SQLite helper penyimpan distro terinstall
│   │   │   ├── core/
│   │   │   │   ├── PRootManager.java         # Engine Non-Root: binari PRoot & launcher
│   │   │   │   ├── ChrootManager.java        # Engine Root: su & native chroot mount
│   │   │   │   ├── ProcessRunner.java        # Interaktif terminal I/O process runner
│   │   │   │   ├── ExtractionHelper.java     # Dekompresi arsip tar, tar.gz, tar.xz
│   │   │   │   └── ScriptGenerator.java      # Generator bash launch script, DNS, GUI
│   │   │   ├── service/
│   │   │   │   └── LinuxService.java         # Foreground Service & WakeLock
│   │   │   ├── ui/
│   │   │   │   ├── MainActivity.java         # Bottom navigation controller (Material 3)
│   │   │   │   ├── distros/                  # Tab daftar distro terinstall & card adapter
│   │   │   │   ├── catalog/                  # Tab katalog distro & card adapter
│   │   │   │   ├── custom/                   # Dialog input distro custom (URL / File)
│   │   │   │   ├── install/                  # Wizard instalasi bertahap & live logs
│   │   │   │   ├── terminal/                 # Terminal shell & special key toolbar
│   │   │   │   ├── tools/                    # Tab alat GUI, XFCE, VNC, PulseAudio
│   │   │   │   └── settings/                 # Tab arsitektur, cek root, preferensi
│   │   │   └── utils/
│   │   │       ├── DeviceUtils.java          # Deteksi CPU arch (aarch64, armhf, x86) & Root
│   │   │       └── AnsiColorParser.java      # Konversi ANSI terminal color ke Android Spans
│   │   └── res/                              # Layout, XML, themes Material 3, Vector drawable
├── build.gradle                              # Root build gradle
├── settings.gradle                           # Konfigurasi modul & repositori
└── gradle.properties
```

---

## 🚀 Cara Menjalankan & Build

### Melalui Android Studio:
1. Buka **Android Studio**.
2. Pilih **Open** lalu arahkan ke direktori `/home/wellkops/AnLinux`.
3. Tunggu proses *Gradle Sync* selesai.
4. Hubungkan HP Android atau jalankan Emulator.
5. Klik tombol **Run** (Shift + F10).

### Melalui Baris Perintah (CLI):
```bash
cd /home/wellkops/AnLinux
./gradlew assembleDebug
```
Berkas APK akan dihasilkan di:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 💡 Panduan Singkat Penggunaan

1. **Memasang Distro Pertama**:
   - Buka tab **Katalog**.
   - Pilih distro yang diinginkan (misal **Ubuntu 24.04**).
   - Pada wizard, pilih **Non-Root (PRoot)** jika HP standar, atau **Mode Root** jika HP sudah di-root.
   - Klik **Mulai Unduh & Pasang**.
   - Setelah selesai, tekan **Buka Terminal**.

2. **Menjalankan Tampilan Grafis (Desktop XFCE & VNC)**:
   - Buka tab **Alat & GUI**.
   - Tekan **Salin Perintah** pada kartu *Pasang Desktop XFCE4*.
   - Buka terminal distro Anda dan paste perintah tersebut.
   - Setelah selesai, jalankan perintah *VNC Server*.
   - Buka aplikasi **VNC Viewer**, masukkan alamat `127.0.0.1:5901` dan password yang Anda buat.

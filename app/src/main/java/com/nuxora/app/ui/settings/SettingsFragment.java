package com.nuxora.app.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nuxora.app.R;
import com.nuxora.app.utils.DeviceUtils;
import com.nuxora.app.utils.ShizukuHelper;
import com.nuxora.app.utils.StoragePermissionHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import rikka.shizuku.Shizuku;

import java.io.File;

public class SettingsFragment extends Fragment {

    public static final String PREFS_NAME = "nuxora_prefs";
    public static final String PREF_WAKELOCK = "pref_wakelock";

    private TextView tvStorageStatus;
    private MaterialButton btnRequestStorage;
    private TextView tvShizukuStatus;
    private MaterialButton btnPairShizuku;
    private MaterialButton btnGrantStorageShizuku;

    private final Shizuku.OnBinderReceivedListener onBinderReceivedListener = () -> {
        if (isAdded()) {
            requireActivity().runOnUiThread(this::updateShizukuStatus);
        }
    };

    private final Shizuku.OnBinderDeadListener onBinderDeadListener = () -> {
        if (isAdded()) {
            requireActivity().runOnUiThread(this::updateShizukuStatus);
        }
    };

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
            (requestCode, grantResult) -> {
                if (requestCode == ShizukuHelper.SHIZUKU_PERMISSION_REQUEST_CODE) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            updateShizukuStatus();
                            if (ShizukuHelper.hasShizukuPermission()) {
                                ShizukuHelper.grantStoragePermissionsViaShizukuAsync(requireContext(), (success, msg) -> {
                                    if (isAdded()) {
                                        requireActivity().runOnUiThread(this::updateStorageStatus);
                                    }
                                });
                            }
                        });
                    }
                }
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView tvSettingArch = view.findViewById(R.id.tvSettingArch);
        TextView tvSettingRoot = view.findViewById(R.id.tvSettingRoot);
        MaterialSwitch switchWakeLock = view.findViewById(R.id.switchWakeLock);
        MaterialButton btnClearCache = view.findViewById(R.id.btnClearCache);

        tvStorageStatus = view.findViewById(R.id.tvStorageStatus);
        btnRequestStorage = view.findViewById(R.id.btnRequestStorage);

        tvShizukuStatus = view.findViewById(R.id.tvShizukuStatus);
        btnPairShizuku = view.findViewById(R.id.btnPairShizuku);
        btnGrantStorageShizuku = view.findViewById(R.id.btnGrantStorageShizuku);

        String arch = DeviceUtils.getNormalizedArch();
        tvSettingArch.setText(getString(R.string.setting_device_arch, arch));

        boolean rootAvailable = DeviceUtils.isRootAvailable();
        tvSettingRoot.setText(getString(R.string.setting_root_status,
                rootAvailable ? getString(R.string.root_available) : getString(R.string.root_unavailable)));

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        switchWakeLock.setChecked(prefs.getBoolean(PREF_WAKELOCK, true));
        switchWakeLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(PREF_WAKELOCK, isChecked).apply();
        });

        btnClearCache.setOnClickListener(v -> {
            clearDirectory(requireContext().getCacheDir());
            File extCache = requireContext().getExternalCacheDir();
            if (extCache != null) clearDirectory(extCache);
            Toast.makeText(requireContext(), "Berkas sementara berhasil dibersihkan.", Toast.LENGTH_SHORT).show();
        });

        btnRequestStorage.setOnClickListener(v -> {
            StoragePermissionHelper.requestStoragePermission(requireActivity());
        });

        btnPairShizuku.setOnClickListener(v -> {
            if (!ShizukuHelper.isShizukuInstalledAndRunning()) {
                Toast.makeText(requireContext(), "Layanan Shizuku tidak aktif atau belum terpasang di perangkat. Pastikan Shizuku berstatus 'Running' di aplikasi Shizuku.", Toast.LENGTH_LONG).show();
                return;
            }
            if (ShizukuHelper.hasShizukuPermission()) {
                Toast.makeText(requireContext(), "Shizuku sudah terhubung dan izin aktif!", Toast.LENGTH_SHORT).show();
            } else {
                ShizukuHelper.requestShizukuPermission();
            }
            updateShizukuStatus();
        });

        btnGrantStorageShizuku.setOnClickListener(v -> {
            if (!ShizukuHelper.hasShizukuPermission()) {
                Toast.makeText(requireContext(), "Izin Shizuku belum diberikan. Silakan hubungkan Shizuku terlebih dahulu.", Toast.LENGTH_LONG).show();
                return;
            }
            btnGrantStorageShizuku.setEnabled(false);
            Toast.makeText(requireContext(), "Menerapkan konfigurasi penyimpanan & Scoped Storage bypass via Shizuku...", Toast.LENGTH_SHORT).show();
            ShizukuHelper.grantStoragePermissionsViaShizukuAsync(requireContext(), (success, message) -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        btnGrantStorageShizuku.setEnabled(true);
                        updateStorageStatus();
                        if (success) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Izin Penyimpanan Luas Diterapkan")
                                    .setMessage("Izin penyimpanan All Files Access dan bypass Scoped Storage (no_isolated_storage) berhasil diberikan via Shizuku!\n\nPenting: Jika sesi terminal sedang terbuka, silakan tutup dan buka kembali terminal agar izin sistem Android diterapkan ke proses Linux.")
                                    .setPositiveButton("Mengerti", null)
                                    .show();
                        } else {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        });

        try {
            Shizuku.addBinderReceivedListenerSticky(onBinderReceivedListener);
            Shizuku.addBinderDeadListener(onBinderDeadListener);
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {}

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStorageStatus();
        updateShizukuStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            Shizuku.removeBinderReceivedListener(onBinderReceivedListener);
            Shizuku.removeBinderDeadListener(onBinderDeadListener);
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {}
    }

    private void updateStorageStatus() {
        if (!isAdded() || tvStorageStatus == null) return;
        boolean hasPermission = StoragePermissionHelper.hasStoragePermission(requireContext());
        if (hasPermission) {
            tvStorageStatus.setText("Status: Akses Semua Berkas Diberikan (Penuh)");
            tvStorageStatus.setTextColor(Color.parseColor("#10B981")); // Green
            btnRequestStorage.setEnabled(false);
            btnRequestStorage.setText("Izin Sudah Aktif");
        } else {
            tvStorageStatus.setText("Status: Belum Diberikan (Akses Terbatas)");
            tvStorageStatus.setTextColor(Color.parseColor("#EF4444")); // Red
            btnRequestStorage.setEnabled(true);
            btnRequestStorage.setText("Beri Izin Penyimpanan");
        }
    }

    private void updateShizukuStatus() {
        if (!isAdded() || tvShizukuStatus == null) return;
        boolean running = ShizukuHelper.isShizukuInstalledAndRunning();
        if (!running) {
            tvShizukuStatus.setText("Status: Shizuku Tidak Berjalan / Belum Terpasang");
            tvShizukuStatus.setTextColor(Color.parseColor("#94A3B8"));
            btnPairShizuku.setEnabled(true);
            btnGrantStorageShizuku.setEnabled(false);
        } else {
            boolean hasPermission = ShizukuHelper.hasShizukuPermission();
            if (hasPermission) {
                tvShizukuStatus.setText("Status: Terhubung (Izin Shizuku Diberikan)");
                tvShizukuStatus.setTextColor(Color.parseColor("#10B981"));
                btnPairShizuku.setEnabled(false);
                btnPairShizuku.setText("Terhubung");
                btnGrantStorageShizuku.setEnabled(true);
            } else {
                tvShizukuStatus.setText("Status: Shizuku Aktif (Menunggu Izin)");
                tvShizukuStatus.setTextColor(Color.parseColor("#F59E0B"));
                btnPairShizuku.setEnabled(true);
                btnPairShizuku.setText("Minta Izin Shizuku");
                btnGrantStorageShizuku.setEnabled(false);
            }
        }
    }

    private void clearDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        clearDirectory(file);
                    }
                    file.delete();
                }
            }
        }
    }
}

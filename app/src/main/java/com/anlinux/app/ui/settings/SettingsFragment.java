package com.anlinux.app.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anlinux.app.R;
import com.anlinux.app.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.io.File;

public class SettingsFragment extends Fragment {

    public static final String PREFS_NAME = "anlinux_prefs";
    public static final String PREF_WAKELOCK = "pref_wakelock";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView tvSettingArch = view.findViewById(R.id.tvSettingArch);
        TextView tvSettingRoot = view.findViewById(R.id.tvSettingRoot);
        MaterialSwitch switchWakeLock = view.findViewById(R.id.switchWakeLock);
        MaterialButton btnClearCache = view.findViewById(R.id.btnClearCache);

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

        return view;
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

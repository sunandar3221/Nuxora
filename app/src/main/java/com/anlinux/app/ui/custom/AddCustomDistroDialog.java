package com.anlinux.app.ui.custom;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.anlinux.app.R;
import com.anlinux.app.data.model.Distro;
import com.anlinux.app.ui.install.InstallWizardActivity;
import com.anlinux.app.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class AddCustomDistroDialog extends DialogFragment {

    private TextInputEditText etCustomName;
    private TextInputEditText etCustomUrl;
    private TextInputEditText etCustomShell;
    private TextView tvSelectedFile;
    private Uri selectedFileUri = null;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null && tvSelectedFile != null) {
                        tvSelectedFile.setText(selectedFileUri.getLastPathSegment());
                    }
                }
            }
    );

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_custom_distro, null);

        etCustomName = view.findViewById(R.id.etCustomName);
        etCustomUrl = view.findViewById(R.id.etCustomUrl);
        etCustomShell = view.findViewById(R.id.etCustomShell);
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        MaterialButton btnPickFile = view.findViewById(R.id.btnPickFile);

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> {
                    String name = etCustomName.getText() != null ? etCustomName.getText().toString().trim() : "";
                    String url = etCustomUrl.getText() != null ? etCustomUrl.getText().toString().trim() : "";
                    String shell = etCustomShell.getText() != null ? etCustomShell.getText().toString().trim() : "/bin/bash";

                    if (name.isEmpty()) {
                        name = "Custom Distro";
                    }

                    if (url.isEmpty() && selectedFileUri == null) {
                        Toast.makeText(requireContext(), "Harap masukkan URL atau pilih berkas arsip lokal.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Distro customDistro = new Distro(
                            "custom_" + System.currentTimeMillis(),
                            name,
                            "Custom Rootfs",
                            "Distribusi Linux khusus yang ditentukan pengguna.",
                            R.drawable.ic_distro_generic
                    );
                    customDistro.setCustom(true);
                    customDistro.setDefaultShell(shell.isEmpty() ? "/bin/bash" : shell);

                    String currentArch = DeviceUtils.getNormalizedArch();
                    if (!url.isEmpty()) {
                        customDistro.addRootfsUrl(currentArch, url);
                    }

                    Intent intent = new Intent(requireContext(), InstallWizardActivity.class);
                    intent.putExtra(InstallWizardActivity.EXTRA_DISTRO, customDistro);
                    if (selectedFileUri != null) {
                        intent.putExtra(InstallWizardActivity.EXTRA_LOCAL_FILE_URI, selectedFileUri.toString());
                    }
                    startActivity(intent);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }
}

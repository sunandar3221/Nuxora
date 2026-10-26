package com.nuxora.app.ui.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.google.android.material.button.MaterialButton;

public class ToolsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        TextView tvXfceCmd = view.findViewById(R.id.tvXfceCmd);
        TextView tvVncCmd = view.findViewById(R.id.tvVncCmd);
        TextView tvAudioCmd = view.findViewById(R.id.tvAudioCmd);
        TextView tvDnsCmd = view.findViewById(R.id.tvDnsCmd);

        MaterialButton btnCopyXfce = view.findViewById(R.id.btnCopyXfce);
        MaterialButton btnCopyVnc = view.findViewById(R.id.btnCopyVnc);
        MaterialButton btnOpenVncApp = view.findViewById(R.id.btnOpenVncApp);
        MaterialButton btnCopyAudio = view.findViewById(R.id.btnCopyAudio);
        MaterialButton btnCopyDns = view.findViewById(R.id.btnCopyDns);

        btnCopyXfce.setOnClickListener(v -> copyToClipboard(tvXfceCmd.getText().toString()));
        btnCopyVnc.setOnClickListener(v -> copyToClipboard(tvVncCmd.getText().toString()));
        btnCopyAudio.setOnClickListener(v -> copyToClipboard(tvAudioCmd.getText().toString()));
        btnCopyDns.setOnClickListener(v -> copyToClipboard(tvDnsCmd.getText().toString()));

        btnOpenVncApp.setOnClickListener(v -> openVncApp());

        return view;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("AnLinux Command", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.command_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void openVncApp() {
        Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage("com.realvnc.viewer.android");
        if (intent == null) {
            intent = requireContext().getPackageManager().getLaunchIntentForPackage("com.iiordanov.bVNC");
        }
        if (intent != null) {
            startActivity(intent);
        } else {
            // Open Play Store search for VNC Viewer
            try {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=vnc%20viewer"));
                startActivity(marketIntent);
            } catch (Exception e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=vnc%20viewer&c=apps"));
                startActivity(webIntent);
            }
        }
    }
}

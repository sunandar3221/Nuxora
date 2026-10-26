package com.nuxora.app.ui.distros;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nuxora.app.R;
import com.nuxora.app.core.ChrootManager;
import com.nuxora.app.data.database.DistroDatabaseHelper;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.nuxora.app.service.LinuxService;
import com.nuxora.app.ui.MainActivity;
import com.nuxora.app.ui.custom.AddCustomDistroDialog;
import com.nuxora.app.ui.terminal.TerminalActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InstalledDistrosFragment extends Fragment implements InstalledDistrosAdapter.OnDistroActionListener {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private ExtendedFloatingActionButton fabAddDistro;
    private MaterialButton btnExploreCatalog;
    private InstalledDistrosAdapter adapter;
    private final List<InstalledDistro> distrosList = new ArrayList<>();
    private DistroDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_installed, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewDistros);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        fabAddDistro = view.findViewById(R.id.fabAddDistro);
        btnExploreCatalog = view.findViewById(R.id.btnExploreCatalog);

        dbHelper = DistroDatabaseHelper.getInstance(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InstalledDistrosAdapter(requireContext(), distrosList, this);
        recyclerView.setAdapter(adapter);

        fabAddDistro.setOnClickListener(v -> {
            AddCustomDistroDialog dialog = new AddCustomDistroDialog();
            dialog.show(getParentFragmentManager(), "AddCustomDistro");
        });

        btnExploreCatalog.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCatalog();
            }
        });

        loadDistros();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDistros();
    }

    public void loadDistros() {
        if (dbHelper == null) return;
        distrosList.clear();
        distrosList.addAll(dbHelper.getAllInstalledDistros());
        adapter.notifyDataSetChanged();

        if (distrosList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onOpenTerminal(InstalledDistro distro) {
        boolean hasPermission = com.nuxora.app.utils.StoragePermissionHelper.hasStoragePermission(requireContext())
                || com.nuxora.app.utils.ShizukuHelper.hasShizukuPermission();
        if (distro.isMountSdcard() && !hasPermission) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Izin Penyimpanan Dibutuhkan")
                    .setMessage("Distro ini mengaktifkan 'Mount SD Card', tetapi izin akses penyimpanan belum diberikan. Folder /sdcard tidak dapat dibaca tanpa izin ini.")
                    .setPositiveButton("Beri Izin", (dialog, which) -> {
                        com.nuxora.app.utils.StoragePermissionHelper.requestStoragePermission(requireActivity());
                    })
                    .setNegativeButton("Buka Terminal Saja", (dialog, which) -> {
                        openTerminalInternal(distro);
                    })
                    .show();
            return;
        }
        openTerminalInternal(distro);
    }

    private void openTerminalInternal(InstalledDistro distro) {
        Intent intent = new Intent(requireContext(), TerminalActivity.class);
        intent.putExtra(TerminalActivity.EXTRA_DISTRO_ID, distro.getId());
        startActivity(intent);
    }

    @Override
    public void onToggleStartStop(InstalledDistro distro) {
        boolean willRun = !distro.isRunning();
        distro.setRunning(willRun);
        dbHelper.updateRunningStatus(distro.getId(), willRun);

        Intent serviceIntent = new Intent(requireContext(), LinuxService.class);
        if (willRun) {
            serviceIntent.putExtra(LinuxService.EXTRA_DISTRO_NAME, distro.getName());
            serviceIntent.putExtra(LinuxService.EXTRA_DISTRO_ID, distro.getId());
            requireContext().startService(serviceIntent);
            Toast.makeText(requireContext(), distro.getName() + " dimulai.", Toast.LENGTH_SHORT).show();
        } else {
            if (distro.getExecutionMode() == ExecutionMode.ROOT) {
                new ChrootManager(requireContext()).stopChroot(distro);
            }
            serviceIntent.setAction(LinuxService.ACTION_STOP);
            requireContext().startService(serviceIntent);
            Toast.makeText(requireContext(), distro.getName() + " dihentikan.", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDelete(InstalledDistro distro) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hapus " + distro.getName() + "?")
                .setMessage("Semua berkas dan data di dalam instalasi distro ini akan dihapus secara permanen.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    // Stop service if running
                    if (distro.isRunning()) {
                        onToggleStartStop(distro);
                    }
                    // Delete files in background
                    new Thread(() -> {
                        deleteRecursive(new File(distro.getRootfsPath()));
                    }).start();

                    dbHelper.deleteDistro(distro.getId());
                    loadDistros();
                    Toast.makeText(requireContext(), "Distro berhasil dihapus.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}

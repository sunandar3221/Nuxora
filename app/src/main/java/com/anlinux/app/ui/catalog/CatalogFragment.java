package com.anlinux.app.ui.catalog;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anlinux.app.R;
import com.anlinux.app.data.model.Distro;
import com.anlinux.app.data.repository.DistroRepository;
import com.anlinux.app.ui.custom.AddCustomDistroDialog;
import com.anlinux.app.ui.install.InstallWizardActivity;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CatalogFragment extends Fragment implements DistroCatalogAdapter.OnDistroInstallClickListener {

    private RecyclerView recyclerView;
    private MaterialButton btnCustomDistroBanner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_catalog, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCatalog);
        btnCustomDistroBanner = view.findViewById(R.id.btnCustomDistroBanner);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<Distro> distros = DistroRepository.getBuiltInDistros();
        DistroCatalogAdapter adapter = new DistroCatalogAdapter(requireContext(), distros, this);
        recyclerView.setAdapter(adapter);

        btnCustomDistroBanner.setOnClickListener(v -> {
            AddCustomDistroDialog dialog = new AddCustomDistroDialog();
            dialog.show(getParentFragmentManager(), "AddCustomDistro");
        });

        return view;
    }

    @Override
    public void onInstallClick(Distro distro) {
        Intent intent = new Intent(requireContext(), InstallWizardActivity.class);
        intent.putExtra(InstallWizardActivity.EXTRA_DISTRO, distro);
        startActivity(intent);
    }
}

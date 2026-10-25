package com.anlinux.app.ui.catalog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anlinux.app.R;
import com.anlinux.app.data.model.Distro;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DistroCatalogAdapter extends RecyclerView.Adapter<DistroCatalogAdapter.ViewHolder> {

    public interface OnDistroInstallClickListener {
        void onInstallClick(Distro distro);
    }

    private final Context context;
    private final List<Distro> distroList;
    private final OnDistroInstallClickListener listener;

    public DistroCatalogAdapter(Context context, List<Distro> distroList, OnDistroInstallClickListener listener) {
        this.context = context;
        this.distroList = distroList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_catalog_distro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Distro item = distroList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvVersion.setText(item.getVersion());
        holder.tvDesc.setText(item.getDescription());
        holder.imgIcon.setImageResource(item.getIconRes());

        holder.btnInstall.setOnClickListener(v -> {
            if (listener != null) {
                listener.onInstallClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return distroList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        TextView tvVersion;
        TextView tvDesc;
        MaterialButton btnInstall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.catalogDistroIcon);
            tvName = itemView.findViewById(R.id.catalogDistroName);
            tvVersion = itemView.findViewById(R.id.catalogDistroVersion);
            tvDesc = itemView.findViewById(R.id.catalogDistroDesc);
            btnInstall = itemView.findViewById(R.id.btnInstallDistro);
        }
    }
}

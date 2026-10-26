package com.nuxora.app.ui.distros;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nuxora.app.R;
import com.nuxora.app.data.model.ExecutionMode;
import com.nuxora.app.data.model.InstalledDistro;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.List;

public class InstalledDistrosAdapter extends RecyclerView.Adapter<InstalledDistrosAdapter.ViewHolder> {

    public interface OnDistroActionListener {
        void onOpenTerminal(InstalledDistro distro);
        void onToggleStartStop(InstalledDistro distro);
        void onDelete(InstalledDistro distro);
    }

    private final Context context;
    private final List<InstalledDistro> distros;
    private final OnDistroActionListener listener;

    public InstalledDistrosAdapter(Context context, List<InstalledDistro> distros, OnDistroActionListener listener) {
        this.context = context;
        this.distros = distros;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_installed_distro, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InstalledDistro item = distros.get(position);

        holder.tvName.setText(item.getName());
        holder.tvPath.setText(item.getRootfsPath());

        // Mode badge
        if (item.getExecutionMode() == ExecutionMode.ROOT) {
            holder.chipMode.setText(R.string.badge_root);
            holder.chipMode.setChipIconResource(R.drawable.ic_root);
        } else {
            holder.chipMode.setText(R.string.badge_non_root);
            holder.chipMode.setChipIconResource(R.drawable.ic_shield);
        }

        // Status badge
        if (item.isRunning()) {
            holder.chipStatus.setText(R.string.status_running);
            holder.chipStatus.setTextColor(Color.parseColor("#1B5E20"));
            holder.btnStartStop.setText(R.string.btn_stop);
            holder.btnStartStop.setIconResource(R.drawable.ic_stop);
        } else {
            holder.chipStatus.setText(R.string.status_stopped);
            holder.chipStatus.setTextColor(Color.parseColor("#64748B"));
            holder.btnStartStop.setText(R.string.btn_start);
            holder.btnStartStop.setIconResource(R.drawable.ic_play);
        }

        // Mount sdcard chip
        holder.chipStorage.setVisibility(item.isMountSdcard() ? View.VISIBLE : View.GONE);

        // Actions
        holder.btnOpenTerminal.setOnClickListener(v -> {
            if (listener != null) listener.onOpenTerminal(item);
        });

        holder.btnStartStop.setOnClickListener(v -> {
            if (listener != null) listener.onToggleStartStop(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return distros.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;
        TextView tvPath;
        Chip chipMode;
        Chip chipStatus;
        Chip chipStorage;
        MaterialButton btnStartStop;
        MaterialButton btnOpenTerminal;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.distroIcon);
            tvName = itemView.findViewById(R.id.distroName);
            tvPath = itemView.findViewById(R.id.distroPath);
            chipMode = itemView.findViewById(R.id.chipMode);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipStorage = itemView.findViewById(R.id.chipStorage);
            btnStartStop = itemView.findViewById(R.id.btnStartStop);
            btnOpenTerminal = itemView.findViewById(R.id.btnOpenTerminal);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

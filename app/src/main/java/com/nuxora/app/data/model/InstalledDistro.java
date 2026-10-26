package com.nuxora.app.data.model;

import java.io.Serializable;

public class InstalledDistro implements Serializable {
    private long id;
    private String name;
    private String distroKey;
    private String rootfsPath;
    private ExecutionMode executionMode;
    private boolean isRunning;
    private boolean mountSdcard;
    private long installedAt;
    private String diskUsage;
    private String customShell;

    public InstalledDistro() {
        this.installedAt = System.currentTimeMillis();
        this.mountSdcard = true;
        this.isRunning = false;
        this.diskUsage = "0 MB";
    }

    public InstalledDistro(String name, String distroKey, String rootfsPath, ExecutionMode executionMode) {
        this();
        this.name = name;
        this.distroKey = distroKey;
        this.rootfsPath = rootfsPath;
        this.executionMode = executionMode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistroKey() {
        return distroKey;
    }

    public void setDistroKey(String distroKey) {
        this.distroKey = distroKey;
    }

    public String getRootfsPath() {
        return rootfsPath;
    }

    public void setRootfsPath(String rootfsPath) {
        this.rootfsPath = rootfsPath;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isMountSdcard() {
        return mountSdcard;
    }

    public void setMountSdcard(boolean mountSdcard) {
        this.mountSdcard = mountSdcard;
    }

    public long getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(long installedAt) {
        this.installedAt = installedAt;
    }

    public String getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(String diskUsage) {
        this.diskUsage = diskUsage;
    }

    public String getCustomShell() {
        return customShell != null && !customShell.isEmpty() ? customShell : "/bin/bash";
    }

    public void setCustomShell(String customShell) {
        this.customShell = customShell;
    }
}

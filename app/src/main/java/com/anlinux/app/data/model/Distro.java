package com.anlinux.app.data.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Distro implements Serializable {
    private String id;
    private String name;
    private String version;
    private String description;
    private int iconRes;
    private Map<String, String> rootfsUrls; // arch -> url
    private String defaultShell;
    private boolean isCustom;

    public Distro(String id, String name, String version, String description, int iconRes) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.iconRes = iconRes;
        this.rootfsUrls = new HashMap<>();
        this.defaultShell = "/bin/bash";
        this.isCustom = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public int getIconRes() {
        return iconRes;
    }

    public Map<String, String> getRootfsUrls() {
        return rootfsUrls;
    }

    public void addRootfsUrl(String arch, String url) {
        this.rootfsUrls.put(arch, url);
    }

    public String getUrlForArch(String arch) {
        return rootfsUrls.get(arch);
    }

    public String getDefaultShell() {
        return defaultShell;
    }

    public void setDefaultShell(String defaultShell) {
        this.defaultShell = defaultShell;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }
}

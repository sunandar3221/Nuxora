package com.nuxora.app.data.model;

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
        if (rootfsUrls == null || rootfsUrls.isEmpty()) return null;
        if (arch != null) {
            String lower = arch.toLowerCase();
            if (rootfsUrls.containsKey(lower)) return rootfsUrls.get(lower);
            if ((lower.equals("aarch64") || lower.equals("arm64")) && rootfsUrls.containsKey("arm64")) return rootfsUrls.get("arm64");
            if ((lower.equals("aarch64") || lower.equals("arm64")) && rootfsUrls.containsKey("aarch64")) return rootfsUrls.get("aarch64");
            if ((lower.equals("armhf") || lower.equals("armv7") || lower.equals("arm")) && rootfsUrls.containsKey("armhf")) return rootfsUrls.get("armhf");
            if ((lower.equals("armhf") || lower.equals("armv7") || lower.equals("arm")) && rootfsUrls.containsKey("armv7")) return rootfsUrls.get("armv7");
            if ((lower.equals("x86_64") || lower.equals("amd64")) && rootfsUrls.containsKey("amd64")) return rootfsUrls.get("amd64");
            if ((lower.equals("x86_64") || lower.equals("amd64")) && rootfsUrls.containsKey("x86_64")) return rootfsUrls.get("x86_64");
            if ((lower.equals("i686") || lower.equals("i386") || lower.equals("x86")) && rootfsUrls.containsKey("i386")) return rootfsUrls.get("i386");
            if ((lower.equals("i686") || lower.equals("i386") || lower.equals("x86")) && rootfsUrls.containsKey("i686")) return rootfsUrls.get("i686");
            if ((lower.equals("i686") || lower.equals("i386") || lower.equals("x86")) && rootfsUrls.containsKey("x86")) return rootfsUrls.get("x86");
        }
        return rootfsUrls.values().iterator().next();
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

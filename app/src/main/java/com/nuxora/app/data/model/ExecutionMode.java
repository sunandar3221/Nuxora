package com.nuxora.app.data.model;

public enum ExecutionMode {
    PROOT("Non-Root (PRoot)"),
    ROOT("Root (Chroot)");

    private final String displayName;

    ExecutionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

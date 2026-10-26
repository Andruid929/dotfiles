package io.druidlabs.dotfiles.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SyncProvider {

    GOOGLE_DRIVE("GDrive", "Google drive");

    private final String id;
    private final String name;

    SyncProvider(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    public static @Nullable SyncProvider fromString(@NotNull String input) {
        String trimmed = input.trim();

        if (trimmed.isBlank()) return null;

        for (SyncProvider provider : SyncProvider.values()) {

            if (provider.getId().equalsIgnoreCase(trimmed)) {
                return provider;
            }
        }

        return null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return id;
    }
}   

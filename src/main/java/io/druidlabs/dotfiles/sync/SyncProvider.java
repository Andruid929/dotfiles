package io.druidlabs.dotfiles.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A remote provider that a {@link RemoteStore} can sync the dotfiles save file with.
 */
public enum SyncProvider {

    /**
     * Google Drive, through {@link io.druidlabs.dotfiles.sync.gdrive.GoogleDriveRemoteStore}.
     */
    GOOGLE_DRIVE("GDrive", "Google Drive");

    private final String id;
    private final String name;

    SyncProvider(@NotNull String id, @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * @param input a provider ID, matched case-insensitively and ignoring surrounding whitespace. Display names
     *              are not accepted.
     * @return the matching provider, or {@code null} for blank or unknown input, including {@code none}.
     */
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

    /**
     * @return the ID used in the config file and as the name of the provider's secure folder.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the display name shown to the user.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return id;
    }
}   

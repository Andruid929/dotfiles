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
    GOOGLE_DRIVE("GDrive", "Google Drive", "https://myaccount.google.com/permissions");

    private final String id;
    private final String name;
    private final String permissionsUrl;

    SyncProvider(@NotNull String id, @NotNull String name, @NotNull String permissionsUrl) {
        this.id = id;
        this.name = name;
        this.permissionsUrl = permissionsUrl;
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

    /**
     * @return the page where the user can revoke the app's access by hand, for when {@code logout --force} deletes
     * the local credential without revoking it.
     */
    public String getPermissionsUrl() {
        return permissionsUrl;
    }

    @Override
    public String toString() {
        return id;
    }
}

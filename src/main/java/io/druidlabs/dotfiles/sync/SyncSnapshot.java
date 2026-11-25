package io.druidlabs.dotfiles.sync;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.druidlabs.dotfiles.backup.UndoRecord;
import io.druidlabs.dotfiles.exceptions.BlankValueException;
import io.druidlabs.dotfiles.transformer.HashHandler;

/**
 * What the sync snapshot file ({@code .sync}) stores: the state of the dotfiles at the last successful sync, which
 * push and pull compare both sides against to tell which one changed, and what that sync replaced, so it can be
 * undone.
 * <p>
 * Every value is checked on construction, which reading {@code .sync} goes through, so a hand-edited file with a
 * blank provider or a malformed hash reads as corrupt rather than as a snapshot that silently never matches.
 *
 * @param providerId the ID of the sync provider the snapshot was taken with. A snapshot from another provider doesn't
 *                   count, since it describes a different remote file.
 * @param hash       the dotfiles' hash, from {@link HashHandler#hashDotfiles}.
 * @param undo       what the last push or pull replaced, or {@code null} if there is nothing to undo.
 */
public record SyncSnapshot(@NotNull String providerId, @NotNull String hash, @Nullable UndoRecord undo) {

    /**
     * @throws BlankValueException      if the provider ID or the hash is missing or blank.
     * @throws IllegalArgumentException if the hash isn't a SHA-256 hash in lower-case hex.
     */
    public SyncSnapshot {
        //noinspection ConstantValue (Jackson passes null for a missing field)
        if (providerId == null || providerId.isBlank()) {
            throw new BlankValueException("Sync provider ID is required");
        }

        HashHandler.requireHash(hash, "Snapshot hash");
    }
}

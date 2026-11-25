package io.druidlabs.dotfiles.backup;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

import io.druidlabs.dotfiles.exceptions.BlankValueException;
import io.druidlabs.dotfiles.transformer.HashHandler;

/**
 * What the last push or pull replaced, kept in the sync snapshot so {@code undo} can put it back. Only the last
 * overwrite is kept.
 * <p>
 * The backup itself is a file in the backups folder named after its hash, so this stores the hash and never a path.
 * Undo checks the file still hashes to its name before restoring it.
 *
 * @param operation    the operation that ran.
 * @param performedAt  when it ran.
 * @param previousHash the snapshot hash from before it, which undo puts back, or {@code null} if it was the first
 *                     sync. Often the same as {@code backupHash}, but not after a conflict, where the side it replaced
 *                     had changed since the last sync.
 * @param backupHash   the hash of the dotfiles it replaced, which is also the backup's file name: the local dotfiles
 *                     for a pull, the remote ones for a push. {@code null} only for a first push, which replaced
 *                     nothing, and undoing it moves the remote file to the trash.
 * @param remoteTag    for a push, the remote tag it got back, which undo checks the remote file still has. Not used
 *                     for a pull.
 */
public record UndoRecord(@NotNull SyncOperation operation, @NotNull Instant performedAt, @Nullable String previousHash,
                         @Nullable String backupHash, @Nullable String remoteTag) {

    /**
     * @throws IllegalArgumentException if the operation or the time is missing, a hash is malformed, a pull has no
     *                                  backup, or a push has no remote tag.
     */
    public UndoRecord {
        //Suppression note: (Jackson passes null for a missing fild)
        //noinspection ConstantValue
        if (operation == null) {
            throw new IllegalArgumentException("Undo record has no operation");
        }

        //noinspection ConstantValue
        if (performedAt == null) {
            throw new IllegalArgumentException("Undo record has no time");
        }

        if (previousHash != null) {
            HashHandler.requireHash(previousHash, "Previous snapshot hash");
        }

        if (backupHash != null) {
            HashHandler.requireHash(backupHash, "Backup hash");

        } else if (operation == SyncOperation.PULL) {
            throw new IllegalArgumentException("A pull always replaces the local dotfiles, so it needs a backup");
        }

        if (operation == SyncOperation.PUSH && (remoteTag == null || remoteTag.isBlank())) {
            throw new BlankValueException("A push needs the remote tag it got back");
        }
    }
}

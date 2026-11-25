package io.druidlabs.dotfiles.backup;

/**
 * The sync operations that overwrite a side, and so can be undone.
 */
public enum SyncOperation {

    /**
     * Uploaded the local dotfiles over the remote file.
     */
    PUSH,

    /**
     * Replaced the local dotfiles with the remote file.
     */
    PULL
}

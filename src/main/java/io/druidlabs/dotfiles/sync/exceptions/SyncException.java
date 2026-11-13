package io.druidlabs.dotfiles.sync.exceptions;

import io.druidlabs.dotfiles.sync.RemoteStore;

/**
 * Thrown when a sync operation against a {@link RemoteStore} fails.
 */
public class SyncException extends Exception {

    /**
     * @param message the message to show the user.
     */
    public SyncException(String message) {
        super(message);
    }

    /**
     * @param message the message to show the user.
     * @param cause   the underlying failure, for the log file.
     */
    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

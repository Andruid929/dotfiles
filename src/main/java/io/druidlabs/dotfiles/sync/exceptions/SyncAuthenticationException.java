package io.druidlabs.dotfiles.sync.exceptions;

import io.druidlabs.dotfiles.sync.RemoteStore;

/**
 * Thrown when a {@link RemoteStore} has no usable credential, either because the user is not logged in
 * or because the stored credential has expired or been revoked.
 */
public class SyncAuthenticationException extends SyncException {

    /**
     * @param s the message to show the user.
     */
    public SyncAuthenticationException(String s) {
        super(s);
    }

    /**
     * @param message the message to show the user.
     * @param cause   the underlying failure, for the log file.
     */
    public SyncAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

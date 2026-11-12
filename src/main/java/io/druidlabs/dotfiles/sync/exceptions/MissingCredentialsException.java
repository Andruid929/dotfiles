package io.druidlabs.dotfiles.sync.exceptions;

import io.druidlabs.dotfiles.sync.RemoteStore;

/**
 * Thrown when the client credentials file the user must supply is not in the provider's
 * {@link RemoteStore#secureFilesPath()} folder.
 */
public class MissingCredentialsException extends SyncException {

    /**
     * @param message the message to show the user.
     */
    public MissingCredentialsException(String message) {
        super(message);
    }

    /**
     * @param message the message to show the user.
     * @param cause   the underlying failure, for the log file.
     */
    public MissingCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}

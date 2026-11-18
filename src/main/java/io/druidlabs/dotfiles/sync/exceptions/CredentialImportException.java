package io.druidlabs.dotfiles.sync.exceptions;

import io.druidlabs.dotfiles.sync.RemoteStore;

/**
 * Thrown when the file given to {@link RemoteStore#importCredentials(java.nio.file.Path, boolean)} can't be imported
 * for a reason the user can fix, such as a wrong path or a file that isn't a valid client credentials file.
 */
public class CredentialImportException extends SyncException {

    /**
     * @param message the message to show the user.
     */
    public CredentialImportException(String message) {
        super(message);
    }

    /**
     * @param message the message to show the user.
     * @param cause   the underlying failure, for the log file.
     */
    public CredentialImportException(String message, Throwable cause) {
        super(message, cause);
    }
}

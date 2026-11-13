package io.druidlabs.dotfiles.sync.exceptions;

import io.druidlabs.dotfiles.config.ConfigOption;

/**
 * Thrown when a sync command runs while {@link ConfigOption#SYNC_PROVIDER} is set to none.
 */
public class MissingSyncProviderException extends SyncException {

    /**
     * @param message the message to show the user.
     */
    public MissingSyncProviderException(String message) {
        super(message);
    }

    /**
     * @param message the message to show the user.
     * @param cause   the underlying failure, for the log file.
     */
    public MissingSyncProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}

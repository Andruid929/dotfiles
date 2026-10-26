package io.druidlabs.dotfiles.sync;

/**
 * Thrown when a sync operation against a {@link RemoteStore} fails.
 */
public class SyncException extends Exception {

    public SyncException(String message) {
        super(message);
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

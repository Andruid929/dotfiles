package io.druidlabs.dotfiles.sync;

/**
 * Thrown when a {@link RemoteStore} has no usable credential, either because the user is not logged in
 * or because the stored credential has expired or been revoked.
 */
public class SyncAuthenticationException extends SyncException {

    public SyncAuthenticationException(String s) {
        super(s);
    }

    public SyncAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

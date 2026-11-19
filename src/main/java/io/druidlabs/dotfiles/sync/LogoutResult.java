package io.druidlabs.dotfiles.sync;

/**
 * What {@link RemoteStore#logout()} did, so the command can tell the user. A failed logout throws instead.
 */
public enum LogoutResult {

    /**
     * No credential was stored, so there was nothing to revoke. An empty {@link RemoteStore#tokensPath()} folder
     * may still have been deleted.
     */
    NOT_LOGGED_IN,

    /**
     * The credential was revoked with the remote and then deleted locally.
     */
    REVOKED,

    /**
     * The stored credential held no token to revoke, so it was deleted locally without contacting the remote.
     */
    DISCARDED_WITHOUT_REVOKE
}

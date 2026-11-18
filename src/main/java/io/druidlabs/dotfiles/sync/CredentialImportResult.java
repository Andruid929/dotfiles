package io.druidlabs.dotfiles.sync;

/**
 * What {@link RemoteStore#importCredentials(java.nio.file.Path, boolean)} did, so the command can tell the user.
 * A failed import throws instead.
 */
public enum CredentialImportResult {

    /**
     * No credentials file was in place, so the given one was copied in.
     */
    COPIED,

    /**
     * A different credentials file was in place and replacing it was allowed, so it was overwritten.
     */
    REPLACED,

    /**
     * A different credentials file was in place and replacing it was not allowed, so nothing was written.
     */
    REFUSED,

    /**
     * The credentials file in place is identical to the given one, so nothing was written.
     */
    EXISTS
}

package io.druidlabs.dotfiles.sync;

import java.nio.file.Path;
import java.util.Optional;

import io.druidlabs.dotfiles.constants.FilePaths;

/**
 * A remote location that holds a single copy of the dotfiles save file.
 * Implementations only move the file to and from the remote and manage their own credentials,
 * comparing and resolving the two sides is left to the caller.
 * <p>
 * Only {@link #login()} may start an interactive authentication flow. {@link #fetch()} and {@link #upload(String)}
 * must fail with a {@link SyncAuthenticationException} when no usable credential is stored, rather than prompting
 * the user.
 */
public interface RemoteStore {

    SyncProvider getSyncProvider();

    default Path tokensPath() {
        return secureFilesPath().resolve("tokens");
    }

    default Path secureFilesPath() {
        return FilePaths.SYNC_SECURE_DIR.resolve(getSyncProvider().getId());
    }

    /**
     * Downloads the remote save file.
     *
     * @return the file's contents with its version tag, or an empty {@code Optional} if no remote file exists yet.
     * @throws SyncAuthenticationException if there is no stored credential, or it has expired or been revoked.
     * @throws SyncException               if the remote could not be reached or rejected the request.
     */
    Optional<FetchResult> fetch() throws SyncException;

    /**
     * Uploads the given contents, replacing the remote save file.
     * The first upload creates the remote file, along with any folder it lives in.
     *
     * @param content the full contents of the save file.
     * @return the version tag of the newly uploaded file.
     * @throws SyncAuthenticationException if there is no stored credential, or it has expired or been revoked.
     * @throws SyncException               if the remote could not be reached or rejected the request.
     */
    String upload(String content) throws SyncException;

    /**
     * Authenticates with the remote and stores the resulting credential locally.
     * This is the only method allowed to open a browser or otherwise involve the user.
     *
     * @throws SyncException if the client credentials are missing, the user declines access, or the flow fails.
     */
    void login() throws SyncException;

    /**
     * Revokes the stored credential with the remote, then deletes it locally.
     * The revoke comes first so that any copy of the credential made before deletion stops working too.
     * Does nothing if no credential is stored.
     *
     * @throws SyncException if the credential could not be revoked or deleted.
     */
    void logout() throws SyncException;

    /**
     * Checks whether everything this store needs to authenticate is present locally, such as the client credentials
     * and the stored credential from {@link #login()}. A missing piece of either kind counts as not logged in.
     * <p>
     * This is a local check only, it never contacts the remote or starts the authentication flow. A stored credential
     * that has expired or been revoked remotely still counts as logged in, until the next {@link #fetch()} or
     * {@link #upload(String)} fails with a {@link SyncAuthenticationException}.
     *
     * @return {@code true} if everything needed to authenticate is present locally.
     */
    boolean isLoggedIn();
}

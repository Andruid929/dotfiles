package io.druidlabs.dotfiles.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.io.IOTasks;

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

    /**
     * @return the provider this store syncs with.
     */
    SyncProvider getSyncProvider();

    /**
     * Deletes the stored credential from {@link #tokensPath()} without revoking it with the remote first, so a copy
     * made before deletion would still work. Meant for {@code logout --force} when the remote can't be reached, and
     * for cleaning up after a provider switch. The client credentials in {@link #secureFilesPath()} are kept.
     * Does nothing if no credential is stored.
     *
     * @throws IOException if the credential folder exists but could not be deleted.
     */
    default void discardLocalCredential() throws IOException {
        if (Files.exists(tokensPath())) {
            IOTasks.deleteFolder(tokensPath());
        }
    }

    /**
     * @return the folder holding the credential stored by {@link #login()}, inside {@link #secureFilesPath()}.
     * This is the only folder that {@link #logout()} and {@link #discardLocalCredential()} delete.
     */
    default Path tokensPath() {
        return secureFilesPath().resolve("tokens");
    }

    /**
     * @return this provider's folder in {@link FilePaths#SYNC_SECURE_DIR}, holding its client credentials and the
     * {@link #tokensPath()} folder. It is restricted to the current user and never deleted by the app.
     */
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
     * Revokes the stored credential with the remote, then deletes it locally from {@link #tokensPath()}.
     * The revoke comes first so that any copy of the credential made before deletion stops working too, and the
     * local credential is kept if the revoke fails. A credential the remote reports as already invalid counts as
     * revoked. The client credentials are never deleted. Does nothing if no credential is stored.
     *
     * @throws SyncException if the credential could not be revoked, for example while offline, or could not be
     *                       deleted after being revoked.
     */
    void logout() throws SyncException;

    /**
     * Checks whether a credential stored by {@link #login()} is present locally. The client credentials are not
     * checked, a missing or invalid client credentials file is reported by the operations that need it.
     * <p>
     * This is a local check only, it never contacts the remote, starts the authentication flow or creates any folders.
     * A stored credential that has expired or been revoked remotely still counts as logged in, until the next
     * {@link #fetch()} or {@link #upload(String)} fails with a {@link SyncAuthenticationException}.
     *
     * @return {@code true} if a stored credential is present locally.
     */
    boolean isLoggedIn();
}

package io.druidlabs.dotfiles.sync.gdrive;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.User;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.sync.FetchResult;
import io.druidlabs.dotfiles.sync.RemoteStore;
import io.druidlabs.dotfiles.sync.SyncException;
import io.druidlabs.dotfiles.sync.SyncProvider;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;

/**
 * {@link RemoteStore} backed by Google Drive, using the {@code drive.file} scope so the app only sees files it created.
 * The client credentials come from the user's own {@code credentials.json}, loaded by {@link CredentialLoader}.
 */
public final class GoogleDriveRemoteStore implements RemoteStore {

    public static final JsonFactory GSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    private static final String USER_ID = "user";

    private final NetHttpTransport httpTransport = new NetHttpTransport();

    private final List<String> unloggedErrors = new ArrayList<>();

    private GoogleAuthorizationCodeFlow authFlow;

    public GoogleDriveRemoteStore() {
    }

    private GoogleAuthorizationCodeFlow getAuthCodeFlow() throws IOException, SyncException {
        GoogleClientSecrets secrets = CredentialLoader.load(secureFilesPath());

        var dataStoreFactory = new FileDataStoreFactory(tokensPath().toFile());
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, GSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();
    }

    private @Nullable StoredCredential loadStoredCredential() throws IOException {
        if (Files.notExists(tokensPath())) {
            return null;
        }

        var dataStoreFactory = new FileDataStoreFactory(tokensPath().toFile());

        DataStore<StoredCredential> credentialDataStore = StoredCredential.getDefaultDataStore(dataStoreFactory);

        return credentialDataStore.get(USER_ID);
    }

    /**
     * Returns the errors from checks that can't throw, such as {@link #isLoggedIn()}, and clears them, so a second
     * call only returns errors raised since the first.
     *
     * @return the errors collected since the last call, oldest first.
     */
    public @NotNull @Unmodifiable List<String> drainUnloggedErrors() {
        List<String> copy = List.copyOf(unloggedErrors);

        unloggedErrors.clear();

        return copy;
    }

    /**
     * Asks Google to revoke {@code token}. A 200 response, or a 400 with {@code invalid_token} for a token that is
     * already invalid, counts as revoked.
     *
     * @throws SyncException if Google answered with anything else.
     * @throws IOException   if the request failed, for example while offline.
     */
    private void revokeToken(String token) throws SyncException, IOException {
        GenericUrl revokeUrl = new GenericUrl("https://oauth2.googleapis.com/revoke");

        UrlEncodedContent content = new UrlEncodedContent(Map.of("token", token));

        HttpRequest request = httpTransport.createRequestFactory()
                .buildPostRequest(revokeUrl, content);

        int timeoutInMs = 10_000;

        request.setConnectTimeout(timeoutInMs);
        request.setReadTimeout(timeoutInMs);
        request.setThrowExceptionOnExecuteError(false);

        HttpResponse response = request.execute();

        try {
            int responseCode = response.getStatusCode();

            boolean revoked = false;

            if (responseCode == 200) {
                revoked = true;

            } else if (responseCode == 400) {
                String body = response.parseAsString();

                String error = null;

                if (!body.isBlank()) {
                    Object value = GSON_FACTORY.fromString(body, GenericJson.class).get("error");

                    error = (value == null) ? null : value.toString();
                }

                if ("invalid_token".equals(error)) {
                    revoked = true;
                }
            }

            if (!revoked) {
                throw new SyncException(interpolate("Login revocation failed with code {}", responseCode));
            }

        } finally {
            response.disconnect();
        }
    }

    @Override
    public SyncProvider getSyncProvider() {
        return SyncProvider.GOOGLE_DRIVE;
    }

    @Override
    public @NotNull Optional<FetchResult> fetch() throws SyncException {
        return Optional.empty();
    }

    @Override
    public @NotNull String upload(String content) throws SyncException {
        return "";
    }

    @Override
    public void login() throws SyncException {
        try {
            IOTasks.createSyncAuthFolder(secureFilesPath());

        } catch (IOException e) {
            throw new SyncException("Failed to create sync folder", e);
        }

        try {
            if (authFlow == null) {
                authFlow = getAuthCodeFlow();
            }

        } catch (IOException e) {
            throw new SyncException("Failed to establish OAuth flow", e);
        }

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .build();

        try {
            new AuthorizationCodeInstalledApp(authFlow, receiver)
                    .authorize(USER_ID);

        } catch (IOException e) {
            throw new SyncException("Failed to authorise login", e);
        }
    }

    @Override
    public void logout() throws SyncException {
        if (Files.notExists(tokensPath())) {
            UserNotification.notifyUser("You are not logged in");

            return;
        }

        StoredCredential credential;

        try {
            credential = loadStoredCredential();

        } catch (IOException e) {
            throw new SyncException("Failed to logout, error occurred loading local credential", e);
        }

        if (credential == null) {
            UserNotification.notifyUser("You are not logged in");

            try {
                discardLocalCredential();

            } catch (IOException e) {
                String message = ExceptionUtil.describeIOFailure(e,
                        "Failed to delete local credential because {}",
                        "Failed to delete local credential");

                throw new SyncException(message, e);
            }

            return;
        }

        String token;

        if (credential.getRefreshToken() == null) {
            if (credential.getAccessToken() == null) {

                UserNotification.notifyUser("Online revocation skipped due to missing tokens, deleting local credential");

                try {
                    discardLocalCredential();

                } catch (IOException e) {
                    String message = ExceptionUtil.describeIOFailure(e,
                            "Failed to delete local credential because {}",
                            "Failed to delete local credential");

                    throw new SyncException(message, e);
                }

                return;
            }

            token = credential.getAccessToken();
        } else {
            token = credential.getRefreshToken();
        }

        try {
            revokeToken(token);

            UserNotification.notifyUser("Online credential revoked successfully");

        } catch (IOException e) {
            String message = "Could not revoke login";

            if (e instanceof UnknownHostException || e instanceof ConnectException) {
                message = "Could not revoke login because you are offline. Use --force to delete local credential without revoking the online credential";

            } else if (e instanceof SocketTimeoutException) {
                message = "Could not revoke login, you are on a slow network. Try again later";
            }

            throw new SyncException(message, e);
        }

        try {
            discardLocalCredential();

            UserNotification.notifyUser("Local credential deleted successfully");

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e,
                    "Login revoked but failed to delete local credential because {}",
                    "Login revoked but failed to delete local credential");

            throw new SyncException(message, e);
        }
    }

    @Override
    public boolean isLoggedIn() {
        try {
            return loadStoredCredential() != null;

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e,
                    "Failed to load stored credential checking for login because {}",
                    "Failed to load stored credential checking for login");

            unloggedErrors.add(message);

            return false;
        }
    }
}

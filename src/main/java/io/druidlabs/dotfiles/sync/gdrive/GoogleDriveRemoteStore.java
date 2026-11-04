package io.druidlabs.dotfiles.sync.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.sync.FetchResult;
import io.druidlabs.dotfiles.sync.RemoteStore;
import io.druidlabs.dotfiles.sync.SyncException;
import io.druidlabs.dotfiles.sync.SyncProvider;
import io.druidlabs.dotfiles.util.UserNotification;

public final class GoogleDriveRemoteStore implements RemoteStore {

    public static final JsonFactory GSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    private static final String USER_ID = "user";

    private final NetHttpTransport httpTransport = new NetHttpTransport();

    private final List<String> unloggedErrors = new ArrayList<>();

    private GoogleAuthorizationCodeFlow authFLow;

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

    public List<String> getUnloggedErrors() {
        return unloggedErrors;
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
            if (authFLow == null) {
                authFLow = getAuthCodeFlow();
            }

        } catch (IOException e) {
            throw new SyncException("Failed to establish OAuth flow", e);
        }

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .build();

        try {
            new AuthorizationCodeInstalledApp(authFLow, receiver)
                    .authorize(USER_ID);

        } catch (IOException e) {
            throw new SyncException("Failed to authorise login", e);
        }
    }

    @Override
    public void logout() throws SyncException {
        Credential credential;

        try {
            if (authFLow == null) {
                authFLow = getAuthCodeFlow();
            }

            credential = authFLow.loadCredential(USER_ID);

        } catch (IOException e) {
            throw new SyncException("Failed to logout", e);
        }

        if (credential == null) {
            UserNotification.notifyUser("You're already logged out");

            if (Files.exists(tokensPath())) {
                try {
                    IOTasks.deleteFolder(tokensPath());
                } catch (IOException _) {
                }
            }

            return;
        }

        String token;

        if (credential.getRefreshToken() == null) {
            if (credential.getAccessToken() == null) {
                UserNotification.notifyUser("Online revocation bypassed due to missing tokens, deleting local cache");
            }

            token = credential.getAccessToken();
        } else {
            token = credential.getRefreshToken();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/revoke"))
                .timeout(Duration.of(10, ChronoUnit.SECONDS))

                .build();
    }

    @Override
    public boolean isLoggedIn() {
        try {

            if (authFLow == null) {
                authFLow = getAuthCodeFlow();
            }

            return authFLow.loadCredential(USER_ID) != null;

        } catch (IOException _) {
            return false;

        } catch (SyncException e) {
            unloggedErrors.add(e.getMessage());

            return false;
        }
    }
}

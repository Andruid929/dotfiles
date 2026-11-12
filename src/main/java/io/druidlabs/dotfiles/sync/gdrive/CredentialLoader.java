package io.druidlabs.dotfiles.sync.gdrive;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.JsonFactory;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import io.druidlabs.dotfiles.sync.exceptions.MissingCredentialsException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;
import io.druidlabs.dotfiles.util.ExceptionUtil;

/**
 * Reads the Google OAuth client credentials the user downloaded from the Google Cloud console.
 * The file must be a "Desktop app" client, saved as {@code credentials.json}.
 */
public final class CredentialLoader {

    /**
     * The name the user's client credentials file must have.
     */
    public static final String CREDENTIALS_FILENAME = "credentials.json";

    private CredentialLoader() {
        //Utility classes cannot be instantiated
    }

    /**
     * @param gDriveFolderPath the folder holding the credentials file.
     * @return the parsed client secrets, which always hold an {@code installed} (desktop client) section.
     * @throws MissingCredentialsException if the file is missing.
     * @throws SyncException               if the file could not be read, is not valid JSON, or is not a
     *                                     desktop OAuth client file.
     */
    public static @NotNull GoogleClientSecrets load(@NotNull Path gDriveFolderPath) throws SyncException {
        Path credentialsJsonPath = gDriveFolderPath.resolve(CREDENTIALS_FILENAME);

        if (Files.notExists(credentialsJsonPath)) {
            String message = interpolate("Credentials file '{}' not found. Refer to the documentation to see how to acquire this",
                    credentialsJsonPath);

            throw new MissingCredentialsException(message);
        }

        String contents;

        try {
            contents = Files.readString(credentialsJsonPath);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e, "Failed to load Google credentials because {}",
                    "Failed to load Google credentials");

            throw new SyncException(message, e);
        }

        JsonFactory factory = GoogleDriveRemoteStore.GSON_FACTORY;

        GoogleClientSecrets secrets;

        try {
            secrets = GoogleClientSecrets.load(factory, new StringReader(contents));

        } catch (IOException e) {
            throw new SyncException("Credentials file has invalid JSON", e);
        }

        if (secrets.getInstalled() != null) {
            return secrets;

        } else if (secrets.getWeb() != null) {
            throw new SyncException("Dotfiles needs desktop client credentials, found web client credentials");

        } else {
            throw new SyncException("Credentials file is not an OAuth client file, redownload the file");
        }
    }

}

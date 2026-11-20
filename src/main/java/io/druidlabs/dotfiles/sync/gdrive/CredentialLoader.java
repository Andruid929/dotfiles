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

    /**
     * The guide to creating the client credentials, linked from the missing-file message. It only resolves once the
     * guide is on {@code master}, and builds already released keep printing it, so don't rename or move the guide.
     */
    public static final String SETUP_GUIDE_URL = "https://github.com/Andruid929/dotfiles/blob/master/docs/google-drive-setup.md";

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
            String message = interpolate("Credentials file '{}' not found. Create a Desktop app OAuth client for the Google Drive API, then add its downloaded file with 'dotfiles login --credentials <path>'{}Setup guide: {}",
                    credentialsJsonPath, System.lineSeparator(), SETUP_GUIDE_URL);

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

        return parse(contents);
    }

    /**
     * Checks the contents of a client credentials file, wherever it was read from.
     *
     * @param contents the file's contents.
     * @return the parsed client secrets, which always hold an {@code installed} (desktop client) section.
     * @throws SyncException if the contents are not valid JSON, or not a desktop OAuth client file.
     */
    public static @NotNull GoogleClientSecrets parse(@NotNull String contents) throws SyncException {
        JsonFactory factory = GoogleDriveRemoteStore.GSON_FACTORY;

        GoogleClientSecrets secrets;

        try {
            secrets = GoogleClientSecrets.load(factory, new StringReader(contents));

        } catch (IOException | IllegalArgumentException e) {
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

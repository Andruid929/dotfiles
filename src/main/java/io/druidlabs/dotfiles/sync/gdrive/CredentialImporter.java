package io.druidlabs.dotfiles.sync.gdrive;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.sync.CredentialImportResult;
import io.druidlabs.dotfiles.sync.exceptions.CredentialImportException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;
import io.druidlabs.dotfiles.util.ExceptionUtil;

/**
 * Copies the client credentials file the user downloaded from the Google Cloud console into place as
 * {@code credentials.json}. The file is read once, and the bytes that pass the checks are the ones written, so a file
 * changed in between can't slip through.
 */
final class CredentialImporter {

    /**
     * The name pattern of the file Google downloads, {@code client_secret_<id>.apps.googleusercontent.com.json}.
     */
    private static final String DOWNLOAD_PATTERN = "client_secret_*.json";

    /**
     * Anything bigger is the wrong file: a real client credentials file is well under 1 KB.
     */
    private static final long MAX_SIZE_IN_BYTES = 64 * 1024;

    private CredentialImporter() {
        //Utility classes cannot be instantiated
    }

    /**
     * @param gDriveFolderPath the folder the credentials file belongs in, created and restricted if needed.
     * @param source           the downloaded file, or a folder holding exactly one file named like a Google download.
     * @param replace          whether a different credentials file already in place may be overwritten.
     * @return what was done.
     * @throws CredentialImportException if {@code source} can't be used, for a reason the user can fix.
     * @throws SyncException             if the credentials file in place could not be read or written.
     */
    static @NotNull CredentialImportResult importFrom(@NotNull Path gDriveFolderPath, @NotNull Path source,
                                                      boolean replace) throws SyncException {
        Path sourceFile = resolveSourceFile(source);

        byte[] contents = readSource(sourceFile);

        try {
            CredentialLoader.parse(new String(contents, StandardCharsets.UTF_8));

        } catch (SyncException e) {
            String message = interpolate("Can't use '{}'. {}", sourceFile, e.getMessage());

            throw new CredentialImportException(message, e);
        }

        Path credentialsJsonPath = gDriveFolderPath.resolve(CredentialLoader.CREDENTIALS_FILENAME);

        boolean credentialsInPlace = Files.exists(credentialsJsonPath);

        if (credentialsInPlace) {
            byte[] existingContents;

            try {
                existingContents = Files.readAllBytes(credentialsJsonPath);

            } catch (IOException e) {
                String message = ExceptionUtil.describeIOFailure(e,
                        "Failed to compare with the credentials file in place because {}",
                        "Failed to compare with the credentials file in place");

                throw new SyncException(message, e);
            }

            if (Arrays.equals(contents, existingContents)) {
                return CredentialImportResult.EXISTS;
            }

            if (!replace) {
                return CredentialImportResult.REFUSED;
            }
        }

        try {
            IOTasks.createSyncAuthFolder(gDriveFolderPath);

            Files.write(credentialsJsonPath, contents);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e,
                    "Failed to copy the credentials file into place because {}",
                    "Failed to copy the credentials file into place");

            throw new SyncException(message, e);
        }

        return credentialsInPlace ? CredentialImportResult.REPLACED : CredentialImportResult.COPIED;
    }

    /**
     * @param source the path the user gave.
     * @return {@code source} itself, or the one downloaded file inside it if it is a folder.
     * @throws CredentialImportException if {@code source} doesn't exist, or is a folder that doesn't hold exactly one
     *                                   downloaded file.
     */
    private static @NotNull Path resolveSourceFile(@NotNull Path source) throws CredentialImportException {
        if (Files.notExists(source)) {
            String message = interpolate("'{}' does not exist", source);

            throw new CredentialImportException(message);
        }

        if (!Files.isDirectory(source)) {
            return source;
        }

        List<Path> matches = new ArrayList<>();

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(source, DOWNLOAD_PATTERN)) {
            entries.forEach(matches::add);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e,
                    interpolate("Failed to look inside '{}' because {}", source),
                    interpolate("Failed to look inside '{}'", source));

            throw new CredentialImportException(message, e);
        }

        if (matches.isEmpty()) {
            throw new CredentialImportException(interpolate("No {} file found in '{}'. Give the path to the file itself",
                    DOWNLOAD_PATTERN, source));
        }

        if (matches.size() > 1) {
            throw new CredentialImportException(interpolate("Found {} {} files in '{}'. Give the path to the one to use",
                    matches.size(), DOWNLOAD_PATTERN, source));
        }

        return matches.getFirst();
    }

    /**
     * Reads the whole file, after checking it is a regular file small enough to be a credentials file. A named pipe,
     * for example, could otherwise block the read forever.
     *
     * @param sourceFile the file to read.
     * @return its contents.
     * @throws CredentialImportException if it isn't a regular file, is too big, or could not be read.
     */
    private static byte @NotNull [] readSource(@NotNull Path sourceFile) throws CredentialImportException {
        if (!Files.isRegularFile(sourceFile)) {
            String message = interpolate("'{}' is not a regular file", sourceFile);

            throw new CredentialImportException(message);
        }

        try {
            long size = Files.size(sourceFile);

            if (size > MAX_SIZE_IN_BYTES) {
                String message = interpolate("'{}' is {} bytes, too big to be a credentials file", sourceFile, size);

                throw new CredentialImportException(message);
            }

            return Files.readAllBytes(sourceFile);

        } catch (IOException e) {
            String fallback = interpolate("Failed to read '{}'", sourceFile);

            String message = ExceptionUtil.describeIOFailure(e,
                    "Failed to read '" + sourceFile + "' because {}",
                    fallback);

            throw new CredentialImportException(message, e);
        }
    }
}

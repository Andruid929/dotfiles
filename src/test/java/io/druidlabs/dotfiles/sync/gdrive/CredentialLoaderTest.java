package io.druidlabs.dotfiles.sync.gdrive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.druidlabs.dotfiles.sync.exceptions.MissingCredentialsException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;

/**
 * {@code status} and {@code login} both load the file in place. Anything other than a {@link SyncException} skips
 * their handling and ends the command with a stack trace.
 */
class CredentialLoaderTest {

    @TempDir
    Path gDriveFolder;

    private Path placeByHand(String contents) throws IOException {
        return Files.writeString(gDriveFolder.resolve(CredentialLoader.CREDENTIALS_FILENAME), contents);
    }

    @Test
    void load_downloadPlacedByHand_shouldLoad() throws IOException, SyncException {
        placeByHand(CredentialImporterTest.DESKTOP_CLIENT);

        assertEquals("GOCSPX-first", CredentialLoader.load(gDriveFolder).getInstalled().getClientSecret());
    }

    @Test
    void load_downloadNotRenamed_shouldThrowMissingCredentials() throws IOException {
        Files.writeString(gDriveFolder.resolve(CredentialImporterTest.DOWNLOAD_NAME), CredentialImporterTest.DESKTOP_CLIENT);

        assertThrows(MissingCredentialsException.class, () -> CredentialLoader.load(gDriveFolder));
    }

    @Test
    void load_emptyFileInPlace_shouldThrowSyncException() throws IOException {
        placeByHand("");

        assertThrows(SyncException.class, () -> CredentialLoader.load(gDriveFolder));
    }

    @Test
    void load_fileEmptiedToBrackets_shouldThrowSyncException() throws IOException {
        placeByHand("[]");

        assertThrows(SyncException.class, () -> CredentialLoader.load(gDriveFolder));
    }

    @Test
    void load_fileCutOff_shouldThrowSyncException() throws IOException {
        String cutOff = CredentialImporterTest.DESKTOP_CLIENT.substring(0, 80);

        placeByHand(cutOff);

        assertThrows(SyncException.class, () -> CredentialLoader.load(gDriveFolder));
    }

    @Test
    void load_credentialsFolderInsteadOfFile_shouldThrowSyncException() throws IOException {
        Files.createDirectory(gDriveFolder.resolve(CredentialLoader.CREDENTIALS_FILENAME));

        assertThrows(SyncException.class, () -> CredentialLoader.load(gDriveFolder));
    }
}

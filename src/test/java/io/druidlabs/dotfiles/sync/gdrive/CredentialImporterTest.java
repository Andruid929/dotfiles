package io.druidlabs.dotfiles.sync.gdrive;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;

import io.druidlabs.dotfiles.sync.CredentialImportResult;
import io.druidlabs.dotfiles.sync.exceptions.CredentialImportException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;

class CredentialImporterTest {

    /**
     * Shaped like Google's Desktop app download, which is a single line.
     */
    static final String DESKTOP_CLIENT = """
            {"installed":{"client_id":"1234-abcd.apps.googleusercontent.com","project_id":"dotfiles-4821","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_secret":"GOCSPX-first","redirect_uris":["http://localhost"]}}""";

    /**
     * The same client after "Add secret": only the secret differs.
     */
    static final String DESKTOP_CLIENT_NEW_SECRET = DESKTOP_CLIENT.replace("GOCSPX-first", "GOCSPX-second");

    static final String WEB_CLIENT = """
            {"web":{"client_id":"1234-abcd.apps.googleusercontent.com","project_id":"dotfiles-4821","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","client_secret":"GOCSPX-web","redirect_uris":["https://example.com/callback"]}}""";

    static final String SERVICE_ACCOUNT_KEY = """
            {"type":"service_account","project_id":"dotfiles-4821","private_key_id":"abc","private_key":"-----BEGIN PRIVATE KEY-----\\nMIIE\\n-----END PRIVATE KEY-----\\n","client_email":"sync@dotfiles-4821.iam.gserviceaccount.com","client_id":"1234"}""";

    static final String DOWNLOAD_NAME = "client_secret_1234-abcd.apps.googleusercontent.com.json";

    @TempDir
    Path tempDir;

    Path downloads;

    /**
     * Where the credentials file belongs. Not created up front, as on a first login.
     */
    Path gDriveFolder;

    @BeforeEach
    void setUp() throws IOException {
        downloads = Files.createDirectory(tempDir.resolve("Downloads"));

        gDriveFolder = tempDir.resolve("secure").resolve("GDrive");
    }

    private Path download(String name, String contents) throws IOException {
        return Files.writeString(downloads.resolve(name), contents);
    }

    private Path credentialsInPlace() {
        return gDriveFolder.resolve(CredentialLoader.CREDENTIALS_FILENAME);
    }

    @Test
    void importFrom_downloadsFolderWithOtherFiles_shouldPickTheGoogleDownload() throws IOException, SyncException {
        download("package.json", "{\"name\": \"something else\"}");
        download("report.pdf", "%PDF-1.7");
        download(DOWNLOAD_NAME, DESKTOP_CLIENT);

        CredentialImportResult result = CredentialImporter.importFrom(gDriveFolder, downloads, false);

        assertEquals(CredentialImportResult.COPIED, result);
        assertEquals(DESKTOP_CLIENT, Files.readString(credentialsInPlace()));
    }

    @Test
    void importFrom_folderAfterDownloadingTwice_shouldRefuseToGuess() throws IOException {
        download(DOWNLOAD_NAME, DESKTOP_CLIENT);
        download("client_secret_1234-abcd.apps.googleusercontent.com (1).json", DESKTOP_CLIENT_NEW_SECRET);

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, downloads, false));

        assertFalse(Files.exists(gDriveFolder));
    }

    @Test
    void importFrom_folderWithoutDownload_shouldThrow() throws IOException {
        download("credentials.json", DESKTOP_CLIENT);

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, downloads, false));
    }

    @Test
    void importFrom_mistypedPath_shouldThrow() {
        Path typo = downloads.resolve("client_secret_1234.json");

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, typo, false));
    }

    @Test
    void importFrom_sameFileAgain_shouldLeaveFileInPlaceUntouched() throws IOException, SyncException {
        Path source = download(DOWNLOAD_NAME, DESKTOP_CLIENT);

        CredentialImporter.importFrom(gDriveFolder, source, false);

        FileTime earlier = FileTime.from(Instant.parse("2026-01-01T00:00:00Z"));

        Files.setLastModifiedTime(credentialsInPlace(), earlier);

        CredentialImportResult result = CredentialImporter.importFrom(gDriveFolder, source, false);

        assertEquals(CredentialImportResult.EXISTS, result);
        assertEquals(earlier, Files.getLastModifiedTime(credentialsInPlace()));
    }

    @Test
    void importFrom_newSecretWithoutForce_shouldKeepWorkingFile() throws IOException, SyncException {
        CredentialImporter.importFrom(gDriveFolder, download(DOWNLOAD_NAME, DESKTOP_CLIENT), false);

        Path newDownload = download("client_secret_1234-abcd.apps.googleusercontent.com (1).json", DESKTOP_CLIENT_NEW_SECRET);

        CredentialImportResult result = CredentialImporter.importFrom(gDriveFolder, newDownload, false);

        assertEquals(CredentialImportResult.REFUSED, result);
        assertEquals(DESKTOP_CLIENT, Files.readString(credentialsInPlace()));
    }

    @Test
    void importFrom_newSecretWithForce_shouldReplaceFile() throws IOException, SyncException {
        CredentialImporter.importFrom(gDriveFolder, download(DOWNLOAD_NAME, DESKTOP_CLIENT), false);

        Path newDownload = download("client_secret_1234-abcd.apps.googleusercontent.com (1).json", DESKTOP_CLIENT_NEW_SECRET);

        CredentialImportResult result = CredentialImporter.importFrom(gDriveFolder, newDownload, true);

        assertEquals(CredentialImportResult.REPLACED, result);
        assertEquals(DESKTOP_CLIENT_NEW_SECRET, Files.readString(credentialsInPlace()));
    }

    @Test
    void importFrom_webClient_shouldWriteNothing() throws IOException {
        Path source = download(DOWNLOAD_NAME, WEB_CLIENT);

        var e = assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, source, false));

        assertTrue(e.getMessage().contains("web client"), e.getMessage());
        assertFalse(Files.exists(gDriveFolder));
    }

    @Test
    void importFrom_webClientWithForce_shouldKeepWorkingFile() throws IOException, SyncException {
        CredentialImporter.importFrom(gDriveFolder, download(DOWNLOAD_NAME, DESKTOP_CLIENT), false);

        Path webDownload = download("client_secret_web.json", WEB_CLIENT);

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, webDownload, true));

        assertEquals(DESKTOP_CLIENT, Files.readString(credentialsInPlace()));
    }

    @Test
    void importFrom_serviceAccountKey_shouldThrow() throws IOException {
        Path source = download("dotfiles-4821-abc123.json", SERVICE_ACCOUNT_KEY);

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, source, false));

        assertFalse(Files.exists(gDriveFolder));
    }

    /**
     * An interrupted download, or {@code touch}, leaves an empty file behind.
     */
    @Test
    void importFrom_emptyFile_shouldThrow() throws IOException {
        Path source = download(DOWNLOAD_NAME, "");

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, source, false));

        assertFalse(Files.exists(gDriveFolder));
    }

    @Test
    void importFrom_consolePageSavedInsteadOfFile_shouldThrow() throws IOException {
        Path source = download(DOWNLOAD_NAME, "<!DOCTYPE html><html><head><title>Clients – Google Auth Platform</title>");

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, source, false));
    }

    @Test
    void importFrom_wrongBigFile_shouldThrow() throws IOException {
        Path source = downloads.resolve("client_secret_backup.json");

        Files.write(source, new byte[200 * 1024]);

        assertThrows(CredentialImportException.class, () -> CredentialImporter.importFrom(gDriveFolder, source, false));
    }

    /**
     * Notepad on Windows can save UTF-8 with a byte order mark.
     */
    @Test
    void importFrom_fileResavedWithByteOrderMark_shouldStillLoad() throws IOException, SyncException {
        Path source = download(DOWNLOAD_NAME, "﻿" + DESKTOP_CLIENT);

        CredentialImporter.importFrom(gDriveFolder, source, false);

        CredentialLoader.load(gDriveFolder);
    }

    @Test
    void importFrom_shouldWriteExactBytesDownloaded() throws IOException, SyncException {
        String withCrlf = DESKTOP_CLIENT.replace("{\"installed\"", "{\r\n  \"installed\"") + "\r\n";

        Path source = download(DOWNLOAD_NAME, withCrlf);

        CredentialImporter.importFrom(gDriveFolder, source, false);

        assertArrayEquals(withCrlf.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(credentialsInPlace()));
    }

    @Test
    void importFrom_firstLogin_shouldMakeFolderPrivate() throws IOException, SyncException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        CredentialImporter.importFrom(gDriveFolder, download(DOWNLOAD_NAME, DESKTOP_CLIENT), false);

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(gDriveFolder)));
    }

    @Test
    void importFrom_folderLeftOpenByHand_shouldMakeItPrivateAgain() throws IOException, SyncException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        Files.createDirectories(gDriveFolder);

        Files.setPosixFilePermissions(gDriveFolder, PosixFilePermissions.fromString("rwxr-xr-x"));

        CredentialImporter.importFrom(gDriveFolder, download(DOWNLOAD_NAME, DESKTOP_CLIENT), false);

        assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(gDriveFolder)));
    }
}

package io.druidlabs.dotfiles.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;

import io.druidlabs.dotfiles.backup.SyncOperation;
import io.druidlabs.dotfiles.backup.UndoRecord;
import io.druidlabs.dotfiles.sync.SyncSnapshot;

class SyncSnapshotIOTest {

    /**
     * The pinned hash from {@code HashHandlerTest}, standing in for any dotfiles.
     */
    static final String HASH = "7f16cf86d1391b63b64cf5eb396809832a4ca2c7ffece1d07e5b873aa95d3882";

    /**
     * The hash of an empty setup, as after a first pull onto a fresh install.
     */
    static final String EMPTY_HASH = "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945";

    static final Instant PERFORMED_AT = Instant.parse("2026-11-25T08:30:00Z");

    @TempDir
    Path appFolder;

    Path syncFile;

    final SyncSnapshotIO snapshotIO = new SyncSnapshotIO();

    @BeforeEach
    void setUp() {
        syncFile = appFolder.resolve(".sync");
    }

    private Path tempFile() {
        return appFolder.resolve(".sync.tmp");
    }

    @Test
    void readSnapshot_beforeFirstSync_shouldBeEmpty() throws IOException {
        assertEquals(Optional.empty(), snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void saveThenRead_withNothingToUndo_shouldGiveTheSameSnapshot() throws IOException {
        SyncSnapshot snapshot = new SyncSnapshot("GDrive", HASH, null);

        snapshotIO.saveSnapshot(syncFile, snapshot);

        assertEquals(Optional.of(snapshot), snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void saveThenRead_afterPush_shouldKeepEverythingUndoNeeds() throws IOException {
        UndoRecord push = new UndoRecord(SyncOperation.PUSH, PERFORMED_AT, EMPTY_HASH, EMPTY_HASH, "a3f1c09b2e");
        SyncSnapshot snapshot = new SyncSnapshot("GDrive", HASH, push);

        snapshotIO.saveSnapshot(syncFile, snapshot);

        assertEquals(Optional.of(snapshot), snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void saveThenRead_afterFirstPush_shouldKeepTheMissingBackup() throws IOException {
        UndoRecord firstPush = new UndoRecord(SyncOperation.PUSH, PERFORMED_AT, null, null, "a3f1c09b2e");
        SyncSnapshot snapshot = new SyncSnapshot("GDrive", HASH, firstPush);

        snapshotIO.saveSnapshot(syncFile, snapshot);

        UndoRecord readBack = snapshotIO.readSnapshot(syncFile).orElseThrow().undo();

        assertEquals(firstPush, readBack);
    }

    @Test
    void readSnapshot_fromAnotherProvider_shouldReturnItAsStored() throws IOException {
        //Deciding that it doesn't count is the comparison's job, not the reader's
        Files.writeString(syncFile, """
                {"providerId":"Dropbox","hash":"%s"}""".formatted(HASH));

        assertEquals("Dropbox", snapshotIO.readSnapshot(syncFile).orElseThrow().providerId());
    }

    @Test
    void readSnapshot_ofFileIndentedByHandOnWindows_shouldLoad() throws IOException {
        Files.writeString(syncFile, """
                {\r
                  "providerId" : "GDrive",\r
                  "hash" : "%s",\r
                  "undo" : {\r
                    "operation" : "PULL",\r
                    "performedAt" : "2026-11-25T08:30:00Z",\r
                    "backupHash" : "%s"\r
                  }\r
                }\r
                """.formatted(HASH, EMPTY_HASH));

        UndoRecord undo = snapshotIO.readSnapshot(syncFile).orElseThrow().undo();

        assertEquals(SyncOperation.PULL, undo.operation());
        assertEquals(EMPTY_HASH, undo.backupHash());
    }

    @Test
    void readSnapshot_withFieldsFromNewerVersion_shouldLoad() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s","lastChecked":"2027-01-01T00:00:00Z","undo":{"operation":"PULL","performedAt":"2026-11-25T08:30:00Z","backupHash":"%s","backupSize":1024}}"""
                .formatted(HASH, EMPTY_HASH));

        assertEquals(HASH, snapshotIO.readSnapshot(syncFile).orElseThrow().hash());
    }

    @Test
    void readSnapshot_ofFileCutOffMidWrite_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"7f16cf86d1391b63""");

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_ofEmptiedFile_shouldThrowJsonProcessingException() throws IOException {
        //Never written blank, so a blank .sync means something went wrong rather than "no snapshot"
        for (String blank : new String[] {"", "   ", "\r\n\r\n"}) {
            Files.writeString(syncFile, blank);

            assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile),
                    () -> "Accepted a blank file of " + blank.length() + " characters");
        }
    }

    @Test
    void readSnapshot_withEndOfOlderSnapshotLeftAfterIt_shouldThrowJsonProcessingException() throws IOException {
        //A shorter snapshot written over a longer one without truncating. The mapper rejects trailing tokens, since
        //Jackson would otherwise read the first value and ignore the rest
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s"}","undo":{"operation":"PULL"}}""".formatted(HASH));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withNewlineAnEditorAddedAtTheEnd_shouldLoad() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s"}
                """.formatted(HASH));

        assertEquals(HASH, snapshotIO.readSnapshot(syncFile).orElseThrow().hash());
    }

    @Test
    void readSnapshot_ofFileOverwrittenWithSomethingElse_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, "export PATH=\"$HOME/.local/bin:$PATH\"\n");

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withProviderDeletedByHand_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, """
                {"hash":"%s"}""".formatted(HASH));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withUndoTimeDeletedByHand_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s","undo":{"operation":"PULL","backupHash":"%s"}}"""
                .formatted(HASH, EMPTY_HASH));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withHashRetypedInUpperCase_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s"}""".formatted(HASH.toUpperCase()));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withBackupHashPointingOutsideBackups_shouldThrowJsonProcessingException() throws IOException {
        //The backup's location comes from its hash, so a path here must never be accepted
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s","undo":{"operation":"PULL","performedAt":"2026-11-25T08:30:00Z","backupHash":"../.files"}}"""
                .formatted(HASH));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_withOperationRetypedInLowerCase_shouldThrowJsonProcessingException() throws IOException {
        Files.writeString(syncFile, """
                {"providerId":"GDrive","hash":"%s","undo":{"operation":"pull","performedAt":"2026-11-25T08:30:00Z","backupHash":"%s"}}"""
                .formatted(HASH, EMPTY_HASH));

        assertThrows(JsonProcessingException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void readSnapshot_whenSyncIsAFolder_shouldThrowRatherThanCountAsFirstSync() throws IOException {
        Files.createDirectory(syncFile);

        assertThrows(IOException.class, () -> snapshotIO.readSnapshot(syncFile));
    }

    @Test
    void saveSnapshot_overPreviousSnapshot_shouldReplaceItAndLeaveNoTempFile() throws IOException {
        snapshotIO.saveSnapshot(syncFile, new SyncSnapshot("GDrive", EMPTY_HASH, null));

        SyncSnapshot newer = new SyncSnapshot("GDrive", HASH, null);

        snapshotIO.saveSnapshot(syncFile, newer);

        assertEquals(Optional.of(newer), snapshotIO.readSnapshot(syncFile));
        assertFalse(Files.exists(tempFile()));
    }

    @Test
    void saveSnapshot_overTempFileLeftByCrash_shouldWriteACleanFile() throws IOException {
        SyncSnapshot snapshot = new SyncSnapshot("GDrive", HASH, null);

        snapshotIO.saveSnapshot(appFolder.resolve("expected"), snapshot);

        String expected = Files.readString(appFolder.resolve("expected"));

        //Longer than the new snapshot, so any of it left behind would follow the new JSON. Reading the file back
        //wouldn't notice, since Jackson stops after the first complete value, so compare the bytes
        Files.writeString(tempFile(), "x".repeat(4096));

        snapshotIO.saveSnapshot(syncFile, snapshot);

        assertEquals(expected, Files.readString(syncFile));
        assertFalse(Files.exists(tempFile()));
    }

    @Test
    void saveSnapshot_whenMoveIntoPlaceFails_shouldLeaveNoTempFile() throws IOException {
        //A folder with something in it can't be replaced, so the temp file is written but the move fails
        Files.createDirectories(syncFile.resolve("something"));

        assertThrows(IOException.class, () -> snapshotIO.saveSnapshot(syncFile, new SyncSnapshot("GDrive", HASH, null)));

        assertFalse(Files.exists(tempFile()));
    }

    @Test
    void saveSnapshot_whenFolderCannotBeWritten_shouldKeepPreviousSnapshot() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        SyncSnapshot previous = new SyncSnapshot("GDrive", EMPTY_HASH, null);

        snapshotIO.saveSnapshot(syncFile, previous);

        Files.setPosixFilePermissions(appFolder, PosixFilePermissions.fromString("r-x------"));

        try {
            assumeTrue(!Files.isWritable(appFolder), "Running with permissions that ignore the folder's mode");

            assertThrows(IOException.class,
                    () -> snapshotIO.saveSnapshot(syncFile, new SyncSnapshot("GDrive", HASH, null)));

        } finally {
            Files.setPosixFilePermissions(appFolder, PosixFilePermissions.fromString("rwx------"));
        }

        assertEquals(Optional.of(previous), snapshotIO.readSnapshot(syncFile));
        assertFalse(Files.exists(tempFile()));
    }

    @Test
    void saveSnapshot_shouldWriteOneLineOfJson() throws IOException {
        snapshotIO.saveSnapshot(syncFile, new SyncSnapshot("GDrive", HASH, null));

        String written = Files.readString(syncFile);

        assertTrue(written.startsWith("{") && written.endsWith("}"), written);
        assertFalse(written.contains("\n"), written);
    }
}

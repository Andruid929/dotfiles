package io.druidlabs.dotfiles.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

class IOTasksTest {

    @TempDir
    Path folder;

    /**
     * Creates a symlink, skipping the test where that isn't allowed (Windows without Developer Mode or admin rights).
     */
    private static void createLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);

        } catch (UnsupportedOperationException | FileSystemException e) {
            assumeTrue(false, "Can't create symlinks here: " + e);
        }
    }

    /**
     * A save file kept in a dotfiles repo or a synced folder and linked into the app folder.
     */
    @Test
    void atomicSave_toSymlinkedFile_shouldWriteThroughTheLinkAndKeepIt() throws IOException {
        Path repo = Files.createDirectories(folder.resolve("repo"));
        Path appFolder = Files.createDirectories(folder.resolve("app"));

        Path realFile = Files.writeString(repo.resolve("files.json"), "old");
        Path link = appFolder.resolve(".files");

        createLink(link, realFile);

        IOTasks.atomicSave(link, "new");

        assertTrue(Files.isSymbolicLink(link), "The link was replaced by a regular file");
        assertEquals("new", Files.readString(realFile));
        assertFalse(Files.exists(repo.resolve("files.json.tmp")));
        assertFalse(Files.exists(appFolder.resolve(".files.tmp")));
    }

    /**
     * A link left behind after the folder it pointed into was moved or deleted. Writing a regular file in its place
     * would hide that the saves are no longer going where the user thinks.
     */
    @Test
    void atomicSave_toLinkPointingAtNothing_shouldThrowAndLeaveTheLink() throws IOException {
        Path link = folder.resolve(".files");

        createLink(link, folder.resolve("moved-away").resolve("files.json"));

        assertThrows(NoSuchFileException.class, () -> IOTasks.atomicSave(link, "new"));

        assertTrue(Files.isSymbolicLink(link));
        assertFalse(Files.exists(folder.resolve(".files.tmp")));
    }

    /**
     * A save file locked down by hand because it holds a saved {@code .netrc} or {@code .npmrc}. Writing in place
     * kept the mode; a new file would get the default {@code rw-r--r--}.
     */
    @Test
    void atomicSave_overFileRestrictedToOwner_shouldKeepItRestricted() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));

        Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");

        Path file = Files.writeString(folder.resolve(".files"), "old");

        Files.setPosixFilePermissions(file, ownerOnly);

        IOTasks.atomicSave(file, "new");

        assertEquals("new", Files.readString(file));
        assertEquals(ownerOnly, Files.getPosixFilePermissions(file));
    }
}

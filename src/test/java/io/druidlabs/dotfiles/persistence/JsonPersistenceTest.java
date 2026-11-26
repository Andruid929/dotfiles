package io.druidlabs.dotfiles.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

class JsonPersistenceTest {

    @TempDir
    Path appFolder;

    Path saveFile;

    JsonPersistence persistence;

    final Dotfile vimrc = new Dotfile("vimrc", "set number\nset expandtab", "~", ".vimrc");

    final Dotfile bashrc = new Dotfile("bashrc", "alias ll='ls -l'", "~", ".bashrc");

    @BeforeEach
    void setUp() {
        saveFile = appFolder.resolve(".files");

        persistence = new JsonPersistence(saveFile);
    }

    private Path tempFile() {
        return appFolder.resolve(".files.tmp");
    }

    private static String contentsOf(Set<Dotfile> dotfiles, String key) {
        return dotfiles.stream()
                .filter(dotfile -> dotfile.getKey().equals(key))
                .findFirst()
                .orElseThrow()
                .getFileContents();
    }

    /**
     * The empty file {@code validateAppPaths()} creates on a first run.
     */
    @Test
    void load_ofFileCreatedOnFirstRun_shouldBeEmpty() throws IOException {
        Files.createFile(saveFile);

        assertTrue(persistence.load().isEmpty());
    }

    @Test
    void load_ofEmptyArrayRetypedByHand_shouldBeEmpty() throws IOException {
        Files.writeString(saveFile, "[ ]\r\n");

        assertTrue(persistence.load().isEmpty());
    }

    @Test
    void saveThenLoad_shouldGiveBackEveryDotfile() throws IOException {
        Files.createFile(saveFile);

        persistence.save(Set.of(vimrc, bashrc));

        Set<Dotfile> loaded = persistence.load();

        assertEquals(Set.of(vimrc, bashrc), loaded);
        assertEquals("set number\nset expandtab", contentsOf(loaded, "vimrc"));
        assertFalse(Files.exists(tempFile()));
    }

    /**
     * The save after {@code remove} is shorter than the one it replaces, which is when writing in place without
     * truncating left the end of the old save behind.
     */
    @Test
    void save_afterRemovingADotfile_shouldReplaceTheWholeFile() throws IOException {
        persistence.save(Set.of(vimrc, bashrc));

        persistence.save(Set.of(vimrc));

        assertEquals(Set.of(vimrc), persistence.load());
    }

    /**
     * After {@code remove} on the last dotfile, the next run has to start with nothing rather than fail to load.
     */
    @Test
    void save_afterRemovingEveryDotfile_shouldLoadAsEmpty() throws IOException {
        persistence.save(Set.of(vimrc));

        persistence.save(Set.of());

        assertTrue(persistence.load().isEmpty());
    }

    @Test
    void save_overTempFileLeftByCrash_shouldWriteACleanFile() throws IOException {
        new JsonPersistence(appFolder.resolve("expected")).save(Set.of(vimrc));

        String expected = Files.readString(appFolder.resolve("expected"));

        //Longer than the new save, so any of it left behind would follow the new JSON
        Files.writeString(tempFile(), "x".repeat(4096));

        persistence.save(Set.of(vimrc));

        assertEquals(expected, Files.readString(saveFile));
        assertFalse(Files.exists(tempFile()));
    }

    /**
     * Contents go through the disk as UTF-8 both ways, whatever the platform's default charset, so a dotfile saved
     * on one machine reads the same on another.
     */
    @Test
    void saveThenLoad_withNonAsciiContents_shouldKeepThemExactly() throws IOException {
        String contents = "# café ☕ 日本語\r\nPS1='→ '\n";

        persistence.save(Set.of(new Dotfile("prompt", contents, "~", ".promptrc")));

        assertEquals(contents, contentsOf(persistence.load(), "prompt"));
    }
}

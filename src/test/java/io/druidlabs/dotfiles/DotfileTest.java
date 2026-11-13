package io.druidlabs.dotfiles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import io.druidlabs.dotfiles.exceptions.BlankValueException;
import io.github.andruid929.leutils.wora.PathFinder;

class DotfileTest {

    @Test
    void constructor_shouldSetFields() {
        String path = Path.of("/path/to").toString();

        Dotfile dotfile = new Dotfile("mykey", "content", path, "file.txt");
        assertEquals("mykey", dotfile.getKey());
        assertEquals("content", dotfile.getFileContents());
        assertEquals("file.txt", dotfile.getFilename());
        assertEquals(path, dotfile.getFilePath());
    }

    @Test
    void constructor_shouldThrowExceptionIfKeyIsBlank() {
        assertThrows(BlankValueException.class, () -> new Dotfile("", "content", "/path/to", "file.txt"));
        assertThrows(BlankValueException.class, () -> new Dotfile("  ", "content", "/path/to", "file.txt"));
    }

    @Test
    void constructor_shouldThrowExceptionIfContentIsBlank() {
        assertThrows(BlankValueException.class, () -> new Dotfile("key", "", "/path/to", "file.txt"));
        assertThrows(BlankValueException.class, () -> new Dotfile("key", "  ", "/path/to", "file.txt"));
    }

    @Test
    void constructor_shouldThrowExceptionIfFilenameIsBlank() {
        assertThrows(BlankValueException.class, () -> new Dotfile("key", "content", "/path/to", ""));
        assertThrows(BlankValueException.class, () -> new Dotfile("key", "content", "/path/to", "  "));
    }

    @Test
    void constructor_shouldStoreHomePathShortened() {
        String path = Path.of(PathFinder.USER_HOME, ".config", "nvim").toString();

        Dotfile dotfile = new Dotfile("key", "content", path, "init.lua");
        assertEquals("~/.config/nvim", dotfile.getShortFilePath());
        assertEquals(path, dotfile.getFilePath());
        assertEquals(Path.of(PathFinder.USER_HOME, ".config", "nvim", "init.lua"), dotfile.getPathToFile());
    }

    @Test
    void constructor_shouldAcceptBackslashTilde() {
        Dotfile dotfile = new Dotfile("key", "content", "~\\.config\\nvim", "init.lua");
        assertEquals("~/.config/nvim", dotfile.getShortFilePath());
        assertEquals(Path.of(PathFinder.USER_HOME, ".config", "nvim").toString(), dotfile.getFilePath());
    }

    @Test
    void setFilePath_shouldStoreHomePathShortened() {
        Dotfile dotfile = new Dotfile("key", "content", "/old/path", "file.txt");

        dotfile.setFilePath(Path.of(PathFinder.USER_HOME, "dots").toString());
        assertEquals("~/dots", dotfile.getShortFilePath());
        assertEquals("file.txt", dotfile.getFilename());
    }

    @Test
    void setFilePath_shouldUpdatePath() {
        Dotfile dotfile = new Dotfile("key", "content", Path.of("/old/path").toString(), "file.txt");

        String newPath = Path.of("/new/path").toString();

        dotfile.setFilePath(newPath);
        assertEquals(newPath, dotfile.getFilePath());
        assertEquals("file.txt", dotfile.getFilename());
    }

    @Test
    void setFilePath_withBlank_ShouldEqualUserHome() {
        Dotfile dotfile = new Dotfile("key", "content", "/old/path", "file.txt");
        dotfile.setFilePath("");
        assertEquals(PathFinder.USER_HOME, dotfile.getFilePath());
        assertEquals("file.txt", dotfile.getFilename());
    }

    @Test
    void setFilename_shouldUpdateFilename() {
        String path = Path.of("/path").toString();

        Dotfile dotfile = new Dotfile("key", "content", path, "oldfile.txt");
        dotfile.setFilename("newfile.txt");
        assertEquals("newfile.txt", dotfile.getFilename());
        assertEquals(path, dotfile.getFilePath());
    }

    @Test
    void setKey_shouldUpdateKey() {
        Dotfile dotfile = new Dotfile("key1", "content", "/path", "file.txt");
        dotfile.setKey("key2");
        assertEquals("key2", dotfile.getKey());
    }

    @Test
    void setKey_shouldThrowExceptionIfKeyIsBlank() {
        Dotfile dotfile = new Dotfile("key", "content", "/path", "file.txt");
        assertThrows(BlankValueException.class, () -> dotfile.setKey(""));
        assertThrows(BlankValueException.class, () -> dotfile.setKey("  "));
        assertEquals("key", dotfile.getKey());
    }

    @Test
    void setFileContents_shouldUpdateContent() {
        Dotfile dotfile = new Dotfile("key", "content1", "/path", "file.txt");
        dotfile.setFileContents("content2");
        assertEquals("content2", dotfile.getFileContents());
    }

    @Test
    void setFileContents_shouldThrowExceptionIfContentIsBlank() {
        Dotfile dotfile = new Dotfile("key", "content", "/path", "file.txt");
        assertThrows(BlankValueException.class, () -> dotfile.setFileContents(""));
        assertThrows(BlankValueException.class, () -> dotfile.setFileContents("  "));
        assertEquals("content", dotfile.getFileContents());
    }

    @Test
    void equalsAndHashCode_shouldBeBasedOnKey() {
        Dotfile dotfile1 = new Dotfile("key1", "content1", "/path1", "file1.txt");
        Dotfile dotfile2 = new Dotfile("key1", "content2", "/path2", "file2.txt");
        Dotfile dotfile3 = new Dotfile("key2", "content1", "/path1", "file1.txt");

        assertEquals(dotfile1, dotfile2);
        assertNotEquals(dotfile1, dotfile3);
        assertEquals(dotfile1.hashCode(), dotfile2.hashCode());
        assertNotEquals(dotfile1.hashCode(), dotfile3.hashCode());
    }

    @Test
    void toString_shouldContainRelevantInfo() {
        Dotfile dotfile = new Dotfile("mykey", "mycontent", "/mypath", "myfile.txt");
        String toString = dotfile.toString();
        assertTrue(toString.contains("mykey"));
        assertTrue(toString.contains("mycontent"));
        assertTrue(toString.contains("myfile.txt"));
    }

    @Test
    void setFilename_shouldThrowExceptionIfFilenameIsBlank() {
        Dotfile dotfile = new Dotfile("key", "content", "/path", "file.txt");
        assertThrows(BlankValueException.class, () -> dotfile.setFilename(""));
        assertThrows(BlankValueException.class, () -> dotfile.setFilename("  "));
        assertEquals("file.txt", dotfile.getFilename());
    }
}

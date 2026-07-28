package io.github.andruid929.dotfiles;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.github.andruid929.dotfiles.errors.BlankValueException;
import io.github.andruid929.leutils.wora.PathFinder;

class DotfileTest {

    @Test
    void constructor_shouldSetFields() {
        Dotfile dotfile = new Dotfile("mykey", "content", "/path/to", "file.txt");
        assertEquals("mykey", dotfile.getKey());
        assertEquals("content", dotfile.getFileContents());
        assertEquals("file.txt", dotfile.getFilename());
        assertEquals("/path/to", dotfile.getFilePath());
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
    void setFilePath_shouldUpdatePath() {
        Dotfile dotfile = new Dotfile("key", "content", "/old/path", "file.txt");
        dotfile.setFilePath("/new/path");
        assertEquals("/new/path", dotfile.getFilePath());
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
        Dotfile dotfile = new Dotfile("key", "content", "/path", "oldfile.txt");
        dotfile.setFilename("newfile.txt");
        assertEquals("newfile.txt", dotfile.getFilename());
        assertEquals("/path", dotfile.getFilePath());
    }

    @Test
    void setKey_shouldUpdateKey() {
        Dotfile dotfile = new Dotfile("key1", "content", "/path", "file.txt");
        dotfile.setKey("key2");
        assertEquals("key2", dotfile.getKey());
    }

    @Test
    void setFileContents_shouldUpdateContent() {
        Dotfile dotfile = new Dotfile("key", "content1", "/path", "file.txt");
        dotfile.setFileContents("content2");
        assertEquals("content2", dotfile.getFileContents());
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
        assertThrows(IllegalArgumentException.class, () -> dotfile.setFilename(""));
        assertThrows(IllegalArgumentException.class, () -> dotfile.setFilename("  "));
    }
}

package io.druidlabs.dotfiles.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import io.github.andruid929.leutils.wora.PathFinder;

class PathUtilTest {

    @Test
    void getPathAndFile_shouldSplitPathCorrectly() {
        Path path = Path.of("home", "andrew", ".ssh", "id_rsa");

        String[] pathAndFile = PathUtil.getPathAndFile(path);

        assertEquals(2, pathAndFile.length);

        assertEquals(Path.of("home", "andrew", ".ssh").toString(), pathAndFile[0]);
        assertEquals(Path.of("id_rsa").toString(), pathAndFile[1]);

    }

    @Test
    void shortenPath_shouldUseTildeForHomeDir() {
        String path = Path.of("name/is/andruid929").toString();

        String absolutePath = Path.of(PathFinder.USER_HOME, "name/is/andruid929").toString();

        assertEquals("name/is/andruid929", PathUtil.shortenPath(path));

        assertEquals("~/name/is/andruid929", PathUtil.shortenPath(absolutePath));
    }

    @Test
    void shortenPath_shouldUseForwardSlashOutsideHomeDir() {
        assertEquals("C:/temp/configs", PathUtil.shortenPath("C:\\temp\\configs"));
        assertEquals("/etc/configs", PathUtil.shortenPath("/etc/configs"));
    }

    @Test
    void shortenPath_shouldNotShortenFolderSharingHomePrefix() {
        String path = Path.of(PathFinder.USER_HOME + "x", "configs").toString();

        assertEquals(path.replace("\\", "/"), PathUtil.shortenPath(path));
    }

    @Test
    void shortenPath_withBlank_shouldReturnTilde() {
        assertEquals("~", PathUtil.shortenPath(""));
        assertEquals("~", PathUtil.shortenPath("  "));
    }

    @Test
    void expandPath_shouldResolveTildeToHomeDir() {
        String path = "~/name/is/andruid929";

        String fullPath = Path.of(PathFinder.USER_HOME, "name", "is", "andruid929").toString();

        String expandedPath = PathUtil.expandPath(path);

        assertEquals(fullPath, expandedPath);
    }

    @Test
    void expandPath_shouldResolveBackslashTildeToHomeDir() {
        String fullPath = Path.of(PathFinder.USER_HOME, "name", "is", "andruid929").toString();

        assertEquals(fullPath, PathUtil.expandPath("~\\name\\is\\andruid929"));
    }

    @Test
    void expandPath_withBlankOrTildeAlone_shouldReturnHomeDir() {
        assertEquals(PathFinder.USER_HOME, PathUtil.expandPath(""));
        assertEquals(PathFinder.USER_HOME, PathUtil.expandPath("~"));
    }

    @Test
    void expandPath_withoutTilde_shouldUseOsSeparator() {
        String expected = String.join(File.separator, "name", "is", "andruid929");

        assertEquals(expected, PathUtil.expandPath("name/is\\andruid929"));
    }
}

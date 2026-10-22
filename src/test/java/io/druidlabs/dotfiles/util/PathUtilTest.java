package io.druidlabs.dotfiles.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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

        assertEquals(path, PathUtil.shortenPath(path));

        assertEquals("~/name/is/andruid929", PathUtil.shortenPath(absolutePath));
    }

    @Test
    void expandPath_shouldResolveTildeToHomeDir() {
        String path = "~/name/is/andruid929";

        String fullPath = Path.of(PathFinder.USER_HOME, "name", "is", "andruid929").toString();

        String expandedPath = PathUtil.expandPath(path);

        assertEquals(fullPath, expandedPath);
    }
}

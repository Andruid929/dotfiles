package io.druidlabs.dotfiles.constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;

import io.github.andruid929.leutils.wora.PathFinder;

/**
 * The sync folder holds the user's {@code credentials.json} and stored login, so if an update moves it, the update
 * looks like a logout and the user has to set sync up again. These pin where earlier builds kept it, which they
 * worked out themselves before {@code PathFinder.getLocalAppDataFolder()} existed.
 */
class FilePathsTest {

    @Test
    @EnabledOnOs(OS.LINUX)
    void syncSecureDir_onLinux_shouldStayWhereEarlierBuildsKeptLogins() {
        assertEquals(Path.of(PathFinder.USER_HOME, ".config", "dotfiles"), FilePaths.SYNC_SECURE_DIR);
    }

    /**
     * Earlier builds used {@code AppData\Local} in the home folder, which is where {@code LOCALAPPDATA} points unless
     * it was redirected. On a machine where it was, this fails, and logins from those builds won't be found.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void syncSecureDir_onWindows_shouldStayWhereEarlierBuildsKeptLogins() {
        assertEquals(Path.of(PathFinder.USER_HOME, "AppData", "Local", "dotfiles"), FilePaths.SYNC_SECURE_DIR);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void syncSecureDir_onMac_shouldStayWhereEarlierBuildsKeptLogins() {
        assertEquals(Path.of(PathFinder.USER_HOME, "Library", "Application Support", "dotfiles"),
                FilePaths.SYNC_SECURE_DIR);
    }

    /**
     * {@code purge} deletes the app folder, which must never take the user's login and credentials with it.
     */
    @Test
    void syncSecureDir_shouldBeOutsideAppFolder() {
        assertFalse(FilePaths.SYNC_SECURE_DIR.startsWith(FilePaths.APP_FOLDER_DIR));
        assertFalse(FilePaths.APP_FOLDER_DIR.startsWith(FilePaths.SYNC_SECURE_DIR));
    }
}

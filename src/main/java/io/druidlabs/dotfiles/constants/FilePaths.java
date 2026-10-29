package io.druidlabs.dotfiles.constants;

import static io.druidlabs.dotfiles.constants.Names.*;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import io.github.andruid929.leutils.wora.PathFinder;

/**
 * Defines default file names and paths used by the application for storage and logging.
 */
public final class FilePaths {

    public static final Path APP_FOLDER_DIR = PathFinder.getDocumentsFolder().resolve(APP_FOLDER_NAME);

    public static final Path SYNC_SECURE_DIR = syncDirPath();

    public static final Path SAVE_FILE = APP_FOLDER_DIR.resolve(SAVE_FILENAME);

    public static final Path LOG_FILE = APP_FOLDER_DIR.resolve(LOG_FILENAME);

    public static final Path CONFIG_FILE = APP_FOLDER_DIR.resolve(CONFIG_FILENAME);

    private FilePaths() {
    }

    @NotNull
    private static Path syncDirPath() {
        String osName = System.getProperty("os.name");

        if (osName.toLowerCase().contains("win")) {
            Path localAppDataDir = PathFinder.createPathFromHomeRoot("AppData", "Local");

            return localAppDataDir.resolve(APP_FOLDER_NAME);
        }

        return PathFinder.getAppDataFolder().resolve(APP_FOLDER_NAME);
    }

}

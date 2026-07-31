package io.github.andruid929.dotfiles.defaults;

import java.nio.file.Path;

import io.github.andruid929.leutils.wora.PathFinder;

/**
 * Defines default file names and paths used by the application for storage and logging.
 */
public class FilePaths {

    public static final String APP_FOLDER_NAME = "dotfiles";

    public static final String SAVE_FILENAME = ".files";

    public static final String LOG_FILENAME = "events.log";

    public static final Path APP_FOLDER_DIR = PathFinder.getDocumentsFolder().resolve(APP_FOLDER_NAME);

    public static final Path SAVE_FILE = APP_FOLDER_DIR.resolve(SAVE_FILENAME);

    public static final Path LOG_FILE = APP_FOLDER_DIR.resolve(LOG_FILENAME);

    private FilePaths() {
    }

}

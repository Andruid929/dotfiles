package io.druidlabs.dotfiles.constants;

import static io.druidlabs.dotfiles.constants.Names.*;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import io.github.andruid929.leutils.wora.PathFinder;

/**
 * Defines default file names and paths used by the application for storage and logging.
 */
public final class FilePaths {

    /**
     * The app's folder in the user's Documents folder, holding the save, log and config files.
     */
    public static final Path APP_FOLDER_DIR = PathFinder.getDocumentsFolder().resolve(APP_FOLDER_NAME);

    /**
     * The folder for sync credentials, with one subfolder per provider. It is in the local app data folder
     * ({@code LOCALAPPDATA} on Windows, so it never roams, {@code ~/Library/Application Support} on macOS and
     * {@code ~/.config} elsewhere), outside {@link #APP_FOLDER_DIR} so {@code purge} never deletes it.
     * <p>
     * Moving it would make every stored login and {@code credentials.json} look missing, so users would have to set
     * sync up again.
     */
    public static final Path SYNC_SECURE_DIR = syncDirPath();

    /**
     * The JSON file holding every saved dotfile.
     */
    public static final Path SAVE_FILE = APP_FOLDER_DIR.resolve(SAVE_FILENAME);

    /**
     * The log file, written by logback's file appender.
     */
    public static final Path LOG_FILE = APP_FOLDER_DIR.resolve(LOG_FILENAME);

    /**
     * The file holding the user's configs.
     */
    public static final Path CONFIG_FILE = APP_FOLDER_DIR.resolve(CONFIG_FILENAME);

    /**
     * The sync snapshot file, which push and pull write once they go through.
     */
    public static final Path SYNC_SNAPSHOT_FILE = APP_FOLDER_DIR.resolve(SYNC_SNAPSHOT_FILENAME);

    private FilePaths() {
    }

    /**
     * @return where {@link #SYNC_SECURE_DIR} lives on this OS, from {@link PathFinder#getLocalAppDataFolder()}.
     */
    @NotNull
    private static Path syncDirPath() {
        return PathFinder.getLocalAppDataFolder().resolve(APP_FOLDER_NAME);
    }

}

package io.github.andruid929.dotfiles.io;

import static io.github.andruid929.dotfiles.defaults.FilePaths.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Utility class for general input/output operations.
 * Handles creation and deletion of application folders and files.
 */
public final class IOUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class);

    private IOUtil() {
    }

    public static void validateAppPaths() {

        if (Files.notExists(APP_FOLDER_DIR)) {
            createAppFolder();

            createDotFiles();
            createLogFile();
        }

        if (Files.notExists(LOG_FILE)) {
            createLogFile();
        }

        if (Files.notExists(SAVE_FILE)) {
            createDotFiles();
        }
    }

    public static void createAppFolder() {
        try {

            Files.createDirectory(APP_FOLDER_DIR);

        } catch (IOException e) {
            LOGGER.error("Unable to create app folder", e);
        }
    }

    public static void createDotFiles() {
        try {

            Files.createFile(SAVE_FILE);

        } catch (IOException e) {
            LOGGER.error("Unable to create app save file", e);
        }
    }

    public static void createLogFile() {
        try {

            Files.createFile(LOG_FILE);

        } catch (IOException e) {
            LOGGER.error("Unable to create error log file", e);
        }
    }

    public static boolean deleteAppFolder() {
        try {

            return Files.deleteIfExists(APP_FOLDER_DIR);

        } catch (IOException e) {
            LOGGER.error("Unable to delete app folder", e);

            return false;
        }
    }

    public static boolean deleteLogFile() {
        try {

            return Files.deleteIfExists(LOG_FILE);

        } catch (IOException e) {
            LOGGER.error("Unable to delete log file", e);

            return false;
        }
    }

    public static boolean deleteDotfiles() {
        try {

            return Files.deleteIfExists(SAVE_FILE);

        } catch (IOException e) {
            LOGGER.error("Unable to delete save file", e);

            return false;
        }
    }
}

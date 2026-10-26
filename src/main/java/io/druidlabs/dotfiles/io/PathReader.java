package io.druidlabs.dotfiles.io;

import static io.druidlabs.dotfiles.constants.FilePaths.LOG_FILE;
import static io.druidlabs.dotfiles.constants.FilePaths.SAVE_FILE;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.druidlabs.dotfiles.annotations.Blankable;

/**
 * Utility class for reading files from the file system.
 * Provides specific methods for reading the dotfiles save file and logs.
 */
public final class PathReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathReader.class);

    private PathReader() {
    }

    public static @NotNull @Blankable String readFile(Path filePath) {
        try {

            return Files.readString(filePath);
        } catch (IOException e) {

            LOGGER.error("Error reading '{}':", filePath);
            LOGGER.error("Stacktrace", e);

            return "";
        }
    }

    @NotNull
    public static String readDotfiles() {
        return readFile(SAVE_FILE);
    }

    public static String readLogs() {
        return readFile(LOG_FILE);
    }

}

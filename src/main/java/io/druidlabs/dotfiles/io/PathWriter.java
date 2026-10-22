package io.druidlabs.dotfiles.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.druidlabs.dotfiles.defaults.FilePaths;

/**
 * Utility class for writing content to files on the file system.
 * Handles writing to dotfiles save file and general file writing with overwriting capabilities.
 */
public final class PathWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathWriter.class);

    private PathWriter() {
    }

    public static boolean writeToFile(Path filePath, @NotNull String contents) {

        if (Files.notExists(filePath)) {
            LOGGER.error("Unable to write to '{}', file not found", filePath);

            return false;
        }

        try {
            Files.writeString(filePath, contents, UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            return true;

        } catch (IOException e) {
            LOGGER.error("Unable to write to '{}'", filePath);
            LOGGER.error("Here's why...", e);

            return false;
        }
    }

    public static boolean writeToDotfiles(@NotNull String contents) {
        return writeToFile(FilePaths.SAVE_FILE, contents);
    }
}

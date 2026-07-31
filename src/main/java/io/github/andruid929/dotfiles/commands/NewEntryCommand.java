package io.github.andruid929.dotfiles.commands;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.andruid929.dotfiles.Dotfile;
import io.github.andruid929.dotfiles.Worker;
import io.github.andruid929.dotfiles.errors.BlankValueException;
import io.github.andruid929.dotfiles.io.PathReader;
import io.github.andruid929.dotfiles.util.PathUtil;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to save a new configuration file into the dotfiles manager.
 * Reads the content of the specified file and stores it with a unique key.
 */
@Command(name = "save", description = "Save new config")
public final class NewEntryCommand extends CommandAction {

    @Option(names = {"-k", "--key"}, required = true, description = "The key for this file")
    String key;

    @Option(names = {"-f", "--file"}, required = true, description = "Where the file is located")
    String filePath;

    @Override
    public void run() {
        try {

            Path fileToRead = getFilePathToRead();

            if (fileToRead == null) {
                return;
            }

            String content = PathReader.readFile(fileToRead);

            if (content.isBlank()) {
                LOGGER.info("I am unable to read that file");

                return;
            }

            String[] fileAndPath = PathUtil.getPathAndFile(fileToRead);

            String fileLocation = fileAndPath[0];
            String filename = fileAndPath[1];

            Dotfile dotfile = new Dotfile(sanitise(key, false), content, fileLocation, filename);

            Worker.addDotfile(dotfile);

            LOGGER.info("Successfully added new file with key '{}'", key);

        } catch (BlankValueException e) {
            LOGGER.info("You left something blank: {}", e.getMessage());

            LOGGER.error("Error adding dotfile: ", e);
        }
    }

    @Nullable
    private Path getFilePathToRead() {
        if (filePath.isBlank()) {
            LOGGER.info("Use the '-f' flag to direct me to the file I should read");
            LOGGER.error("File path is blank");

            return null;
        }

        String expandedPath = PathUtil.expandPath(sanitise(filePath, true));

        Path pathToFile = Path.of(expandedPath);

        if (Files.exists(pathToFile)) {

            if (Files.isDirectory(pathToFile)) {
                LOGGER.info("I'm called dotFILES not dotFOLDERS: '{}'", pathToFile);
                LOGGER.error("Expected file, got directory: '{}'", pathToFile);

                return null;
            }

            return pathToFile;
        }

        LOGGER.info("I cannot find the file '{}', you sure it exists?", pathToFile);
        LOGGER.error("Unable to find file '{}'", pathToFile);

        return null;

    }
}

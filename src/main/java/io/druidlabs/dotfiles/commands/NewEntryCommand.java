package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.exceptions.BlankValueException;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.PathUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
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
    public Integer call() {
        try {

            Path fileToRead;

            try {
                fileToRead = getFilePathToRead();

            } catch (CommandUsageException e) {
                UserNotification.notifyUserOfError(e.getMessage());

                return ExitCode.USAGE;
            }

            String content;

            try {
                content = Files.readString(fileToRead);

            } catch (IOException e) {
                String message;

                if (e instanceof FileSystemException fse) {
                    String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

                    message = interpolate("Failed to read target file because {}", reason.toLowerCase());
                } else {

                    message = "Failed to read target file";
                }

                UserNotification.notifyUserOfError(message);

                LOGGER.error(message, e);

                return ExitCode.SOFTWARE;
            }

            String[] fileAndPath = PathUtil.getPathAndFile(fileToRead);

            String fileLocation = fileAndPath[0];
            String filename = fileAndPath[1];

            Dotfile dotfile = new Dotfile(sanitise(key, false), content, fileLocation, filename);

            Worker.addDotfile(dotfile);

            LOGGER.info("Successfully added new file with key '{}'", key);

        } catch (BlankValueException e) {
            UserNotification.notifyUserOfError("You left something blank: {}", e.getMessage());

            LOGGER.error("Error adding dotfile: ", e);

            return ExitCode.USAGE;
        }

        return ExitCode.OK;
    }

    private @NotNull Path getFilePathToRead() throws CommandUsageException {
        if (filePath.isBlank()) {
            throw new CommandUsageException("Use the '-f' flag to direct me to the file I should read");
        }

        String expandedPath = PathUtil.expandPath(sanitise(filePath, true));

        Path pathToFile = Path.of(expandedPath);

        if (Files.exists(pathToFile)) {

            if (Files.isDirectory(pathToFile)) {
                String message = interpolate("I'm called dotFILES not dotFOLDERS: '{}' is a folder", pathToFile);

                throw new CommandUsageException(message);
            }

            return pathToFile;
        }

        String message = interpolate("I cannot find the file '{}', you sure it exists?", pathToFile);

        throw new CommandUsageException(message);
    }
}

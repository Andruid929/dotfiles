package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
import picocli.CommandLine.Parameters;

/**
 * Command to update an existing dotfile entry.
 * Can be used to change the key, file path, or refresh the stored content from the disk.
 */
@Command(name = "update", description = "Update a saved file")
public final class UpdateEntryCommand extends CommandAction {

    @Parameters(index = "0", description = "The key to update")
    String targetKey;

    @Option(names = {"-k", "--key"}, description = "The new key")
    String newKey = null;

    @Option(names = {"-p", "--path"}, description = "The new file path")
    String pathString = null;

    @Option(names = "-d", description = "Don't read the saved path whether or not an update happened")
    boolean doNotRead = false;

    @Override
    public Integer call() {

        if (targetKey.isBlank()) {
            UserNotification.notifyUser("Key cannot be blank");

            return ExitCode.USAGE;
        }

        if (isNoOp()) {
            UserNotification.notifyUser("Nothing to do");

            return ExitCode.USAGE;
        }

        String key = sanitiseKey(targetKey);

        Dotfile dotfile = Worker.getDotfile(key);

        if (dotfile == null) {
            UserNotification.notifyUser("Dotfile with key '{}' not found", key);

            return ExitCode.USAGE;
        }

        String k = null;
        String[] pathParts = null;

        if (newKey != null) {
            k = sanitiseKey(newKey);

            if (k.isBlank()) {
                UserNotification.notifyUser("New key cannot be blank");

                return ExitCode.USAGE;
            }

            if (k.equalsIgnoreCase(key)) {
                UserNotification.notifyUser("Key already set to '{}'", k);

                k = null;

            } else if (Worker.getDotfile(k) != null) {
                UserNotification.notifyUser("Dotfile key '{}' already exists", k);

                return ExitCode.USAGE;
            }
        }

        if (pathString != null) {
            try {
                pathParts = PathUtil.getPathAndFile(Path.of(sanitise(pathString)));

            } catch (IllegalArgumentException e) {
                UserNotification.notifyUserOfError(e.getMessage());

                return ExitCode.USAGE;
            }

            if (Path.of(pathParts[0], pathParts[1]).equals(dotfile.getPathToFile())) {
                UserNotification.notifyUser("Path already set to '{}'", dotfile.getPathToFile());

                pathParts = null;
            }
        }

        if (doNotRead && k == null && pathParts == null) {
            UserNotification.notifyUser("Nothing to change");

            return ExitCode.USAGE;
        }

        String contents = null;

        if (!doNotRead) {
            Path fileToRead = pathParts == null ? dotfile.getPathToFile() : Path.of(pathParts[0], pathParts[1]);

            String question = interpolate("Update '{}' and replace its saved contents with '{}'?", key, fileToRead);

            if (confirm(question)) {
                try {
                    contents = readContents(fileToRead);

                } catch (BlankValueException e) {
                    UserNotification.notifyUserOfError("{}, cannot update dotfile", e.getMessage());

                    return ExitCode.USAGE;

                } catch (IOException e) {
                    String message = ExceptionUtil.describeIOFailure(e, "Failed to update contents: {}",
                            "Failed to update contents, IO error occurred");

                    notifyAndLogErrorWithException(message, e);

                    return ExitCode.SOFTWARE;
                }

            } else {
                UserNotification.notifyUser("Update cancelled, nothing was changed");

                return ExitCode.OK;
            }
        }

        //Every change has been checked and the file read by now, so nothing below can leave the dotfile half updated

        if (k != null) {
            dotfile.setKey(k);

            notifyAndLog("Key '{}' updated to '{}'", key, k);
        }

        if (pathParts != null) {
            Path oldPath = dotfile.getPathToFile();

            dotfile.setFilePath(pathParts[0]);
            dotfile.setFilename(pathParts[1]);

            notifyAndLog("Path of '{}' updated from '{}' to '{}'", dotfile.getKey(), oldPath, dotfile.getPathToFile());
        }

        if (contents != null) {
            dotfile.setFileContents(contents);

            notifyAndLog("Updated the saved contents of '{}' from '{}'", dotfile.getKey(), dotfile.getPathToFile());
        }

        return ExitCode.OK;
    }

    /**
     * Reads the file a dotfile's contents will be updated from.
     *
     * @param path the file to read.
     * @return the file's contents, never blank.
     * @throws IOException         if the file could not be read.
     * @throws BlankValueException if the file is blank.
     */
    private @NotNull String readContents(@NotNull Path path) throws IOException, BlankValueException {
        String contents = Files.readString(path);

        if (contents.isBlank()) {
            String message = interpolate("Dotfile at '{}' is empty", path);

            throw new BlankValueException(message);
        }

        return contents;
    }

    private boolean isNoOp() {
        return doNotRead && newKey == null && pathString == null;
    }
}

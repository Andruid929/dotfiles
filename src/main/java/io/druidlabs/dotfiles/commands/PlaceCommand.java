package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import io.github.andruid929.leutils.wora.PathFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Command to restore a dotfile from the internal storage to its original or an overridden location.
 * Includes a confirmation step before overwriting existing files.
 */
@Command(name = "place", description = "Place the file at the location saved")
public final class PlaceCommand extends CommandAction {

    @Parameters(index = "0", description = "The key of the file to place")
    String targetKey = null;

    @Option(names = {"-p", "--path"}, description = "Override the saved path and place in this folder instead")
    String pathOverride = null;

    @Option(names = {"-f"}, description = "Create any missing folders when placing")
    boolean createMissingDir = false;

    @Override
    public Integer call() {
        if (targetKey == null || targetKey.isBlank()) {
            UserNotification.notifyUserOfError("I cannot identify file to place without its key");

            return ExitCode.USAGE;
        }

        String key = sanitiseKey(targetKey);

        var dotfile = Worker.getDotfile(key);

        if (dotfile == null) {
            UserNotification.notifyUserOfError("File with key '{}' not found", key);

            return ExitCode.USAGE;
        }

        Path filePath = validateOverridePath(dotfile.getPathToFile());

        String question;

        if (Files.exists(filePath)) {
            question = interpolate("File '{}' already exists, this operation will overwrite it. Are you sure?", filePath);

        } else {
            Path dirs = filePath.getParent();

            question = interpolate("I am about to create {} in '{}', continue?", filePath.getFileName(), dirs);
        }

        if (!confirm(question)) {
            UserNotification.notifyUser("Place operation cancelled");

            UserNotification.notifyUser("Hint: Run 'dotfiles update {}' to update the contents of this file and then you can safely overwrite",
                    dotfile.getKey());

            return ExitCode.OK;
        }

        try {
            Path dirs = filePath.getParent();

            if (createMissingDir && dirs != null && Files.notExists(dirs)) {
                Files.createDirectories(dirs);
            }

            placeFile(dotfile.getFileContents(), filePath);

        } catch (IOException e) {

            String message = ExceptionUtil.describeIOFailure(e, "Oof tough luck, something bad happened while placing file: {}",
                    "Oof tough luck, something bad happened while placing file");

            UserNotification.notifyUserOfError(message);

            String logMessage = interpolate("Error placing dotfile with key '{}' at '{}'", dotfile.getKey(), filePath);

            LOGGER.error(logMessage, e);

            return ExitCode.SOFTWARE;
        }

        return ExitCode.OK;
    }

    /**
     * Writes {@code contents} to {@code file}, creating it if needed and overwriting it otherwise.
     * The file's folder must already exist.
     *
     * @throws IOException if the file could not be created or written.
     */
    private void placeFile(String contents, Path file) throws IOException {
        try {
            Files.createFile(file);

        } catch (FileAlreadyExistsException _) {
            LOGGER.debug("File '{}' already exists, overwriting", file);
        }

        Files.writeString(file, contents);

        notifyAndLog("Successfully placed '{}' in '{}'", file.getFileName(), file.getParent());
    }

    /**
     * Works out where to place the file. Without {@code -p} it is the saved path. A trailing {@code #} places the
     * saved filename in the given folder, and a bare name with no folder is resolved against the home folder.
     *
     * @param def the saved path.
     * @return the path to place the file at.
     */
    private Path validateOverridePath(Path def) {
        if (pathOverride == null) {
            return def;
        }

        String stringPath = sanitise(pathOverride);

        Path fullPath;

        if (stringPath.endsWith("#")) {
            String pathWithoutHash = stringPath.substring(0, stringPath.length() - 1);

            fullPath = Path.of(pathWithoutHash).resolve(def.getFileName());

        } else {
            fullPath = Path.of(stringPath);
        }

        if (!fullPath.isAbsolute() && fullPath.getParent() == null) {
            return PathFinder.createPathFromHomeRoot(fullPath.toString());
        }

        return fullPath;
    }
}

package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.Worker;
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

        String key = sanitise(targetKey, false);

        AtomicInteger exitCode = new AtomicInteger();

        Worker.updateDotfile(key, dotfile -> {
            String currentKey = dotfile.getKey();
            String currentPath = dotfile.getFilePath();

            if (newKey != null) {
                String k = sanitise(newKey, false);

                if (!k.equalsIgnoreCase(currentKey)) {
                    dotfile.setKey(k);

                    UserNotification.notifyUser("Key '{}' updated to '{}'", currentKey, k);
                }
            }

            if (pathString != null) {
                String newPath = sanitise(pathString, true);

                if (!newPath.equalsIgnoreCase(currentPath)) {
                    setPathAndFilename(newPath, dotfile);
                }
            }

            if (!doNotRead) {
                Scanner scanner = new Scanner(System.in);

                String response;

                do {
                    System.out.print("Any changes in the file will overwrite your saved files, are you sure? (y/n): ");

                    response = scanner.next();
                } while (isInvalidConfirmation(response));

                scanner.close();

                switch (response.toLowerCase()) {
                    case "y", "yes" -> {

                        try {
                            setContents(dotfile);

                        } catch (IllegalArgumentException e) {
                            UserNotification.notifyUserOfError("File '{}' is blank, cannot update dotfile");

                            exitCode.set(ExitCode.USAGE);

                        } catch (IOException e) {
                            String message = ExceptionUtil.describeIOFailure(e, "Failed to update contents: {}",
                                    "Failed to update contents, IO error occurred");

                            UserNotification.notifyUserOfError(message);

                            LOGGER.error(message, e);

                            exitCode.set(ExitCode.SOFTWARE);
                        }

                    }
                    case "n", "no" -> UserNotification.notifyUser("File update cancelled");
                }
            }
        });

        return exitCode.get();
    }

    private void setPathAndFilename(@NotNull String newPath, @NotNull Dotfile dotfile) {
        String[] pathParts = PathUtil.getPathAndFile(Path.of(newPath));

        String filepath = pathParts[0];
        String filename = pathParts[1];

        dotfile.setFilePath(filepath);
        dotfile.setFilename(filename);
    }

    private void setContents(@NotNull Dotfile dotfile) throws IOException, IllegalArgumentException {
        Path path = dotfile.getPathToFile();

        String contents = Files.readString(path);

        if (contents.isBlank()) {
            throw new IllegalArgumentException();
        }

        dotfile.setFileContents(contents);

        LOGGER.info("Content update successful");
    }
}

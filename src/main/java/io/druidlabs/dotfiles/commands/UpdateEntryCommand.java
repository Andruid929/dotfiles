package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Scanner;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.io.PathReader;
import io.druidlabs.dotfiles.util.PathUtil;
import picocli.CommandLine.Command;
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
    public void run() {
        if (targetKey.isBlank()) {
            LOGGER.info("Key cannot be blank");
            LOGGER.error("Cannot update dotfile with blank key");
        }

        String key = sanitise(targetKey, false);

        Worker.updateDotfile(key, dotfile -> {
            String currentKey = dotfile.getKey();
            String currentPath = dotfile.getFilePath();

            if (newKey != null) {
                String k = sanitise(newKey, false);

                if (!k.equalsIgnoreCase(currentKey)) {
                    dotfile.setKey(k);

                    LOGGER.info("Key '{}' updated to '{}'", currentKey, k);
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
                System.out.print("Any changes in the file will overwrite your saved files, are you sure? (y/n): ");

                String response = scanner.next();
                scanner.close();

                switch (response.toLowerCase()) {

                    case "y", "yes" -> {
                        setContents(dotfile);
                    }
                    case "n", "no" -> LOGGER.info("File update cancelled");
                    default -> LOGGER.info("'{}' is not a valid answer", response);
                }
            }
        });
    }

    private void setPathAndFilename(@NotNull String newPath, @NotNull Dotfile dotfile) {
        String[] pathParts = PathUtil.getPathAndFile(Path.of(newPath));

        String filepath = pathParts[0];
        String filename = pathParts[1];

        dotfile.setFilePath(filepath);
        dotfile.setFilename(filename);
    }

    private void setContents(@NotNull Dotfile dotfile) {
        Path path = dotfile.getPathToFile();

        String contents = PathReader.readFile(path);

        if (contents.isBlank()) {
            LOGGER.info("Update cancelled, something isn't right");

            return;
        }

        dotfile.setFileContents(contents);

        LOGGER.info("Content update successful");
    }
}

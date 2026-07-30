package io.github.andruid929.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import io.github.andruid929.dotfiles.Worker;
import io.github.andruid929.dotfiles.io.PathWriter;
import io.github.andruid929.leutils.wora.PathFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "place", description = "Place the file at the location saved")
public final class PlaceCommand extends CommandAction {

    @Parameters(index = "0", description = "The key of the file to place")
    String targetKey = null;

    @Option(names = {"-p", "--path"}, description = "Override the saved path and place in this folder instead")
    String pathOverride = null;

    @Option(names = {"-f"}, description = "Create any missing folders when placing")
    boolean createMissingDir = false;

    @Override
    public void run() {
        if (targetKey == null || targetKey.isBlank()) {
            LOGGER.error("I cannot identify file to place without its key");

            return;
        }

        String key = sanitise(targetKey, false);

        var dotfile = Worker.getDotfile(key);

        if (dotfile == null) {
            LOGGER.info("File with key '{}' not found", key);

            return;
        }

        Path filePath = validateOverridePath(dotfile.getPathToFile());

        try (Scanner scanner = new Scanner(System.in)) {
            String confirmation;

            if (Files.exists(filePath)) {
                confirmation = interpolate("File '{}' already exists, this operation will overwrite it. Are you sure? (y/n): ",
                        filePath);

            } else {
                Path dirs = filePath.getParent();

                confirmation = interpolate("I am about to create {} in '{}', continue? (y/n): ",
                        filePath.getFileName(), dirs);
            }

            System.out.print(confirmation);

            String response = scanner.next();

            switch (response.toLowerCase()) {
                case "y", "yes" -> {
                    Path dirs = filePath.getParent();

                    if (createMissingDir && dirs != null && Files.notExists(dirs)) {
                        Files.createDirectories(dirs);
                    }

                    placeFile(dotfile.getFileContents(), filePath);
                }
                case "n", "no" -> {
                    LOGGER.info("Place operation cancelled");

                    System.out.println(interpolate("Hint: Run 'dotfiles update {}' to update the contents of this file and then you can safely overwrite",
                            dotfile.getKey()));
                }
                default -> LOGGER.info("'{}' is not a valid response", response);
            }

        } catch (IOException e) {
            LOGGER.info("Oof tough luck, something bad happened while placing file");
            LOGGER.debug("Error placing dotfile with key '{}' at '{}'", dotfile.getKey(), filePath);
            LOGGER.error("Stacktrace:", e);
        }
    }

    private void placeFile(String contents, Path file) throws IOException {
        try {
            Files.createFile(file);

        } catch (FileAlreadyExistsException _) {
            LOGGER.debug("File '{}' already exists, overwriting", file);
        }

        if (PathWriter.writeToFile(file, contents)) {
            String outputMessage = interpolate("Successfully placed '{}' in '{}'", file.getFileName(), file.getParent());

            System.out.println(outputMessage);
        }
    }

    private Path validateOverridePath(Path def) {
        if (pathOverride == null) {
            return def;
        }

        String stringPath = sanitise(pathOverride, true);

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

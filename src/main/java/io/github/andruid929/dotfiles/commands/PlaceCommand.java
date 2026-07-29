package io.github.andruid929.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import io.github.andruid929.dotfiles.Worker;
import io.github.andruid929.dotfiles.io.PathWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "place", description = "Place the file at the location saved")
public final class PlaceCommand extends CommandAction {

    @Parameters(index = "0", description = "The key of the file to place")
    String targetKey = null;

    @Option(names = {"-p", "--path"}, description = "Override the saved path and place here instead")
    String pathOverride = null;

    @Override
    public void run() {
        if (targetKey == null || targetKey.isBlank()) {
            LOGGER.error("I cannot identify file to place without its key");
        }

        String key = sanitise(targetKey, false);

        var dotfile = Worker.getDotfile(key);

        if (dotfile == null) {
            LOGGER.info("File with key '{}' not found", key);

            return;
        }

        Path filePath = dotfile.getPathToFile();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean fileExists = Files.exists(filePath);

            String confirmation;

            if (fileExists) {
                confirmation = interpolate("File '{}' already exists, this operation will overwrite it. Are you sure? (y/n): ",
                        filePath);
            } else {
                confirmation = interpolate("I am about to create {} in '{}', continue? (y/n): ",
                        dotfile.getFilename(), dotfile.getFilePath());
            }

            System.out.print(confirmation);

            String response = scanner.next();

            switch (response.toLowerCase()) {
                case "y", "yes" -> {
                    String contents = dotfile.getFileContents();

                    if (PathWriter.writeToFile(filePath, contents)) {
                        String outputMessage = interpolate("Successfully placed '{}'", dotfile.getFilename());

                        System.out.println(outputMessage);
                    }
                }
                case "n", "no" -> LOGGER.info("Place operation cancelled");
                default -> LOGGER.info("'{}' is not a valid response", response);
            }
        }
    }
}

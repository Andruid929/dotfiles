package io.druidlabs.dotfiles.commands;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Scanner;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.io.IOTasks;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to delete application data, such as save files and log files.
 * Provides options to exclude either the log file or the save file from deletion.
 */
@Command(name = "purge", description = "Delete this program's save and log files or exclude either")
public final class PurgeCommand extends CommandAction {

    @Option(names = {"-l", "--log"}, description = "Don't delete the logs file")
    boolean excludeLog = false;

    @Option(names = {"-s", "--save"}, description = "Don't delete the save file")
    boolean excludeSave = false;

    @Override
    public void run() {
        if (excludeSave && excludeLog) {
            tellUser("I have nothing to purge, bye");

            return;
        }

        tellUser("I will purge:");

        if (!excludeLog) {
            tellUser("Log file");
        }

        if (!excludeSave) {
            tellUser("Save file");
        }

        tellUser("");

        System.out.print("Are you sure? (y/n) -> ");

        try (Scanner scanner = new Scanner(System.in)) {
            String response = scanner.next().toLowerCase();

            System.out.println();

            switch (response) {
                case "y", "yes" -> {

                    if (!excludeSave) {
                        try {
                            IOTasks.deleteFile(FilePaths.SAVE_FILE);

                            tellUser("Successfully deleted save file '{}'", FilePaths.SAVE_FILE);

                        } catch (IOException ex) {
                            if (ex instanceof NoSuchFileException e) {

                                tellUserOfErrors("Cannot delete save file '{}' because it doesn't exist", e.getFile());
                            } else {

                                tellUserOfErrors("Unable to delete save file");
                            }
                        }
                    }

                    if (!excludeLog) {
                        try {
                            IOTasks.deleteFile(FilePaths.LOG_FILE);

                            tellUser("Successfully deleted log file '{}'", FilePaths.LOG_FILE);

                        } catch (IOException ex) {
                            if (ex instanceof NoSuchFileException e) {

                                tellUserOfErrors("Cannot delete log file '{}' because it doesn't exist", e.getFile());
                            } else {

                                tellUserOfErrors("Unable to delete log file");
                            }
                        }
                    }

                }
                case "n", "no" -> LOGGER.info("Purge cancelled");

                default -> LOGGER.info("I wanted a yes or no but anyways");
            }
        }
    }

}

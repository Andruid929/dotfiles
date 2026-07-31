package io.github.andruid929.dotfiles.commands;

import java.util.Scanner;

import io.github.andruid929.dotfiles.io.IOUtil;
import io.github.andruid929.leutils.wora.PathFinder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to delete application data, such as save files and log files.
 * Provides options to exclude either the log file or the save file from deletion.
 */
@Command(name = "purge", description = "Delete this program's save and log files or exclude either")
public final class PurgeCommand extends CommandAction {

    @Option(names = {"-l", "--log"}, description = "Don't delete the logs file")
    private boolean excludeLog = false;

    @Option(names = {"-s", "--save"}, description = "Don't delete the save file")
    private boolean excludeSave = false;

    @Override
    public void run() {
        if (excludeSave && excludeLog) {
            LOGGER.info("I have nothing to purge, bye");

            return;
        }

        LOGGER.info("I will purge:");

        if (!excludeLog) {
            LOGGER.info("Log file");
        }

        if (!excludeSave) {
            LOGGER.info("Save file");
        }

        System.out.println();

        System.out.print("Are you sure? (y/n) -> ");

        try (Scanner scanner = new Scanner(System.in)) {
            String response = scanner.next().toLowerCase();

            System.out.println();

            switch (response) {
                case "y", "yes" -> {

                    PathFinder.createPathFromHomeRoot("/");

                    if (!excludeSave) {

                        if (IOUtil.deleteDotfiles()) {
                            LOGGER.info("Purged saved files");

                        } else {
                            LOGGER.info("So uhm, I couldn't purge the save file");
                        }
                    }

                    if (!excludeLog) {
                        if (IOUtil.deleteLogFile()) {
                            LOGGER.info("Successfully purged logs file");

                        } else {
                            LOGGER.info("So uhm, I couldn't purge the log file");
                        }
                    }

                }
                case "n", "no" -> LOGGER.info("Purge cancelled");

                default -> LOGGER.info("I wanted a yes or no but anyways");
            }
        }
    }

}

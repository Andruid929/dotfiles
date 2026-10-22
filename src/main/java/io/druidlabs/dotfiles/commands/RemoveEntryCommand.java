package io.druidlabs.dotfiles.commands;

import java.util.Scanner;

import io.druidlabs.dotfiles.Worker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Command to remove a dotfile entry from the manager's internal storage.
 * Does NOT delete the physical file from the disk.
 */
@Command(name = "remove", description = "Remove a saved file")
public final class RemoveEntryCommand extends CommandAction {

    @Parameters(index = "0", defaultValue = "", description = "The key of the dotfile to remove")
    String key;

    @Override
    public void run() {
        if (key.isBlank()) {
            LOGGER.info("It's hard to remove an element whose key I don't know");
            LOGGER.error("Dotfile key is blank, cannot remove dotfile");

            return;
        }

        String dotfileKey = sanitise(key, false);

        String confirmationMessage = "I hope it goes without saying that there's no Ctrl-Z for this operation";

        System.out.println(confirmationMessage);
        System.out.print("Are you sure? (y/n) -> ");

        try (Scanner scanner = new Scanner(System.in)) {
            String response = scanner.next();

            System.out.println();

            switch (response.toLowerCase()) {

                case "yes", "y", "yeah", "si" -> {
                    boolean removed = Worker.removeDotfile(dotfileKey);

                    if (removed) {
                        LOGGER.info("Successfully removed file with key '{}'", dotfileKey);

                    } else {
                        LOGGER.info("No saved file found with key '{}'", dotfileKey);
                    }
                }

                case "n", "no", "nope", "nah" -> LOGGER.info("Remove operation cancelled");

                default -> LOGGER.info("It's a yes or no question but okay... goodbye");
            }
        }
    }

}

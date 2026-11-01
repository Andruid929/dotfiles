package io.druidlabs.dotfiles.commands;

import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine;
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
    public Integer call() {
        if (key.isBlank()) {
            UserNotification.notifyUser("It's hard to remove an element whose key I don't know");

            return CommandLine.ExitCode.OK;
        }

        String dotfileKey = sanitise(key, false);

        UserNotification.notifyUser("I hope it goes without saying that there's no Ctrl-Z for this operation");

        if (!confirm("Are you sure?")) {
            UserNotification.notifyUser("Remove operation cancelled");

            return CommandLine.ExitCode.OK;
        }

        boolean removed = Worker.removeDotfile(dotfileKey);

        if (removed) {
            UserNotification.notifyUser("Successfully removed file with key '{}'", dotfileKey);

            LOGGER.info("Removed dotfile with key '{}'", dotfileKey);

        } else {
            UserNotification.notifyUser("No saved file found with key '{}'", dotfileKey);
        }

        return CommandLine.ExitCode.OK;
    }

}

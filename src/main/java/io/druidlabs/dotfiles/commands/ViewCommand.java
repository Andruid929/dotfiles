package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

/**
 * Command to view the details and content of a saved dotfile.
 * Displays the key, file path, and the full content of the dotfile.
 */
@Command(name = "view", description = "View details about a saved file")
public final class ViewCommand extends CommandAction {

    @Parameters(index = "0", description = "The key for the file to view")
    String key;

    @Override
    public Integer call() {
        String k = sanitise(key, false);

        var dotfile = Worker.getDotfile(k);

        if (dotfile == null) {
            UserNotification.notifyUser("File with key '{}' not found", k);

            return ExitCode.OK;
        }

        LOGGER.debug("Viewing dotfile with key '{}'", k);

        UserNotification.notifyUser(formatData("Key", "File location"));
        UserNotification.notifyUser(formatData(dotfile.getKey(), dotfile.getPathToFile()));
        UserNotification.notifyUser("--------------");
        UserNotification.notifyUser(dotfile.getFileContents());

        return ExitCode.OK;
    }

    /**
     * Formats one row of the header in fixed-width columns: key, then file location.
     */
    @NotNull
    @Contract(pure = true)
    private String formatData(@NotNull Object arg1, @NotNull Object arg2) {
        return String.format("%-10s %s", arg1, arg2);
    }
}

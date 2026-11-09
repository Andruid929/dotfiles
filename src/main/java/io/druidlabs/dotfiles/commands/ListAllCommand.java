package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

/**
 * Command to list all currently managed dotfiles.
 * Displays a table with index, key, and the original file path for each entry.
 */
@Command(name = "list", description = "List all saved files")
public final class ListAllCommand extends CommandAction {

    @Override
    public Integer call() {
        Set<Dotfile> loadedDotfiles = Worker.getLoadedDotfilesSet();

        if (loadedDotfiles.isEmpty()) {
            UserNotification.notifyUser("Nothing to see here... yet");

            return ExitCode.OK;
        }

        int count = 0;

        UserNotification.notifyUser(formatOutput("Num", "Key", "Filepath"));

        UserNotification.notifyUser(formatOutput("---", "---", "--------"));

        for (Dotfile dotfile : loadedDotfiles) {
            String pathToFile = dotfile.getPathToFile().toString();

            String output = formatOutput(String.valueOf(++count), dotfile.getKey(), pathToFile);

            UserNotification.notifyUser(output);
            UserNotification.notifyUser("");
        }

        return ExitCode.OK;
    }

    /**
     * Formats one row of the list in fixed-width columns: number, key, then file path.
     */
    @NotNull
    @Contract(pure = true)
    private String formatOutput(String arg1, String arg2, String arg3) {
        return String.format("%-5s %-12s %s", arg1, arg2, arg3);
    }
}

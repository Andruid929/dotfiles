package io.github.andruid929.dotfiles.commands;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.github.andruid929.dotfiles.Worker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "view", description = "View details about a saved file")
public class ViewCommand extends CommandAction {

    @Parameters(index = "0", description = "The key for the file to view")
    String key = null;

    @Override
    public void run() {
        if (key == null) {
            LOGGER.info("Please specify a key to view");

            return;
        }

        String k = sanitise(key, false);

        var dotfile = Worker.getDotfile(k);

        if (dotfile == null) {
            LOGGER.info("File with key '{}' not found", k);

            return;
        }

        LOGGER.debug("Viewing dotfile with key '{}'", k);

        System.out.println(formatData("Key", "File location"));
        System.out.println(formatData(dotfile.getKey(), dotfile.getPathToFile()));
        System.out.println("--------------");
        System.out.println(dotfile.getFileContents());
    }

    @NotNull
    @Contract(pure = true)
    private String formatData(@NotNull Object arg1, @NotNull Object arg2) {
        return String.format("%-10s %s", arg1, arg2);
    }
}

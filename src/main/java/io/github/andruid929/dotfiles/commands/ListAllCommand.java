package io.github.andruid929.dotfiles.commands;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import io.github.andruid929.dotfiles.Dotfile;
import io.github.andruid929.dotfiles.Worker;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List all saved files")
public class ListAllCommand extends CommandAction {

    @Override
    public void run() {
        Set<Dotfile> loadedDotfiles = Worker.getLoadedDotfilesSet();

        if (loadedDotfiles.isEmpty()) {
            System.out.println("Nothing to see here... yet");

            return;
        }

        int count = 0;

        System.out.println(formatOutput("Num", "Key", "Filepath"));

        System.out.println(formatOutput("---", "---", "--------"));

        for (Dotfile dotfile : loadedDotfiles) {
            String pathToFile = dotfile.getPathToFile().toString();

            String output = formatOutput(String.valueOf(++count), dotfile.getKey(), pathToFile);

            System.out.println(output);
            System.out.println();
        }

    }

    @NotNull
    @Contract(pure = true)
    private String formatOutput(String arg1, String arg2, String arg3) {
        return String.format("%-5s %-12s %s", arg1, arg2, arg3);
    }
}

package io.github.andruid929.dotfiles;

import io.github.andruid929.dotfiles.commands.*;
import io.github.andruid929.dotfiles.defaults.FilePaths;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "dotfiles",
        description = "Save and restore config files",
        version = "Dotfiles 1.0",
        subcommands = {
                NewEntryCommand.class,
                PurgeCommand.class,
                ListAllCommand.class,
                RemoveEntryCommand.class,
                UpdateEntryCommand.class,
                ViewCommand.class
        },
        mixinStandardHelpOptions = true
)
public class Main implements Runnable {

    static {
        final var LOG_FILE_PATH_STRING = FilePaths.LOG_FILE.toString();

        final var LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
        final var CONSOLE_LOG_PATTERN = "%msg%n";

        System.setProperty("LOG_FILE_PATH", LOG_FILE_PATH_STRING);
        System.setProperty("LOG_PATTERN", LOG_PATTERN);
        System.setProperty("CONSOLE_LOG_PATTERN", CONSOLE_LOG_PATTERN);
    }

    public static void main(String[] args) {
        Worker.start();

        int exitCode = new CommandLine(new Main()).execute(args);

        Worker.dismiss();

        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Dotfiles: Save your configs");
        System.out.println();

        main(new String[]{"-h"});
    }
}

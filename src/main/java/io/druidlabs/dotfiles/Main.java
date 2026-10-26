package io.druidlabs.dotfiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.druidlabs.dotfiles.commands.*;
import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The main entry point for the Dotfiles application.
 * This class handles command-line argument parsing using picocli and initializes the application worker.
 */
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
                ViewCommand.class,
                PlaceCommand.class,
                LogCommand.class
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

    /**
     * Entry point of the application.
     * Initializes the {@link Worker}, executes the command, and ensures a graceful shutdown.
     *
     * @param args command-line arguments.
     */
    public static void main(String[] args) {
        int exitCode;

        try {
            Worker.start();

            exitCode = new CommandLine(new Main()).execute(args);

            Worker.dismiss();

            System.exit(exitCode);

        } catch (Exception e) {
            exitCode = CommandLine.ExitCode.SOFTWARE;

            UserNotification.notifyUserOfError("Unexpected error occurred");

            Logger unhandledExceptionLogger = LoggerFactory.getLogger("UnhandledExceptionReporter");

            unhandledExceptionLogger.error("Unhandled exception thrown", e);
        }

        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Missing action, run with -h to see help");
    }
}

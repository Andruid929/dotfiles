package io.druidlabs.dotfiles;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.druidlabs.dotfiles.commands.ConfigCommand;
import io.druidlabs.dotfiles.commands.ListAllCommand;
import io.druidlabs.dotfiles.commands.LogCommand;
import io.druidlabs.dotfiles.commands.NewEntryCommand;
import io.druidlabs.dotfiles.commands.PlaceCommand;
import io.druidlabs.dotfiles.commands.PurgeCommand;
import io.druidlabs.dotfiles.commands.RemoveEntryCommand;
import io.druidlabs.dotfiles.commands.UpdateEntryCommand;
import io.druidlabs.dotfiles.commands.ViewCommand;
import io.druidlabs.dotfiles.commands.sync.LoginCommand;
import io.druidlabs.dotfiles.commands.sync.LogoutCommand;
import io.druidlabs.dotfiles.commands.sync.StatusCommand;
import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.util.ManifestVersionProvider;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Option;

/**
 * The main entry point for the Dotfiles application.
 * This class handles command-line argument parsing using picocli and initializes the application worker.
 */
@Command(
        name = "dotfiles",
        description = "Save and restore config files",
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                NewEntryCommand.class,
                PurgeCommand.class,
                ListAllCommand.class,
                RemoveEntryCommand.class,
                UpdateEntryCommand.class,
                ViewCommand.class,
                PlaceCommand.class,
                LogCommand.class,
                ConfigCommand.class,
                LoginCommand.class,
                LogoutCommand.class,
                StatusCommand.class
        }
)
public class Main implements Runnable {

    // Must run before any logger is created, since logback.xml reads these properties when it starts
    static {
        final var LOG_FILE_PATH_STRING = FilePaths.LOG_FILE.toString();

        final var LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
        final var CONSOLE_LOG_PATTERN = "%msg%n";

        System.setProperty("LOG_FILE_PATH", LOG_FILE_PATH_STRING);
        System.setProperty("LOG_PATTERN", LOG_PATTERN);
        System.setProperty("CONSOLE_LOG_PATTERN", CONSOLE_LOG_PATTERN);
    }

    @Option(names = {"-V", "-v", "--version"}, description = "Show Dotfiles app version", versionHelp = true)
    boolean version = false;

    @Option(names = {"-h", "--help"}, description = "Show this help message and exit", usageHelp = true)
    boolean help = false;

    /**
     * Entry point of the application.
     * Initialises the {@link Worker}, executes the command, and ensures a graceful shutdown.
     *
     * @param args command-line arguments.
     */
    static void main(String[] args) {
        Logger unhandledExceptionLogger = LoggerFactory.getLogger("UnhandledExceptionReporter");

        int exitCode;

        try {
            Worker.start();

            IExecutionExceptionHandler exceptionHandler = executionExceptionHandler(unhandledExceptionLogger);

            var commandLine = new CommandLine(new Main())
                    .setExecutionExceptionHandler(exceptionHandler);

            exitCode = commandLine.execute(args);

            Worker.dismiss();

        } catch (Exception e) {
            exitCode = ExitCode.SOFTWARE;

            UserNotification.notifyFailure("Unexpected error occurred");

            unhandledExceptionLogger.error("Unhandled exception thrown", e);
        }

        System.exit(exitCode);
    }

    /**
     * Builds the handler for exceptions a command doesn't catch itself. picocli's {@code execute()} catches those (and
     * anything a handler rethrows), so they never reach {@code main}'s {@code catch}. The handler disables saving,
     * since the command may have left its changes half done, reports the error and logs it.
     *
     * @param unhandledExceptionLogger the logger the exception is written to.
     * @return the handler, which always returns {@link ExitCode#SOFTWARE}.
     */
    private static @NotNull IExecutionExceptionHandler executionExceptionHandler(Logger unhandledExceptionLogger) {
        return (e, _, _) -> {
            Worker.disableSaving();

            UserNotification.notifyFailure("Unexpected error occurred");

            unhandledExceptionLogger.error("Unhandled exception thrown", e);

            return ExitCode.SOFTWARE;
        };
    }

    /**
     * Runs when {@code dotfiles} is called without a subcommand, pointing the user to the help.
     */
    @Override
    public void run() {
        System.out.println("Missing action, run with -h to see help");
    }
}

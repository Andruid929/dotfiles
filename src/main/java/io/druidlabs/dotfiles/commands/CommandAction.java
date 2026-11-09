package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Callable;

import io.druidlabs.dotfiles.util.PathUtil;
import io.druidlabs.dotfiles.util.UserNotification;

/**
 * Base class for all dotfiles commands.
 * Provides common utility methods like input sanitisation and logging.
 */
public abstract class CommandAction implements Callable<Integer> {

    // Shared and never closed, since closing it would also close System.in for any later prompt
    private static final Scanner INPUT = new Scanner(System.in);

    /**
     * Logger named after the concrete command class. It writes to the log file only, so use the
     * {@code notifyAndLog} helpers or {@link UserNotification} for anything the user should see.
     */
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Sanitizes a string value.
     * If {@code isPath} is true, it expands the path (e.g., handles ~) and normalizes it.
     *
     * @param value  the string to sanitise.
     * @param isPath whether the value represents a file path.
     * @return the sanitised string.
     */
    @NotNull
    protected final String sanitise(@NotNull String value, boolean isPath) {
        if (isPath) {
            String expandedPath = PathUtil.expandPath(value.trim());

            Path normalised = Path.of(expandedPath).normalize();

            return normalised.toString();
        }

        return PathUtil.expandPath(value.trim());
    }

    /**
     * Asks the user a yes/no question, asking again until they answer y, yes, n or no in any case.
     * If input ends before a valid answer, such as Ctrl-D or piped input running out, the answer is taken as no.
     *
     * @param question the question to ask, without the {@code (y/n)} suffix.
     * @return {@code true} if the user answered yes.
     */
    protected final boolean confirm(@NotNull String question) {
        while (true) {
            System.out.print(question + " (y/n): ");

            if (!INPUT.hasNextLine()) {
                System.out.println();

                return false;
            }

            switch (INPUT.nextLine().trim().toLowerCase()) {
                case "y", "yes" -> {
                    return true;
                }

                case "n", "no" -> {
                    return false;
                }

                default -> {
                    // Ask again
                }
            }
        }
    }

    /**
     * Shows a message to the user and writes it to the log file at info level.
     *
     * @param message the message, with {@code {}} placeholders for {@code args}.
     * @param args    the values for the placeholders.
     */
    protected final void notifyAndLog(String message, Object... args) {
        UserNotification.notifyUser(message, args);
        LOGGER.info(message, args);
    }

    /**
     * Shows an error to the user on stderr and writes it to the log file at error level.
     *
     * @param errorMessage the message, with {@code {}} placeholders for {@code args}.
     * @param args         the values for the placeholders.
     */
    protected final void notifyAndLogError(String errorMessage, Object... args) {
        UserNotification.notifyUserOfError(errorMessage, args);
        LOGGER.error(errorMessage, args);
    }

    /**
     * Shows an error to the user on stderr and writes it to the log file with the exception's stack trace,
     * which the user never sees.
     *
     * @param errorMessage the message, with {@code {}} placeholders for {@code args}.
     * @param e            the exception to log.
     * @param args         the values for the placeholders.
     */
    protected final void notifyAndLogErrorWithException(String errorMessage, Exception e, Object... args) {
        UserNotification.notifyUserOfError(errorMessage, args);

        String logMessage = interpolate(errorMessage, args);

        LOGGER.error(logMessage, e);
    }

}

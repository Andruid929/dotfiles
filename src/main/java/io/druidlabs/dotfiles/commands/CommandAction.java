package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Callable;

import io.druidlabs.dotfiles.util.PathUtil;

/**
 * Base class for all dotfiles commands.
 * Provides common utility methods like input sanitisation and logging.
 */
public abstract class CommandAction implements Callable<Integer> {

    // Shared and never closed, since closing it would also close System.in for any later prompt
    private static final Scanner INPUT = new Scanner(System.in);

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
}

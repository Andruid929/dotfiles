package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import io.druidlabs.dotfiles.util.PathUtil;

/**
 * Base class for all dotfiles commands.
 * Provides common utility methods like input sanitisation and logging.
 */
public abstract class CommandAction implements Callable<Integer> {

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

    protected final boolean isInvalidConfirmation(@NotNull String response) {
        return switch (response.toLowerCase().trim()) {
            case "yes", "y", "no", "n" -> false;
            default -> true;
        };
    }
}

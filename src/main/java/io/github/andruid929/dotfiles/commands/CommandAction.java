package io.github.andruid929.dotfiles.commands;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import io.github.andruid929.dotfiles.util.PathUtil;

/**
 * Base class for all dotfiles commands.
 * Provides common utility methods like input sanitization and logging.
 */
public abstract class CommandAction implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Sanitizes a string value.
     * If {@code isPath} is true, it expands the path (e.g., handles ~) and normalizes it.
     *
     * @param value  the string to sanitize.
     * @param isPath whether the value represents a file path.
     * @return the sanitized string.
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

}

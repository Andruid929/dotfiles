package io.github.andruid929.dotfiles.commands;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public abstract class CommandAction implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @NotNull
    protected final String sanitise(@NotNull String value, boolean isPath) {
        if (isPath) {
            Path normalised = Path.of(value.trim()).normalize();

            return normalised.toString();
        }

        return value.trim();
    }

}

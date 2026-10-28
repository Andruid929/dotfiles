package io.druidlabs.dotfiles.commands;

/**
 * Thrown when a command receives input it cannot act on, carrying a message meant to be shown to the user.
 */
public class CommandUsageException extends IllegalArgumentException {

    public CommandUsageException(String messageForUser) {
        super(messageForUser);
    }

}

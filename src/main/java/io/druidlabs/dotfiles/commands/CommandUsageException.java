package io.druidlabs.dotfiles.commands;

/**
 * Thrown when a command receives input it cannot act on, carrying a message meant to be shown to the user.
 */
public class CommandUsageException extends IllegalArgumentException {

    /**
     * @param messageForUser the message to show the user, as is.
     */
    public CommandUsageException(String messageForUser) {
        super(messageForUser);
    }

}

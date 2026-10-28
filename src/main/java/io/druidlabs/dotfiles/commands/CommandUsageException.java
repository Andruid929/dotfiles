package io.druidlabs.dotfiles.commands;

public class CommandUsageException extends IllegalArgumentException {

    public CommandUsageException(String messageForUser) {
        super(messageForUser);
    }

}

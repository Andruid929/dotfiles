package io.druidlabs.dotfiles.errors;

/**
 * Exception thrown when a required value (e.g., key or file content) is blank.
 */
public class BlankValueException extends IllegalArgumentException {
    public BlankValueException(String message) {
        super(message);
    }
}

package io.druidlabs.dotfiles.exceptions;

/**
 * Exception thrown when a required value (e.g., key or file content) is blank.
 */
public class BlankValueException extends IllegalArgumentException {

    /**
     * @param s a message naming the value that was blank.
     */
    public BlankValueException(String s) {
        super(s);
    }

}

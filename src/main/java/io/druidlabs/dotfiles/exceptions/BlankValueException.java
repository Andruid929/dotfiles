package io.druidlabs.dotfiles.exceptions;

public class BlankValueException extends IllegalArgumentException {

    public BlankValueException(String s) {
        super(s);
    }

}

package io.druidlabs.dotfiles.config;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

/**
 * Thrown when a config value is missing, blank or not one the config accepts. The message is shown to the user.
 */
public class InvalidConfigException extends IllegalArgumentException {

    /**
     * @param s the message to show the user.
     */
    public InvalidConfigException(String s) {
        super(s);
    }

    /**
     * @param placeholder the message to show the user, with {@code {}} placeholders for {@code args}.
     * @param args        the values for the placeholders.
     */
    public InvalidConfigException(String placeholder, Object... args) {
        super(interpolate(placeholder,  args));
    }

}

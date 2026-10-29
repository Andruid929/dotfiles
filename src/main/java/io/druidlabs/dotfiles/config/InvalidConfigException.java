package io.druidlabs.dotfiles.config;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

public class InvalidConfigException extends IllegalArgumentException {

    public InvalidConfigException(String s) {
        super(s);
    }

    public InvalidConfigException(String placeholder, Object... args) {
        super(interpolate(placeholder,  args));
    }

}

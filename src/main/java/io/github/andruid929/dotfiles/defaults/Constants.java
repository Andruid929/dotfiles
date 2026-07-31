package io.github.andruid929.dotfiles.defaults;

import java.io.File;
import java.util.regex.Matcher;

public class Constants {

    public static final String FILE_SEPARATOR = Matcher.quoteReplacement(File.separator);

    private Constants() {
        //Utility classes cannot be instantiated
    }
}

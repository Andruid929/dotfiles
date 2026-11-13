package io.druidlabs.dotfiles.constants;

/**
 * Default names used to build the application's folder and file paths.
 */
public final class Names {

    /**
     * The app's display name.
     */
    public static final String APP_NAME = "Dotfiles";

    /**
     * The name of the app's folders, both in Documents and in the app data folder.
     */
    public static final String APP_FOLDER_NAME = "dotfiles";

    /**
     * The name of the JSON save file.
     */
    public static final String SAVE_FILENAME = ".files";

    /**
     * The name of the log file.
     */
    public static final String LOG_FILENAME = "events.log";

    /**
     * The name of the config file.
     */
    public static final String CONFIG_FILENAME = ".config";

    private Names() {
        //Utility classes cannot be instantiated
    }

}

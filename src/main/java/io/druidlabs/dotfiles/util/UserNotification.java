package io.druidlabs.dotfiles.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for sending user-facing messages to the console, routed by the "UserNotification" logger
 * configured in {@code logback.xml} to bypass the file log.
 */
public final class UserNotification {

    private static final Logger logger = LoggerFactory.getLogger("UserNotification");

    private UserNotification() {
        //Utility classes cannot be instantiated
    }

    /**
     * Prints a message on stdout. It is never written to the log file.
     *
     * @param message the message.
     */
    public static void notifyUser(String message) {
        logger.info(message);
    }

    /**
     * Prints a message on stdout. It is never written to the log file.
     *
     * @param placeholder the message, with {@code {}} placeholders for {@code args}.
     * @param args        the values for the placeholders.
     */
    public static void notifyUser(String placeholder, Object... args) {
        logger.info(placeholder, args);
    }

    /**
     * Prints an error on stderr. It is never written to the log file.
     *
     * @param errorMessage the message.
     */
    public static void notifyUserOfError(String errorMessage) {
        logger.error(errorMessage);
    }

    /**
     * Prints an error on stderr. It is never written to the log file.
     *
     * @param placeholder the message, with {@code {}} placeholders for {@code args}.
     * @param args        the values for the placeholders.
     */
    public static void notifyUserOfError(String placeholder, Object... args) {
        logger.error(placeholder, args);
    }

}

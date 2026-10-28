package io.druidlabs.dotfiles.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserNotification {

    private static final Logger logger = LoggerFactory.getLogger("UserNotification");

    private UserNotification() {
        //Utility classes cannot be instantiated
    }

    public static void notifyUser(String message) {
        logger.info(message);
    }

    public static void notifyUser(String placeholder, Object... args) {
        logger.info(placeholder, args);
    }

    public static void notifyUserOfError(String errorMessage) {
        logger.error(errorMessage);
    }

    public static void notifyUserOfError(String placeholder, Object... args) {
        logger.error(placeholder, args);
    }

}

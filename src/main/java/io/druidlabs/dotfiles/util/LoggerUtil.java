package io.druidlabs.dotfiles.util;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Utility class for managing the application's logback appenders.
 */
public final class LoggerUtil {

    private LoggerUtil() {
        //Utility classes cannot be instantiated
    }

    /**
     * Stops and removes the root logger's file appender, closing the log file so it can be deleted. Anything
     * logged afterwards is not written to the file. Does nothing if the appender is already detached.
     */
    public static void detachFileAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        Appender<ILoggingEvent> fileAppender = rootLogger.getAppender("FILE");

        if (fileAppender != null) {
            fileAppender.stop();

            rootLogger.detachAppender(fileAppender);
        }
    }
}

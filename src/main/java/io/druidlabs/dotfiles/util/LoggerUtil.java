package io.druidlabs.dotfiles.util;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public final class LoggerUtil {

    private LoggerUtil() {
        //Utility classes cannot be instantiated
    }

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

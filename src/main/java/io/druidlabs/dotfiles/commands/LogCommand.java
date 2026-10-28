package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

/**
 * Command to locate and optionally read the application log file.
 */
@Command(name = "logs", description = "Locate the log file")
public class LogCommand extends CommandAction {

    @Option(names = {"-r", "--read"}, description = "Display the location and then read it")
    boolean readLogs = false;

    @Override
    public Integer call() {
        UserNotification.notifyUser("You can find the logs at '{}'", FilePaths.LOG_FILE);
        UserNotification.notifyUser("");

        if (readLogs) {
            String logContents;

            try {
                logContents = Files.readString(FilePaths.LOG_FILE);

            } catch (IOException e) {
                String message;

                if (e instanceof FileSystemException fse) {
                    String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

                    message = interpolate("Failed to read log file because {}", reason.toLowerCase());
                } else {
                    message = "Failed to read log file";
                }

                UserNotification.notifyUserOfError(message);

                LOGGER.error(message, e);

                return ExitCode.SOFTWARE;
            }

            if (logContents.isBlank()) {
                System.out.println("Wow, such empty");

                return ExitCode.OK;
            }

            System.out.println(logContents);

            LOGGER.debug("Displayed logs to user");
        }

        return ExitCode.OK;
    }
}

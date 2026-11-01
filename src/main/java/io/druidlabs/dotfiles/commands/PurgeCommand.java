package io.druidlabs.dotfiles.commands;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

/**
 * Command to delete application data, such as save files and log files.
 * Provides options to exclude either the log file or the save file from deletion.
 */
@Command(name = "purge", description = "Delete this program's save and log files or exclude either")
public final class PurgeCommand extends CommandAction {

    @Option(names = {"-l", "--log"}, description = "Don't delete the logs file")
    boolean excludeLog = false;

    @Option(names = {"-s", "--save"}, description = "Don't delete the save file")
    boolean excludeSave = false;

    @Override
    public Integer call() {
        if (excludeSave && excludeLog) {
            UserNotification.notifyUser("I have nothing to purge, bye");

            return ExitCode.OK;
        }

        UserNotification.notifyUser("I will purge:");

        if (!excludeLog) {
            UserNotification.notifyUser("Log file");
        }

        if (!excludeSave) {
            UserNotification.notifyUser("Save file");
        }

        UserNotification.notifyUser("");

        if (!confirm("Are you sure?")) {
            UserNotification.notifyUser("Purge cancelled");

            return ExitCode.OK;
        }

        if (!excludeSave) {

            try {
                IOTasks.deleteFile(FilePaths.SAVE_FILE);

                UserNotification.notifyUser("Successfully deleted save file '{}'", FilePaths.SAVE_FILE);

            } catch (IOException e) {
                if (e instanceof NoSuchFileException nsfe) {

                    UserNotification.notifyUserOfError("Cannot delete save file '{}' because it doesn't exist", nsfe.getFile());
                } else {

                    UserNotification.notifyUserOfError("Unable to delete save file");
                }
            }
        }

        if (!excludeLog) {
            try {
                IOTasks.deleteFile(FilePaths.LOG_FILE);

                UserNotification.notifyUser("Successfully deleted log file '{}'", FilePaths.LOG_FILE);

            } catch (IOException ex) {
                if (ex instanceof NoSuchFileException e) {

                    UserNotification.notifyUserOfError("Cannot delete log file '{}' because it doesn't exist", e.getFile());
                } else {

                    UserNotification.notifyUserOfError("Unable to delete log file '{}'", FilePaths.LOG_FILE);
                }
            }
        }

        return ExitCode.OK;
    }

}

package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.LoggerUtil;
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

    @Option(names = {"-a", "--all"}, description = "Delete everything in the App folder")
    boolean purgeAll = false;

    @Override
    public Integer call() {
        if (purgeAll && (excludeLog || excludeSave)) {
            UserNotification.notifyUserOfError("--all deletes everything, so it can't be combined with --log or --save");

            return ExitCode.USAGE;
        }

        if (excludeSave && excludeLog) {
            UserNotification.notifyUser("I have nothing to purge, bye");

            return ExitCode.OK;
        }

        if (purgeAll) {
            if (!confirm("This will delete all app folders, are you sure?")) {
                UserNotification.notifyUser("Purge cancelled");

                return ExitCode.OK;
            }

            return purgeEverything();
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

                Worker.clearAllDotfiles();

                Worker.disableSaving();

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
                LoggerUtil.detachFileAppender();

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

    private int purgeEverything() {
        Path appFolder = FilePaths.APP_FOLDER_DIR;

        try {
            IOTasks.deleteFolder(appFolder);

            Worker.clearAllDotfiles();

            Worker.disableSaving();

            UserNotification.notifyUser("Successfully deleted app folder");

            return ExitCode.OK;

        } catch (IOException e) {
            if (e instanceof FileSystemException fse) {

                String file = fse.getFile();

                String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

                String message;

                if (file == null) {
                    message = interpolate("Unable to delete one or more files: {}", reason);

                } else {
                    message = interpolate("Unable to delete '{}': {}", file, reason);
                }

                notifyAndLogErrorWithException(message, e);

            } else {
                notifyAndLogErrorWithException("Failed to delete one or more files", e);
            }

            return ExitCode.SOFTWARE;
        }
    }
}

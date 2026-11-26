package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
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

    @Option(names = {"-s", "--save"}, description = "Don't delete the save file, or its sync snapshot and backups")
    boolean excludeSave = false;

    @Option(names = {"-a", "--all"}, description = "Delete everything in the App folder")
    boolean purgeAll = false;

    @Override
    public Integer call() {
        if (purgeAll && (excludeLog || excludeSave)) {
            UserNotification.notifyFailure("--all deletes everything, so it can't be combined with --log or --save");

            return ExitCode.USAGE;
        }

        if (excludeSave && excludeLog) {
            UserNotification.notify("I have nothing to purge, bye");

            return ExitCode.OK;
        }

        if (purgeAll) {
            if (!confirm("This will delete all app folders, are you sure?")) {
                UserNotification.notify("Purge cancelled");

                return ExitCode.OK;
            }

            return purgeEverything();
        }

        UserNotification.notify("I will purge:");

        if (!excludeLog) {
            UserNotification.notify("Log file");
        }

        if (!excludeSave) {
            UserNotification.notify("Save file");

            if (Files.exists(FilePaths.SYNC_SNAPSHOT_FILE)) {
                UserNotification.notify("Sync snapshot");
            }

            if (Files.exists(FilePaths.BACKUPS_DIR)) {
                UserNotification.notify("Sync backups");
            }
        }

        UserNotification.notify("");

        if (!confirm("Are you sure?")) {
            UserNotification.notify("Purge cancelled");

            return ExitCode.OK;
        }

        //A failure stops nothing else: the log is still purged if the save file couldn't be, and the other way round
        boolean failed = false;

        if (!excludeSave) {
            failed = !purgeSaveFile();
        }

        if (!excludeLog) {
            try {
                LoggerUtil.detachFileAppender();

                IOTasks.deleteFile(FilePaths.LOG_FILE);

                UserNotification.notify("Successfully deleted log file '{}'", FilePaths.LOG_FILE);

            } catch (IOException ex) {
                if (ex instanceof NoSuchFileException e) {

                    UserNotification.notifyFailure("Cannot delete log file '{}' because it doesn't exist", e.getFile());
                } else {

                    UserNotification.notifyFailure("Unable to delete log file '{}'", FilePaths.LOG_FILE);
                }

                failed = true;
            }
        }

        return failed ? ExitCode.SOFTWARE : ExitCode.OK;
    }

    /**
     * Deletes the sync snapshot and backups, then the save file. The sync data goes first, and a failure there keeps
     * the save file: an empty save file beside a stale snapshot would make the next push upload nothing over the
     * remote copy, while a save file with no snapshot is only a first sync.
     *
     * @return whether everything was deleted. A failure has already been reported.
     */
    private boolean purgeSaveFile() {
        try {
            deleteSyncData();

        } catch (IOException e) {
            notifyAndLogDeleteFailure(e);

            UserNotification.notifyFailure("The save file was kept, so the next sync can't mistake it for an empty one");

            return false;
        }

        try {
            IOTasks.deleteFile(FilePaths.SAVE_FILE);

            Worker.clearAllDotfiles();

            Worker.disableSaving();

            UserNotification.notify("Successfully deleted save file '{}'", FilePaths.SAVE_FILE);

            deleteLeftoverTempFile(FilePaths.SAVE_FILE);

            notifyOfLeftoverLogin();

            return true;

        } catch (IOException e) {
            if (e instanceof NoSuchFileException nsfe) {

                UserNotification.notifyFailure("Cannot delete save file '{}' because it doesn't exist", nsfe.getFile());
            } else {

                UserNotification.notifyFailure("Unable to delete save file");
            }

            return false;
        }
    }

    /**
     * Deletes the whole {@link FilePaths#APP_FOLDER_DIR} and stops anything being saved on exit.
     * {@link FilePaths#SYNC_SECURE_DIR} is outside it and is never touched.
     *
     * @return {@link ExitCode#SOFTWARE} if anything could not be deleted, otherwise {@link ExitCode#OK}.
     */
    private int purgeEverything() {
        Path appFolder = FilePaths.APP_FOLDER_DIR;

        try {
            IOTasks.deleteFolder(appFolder);

            Worker.clearAllDotfiles();

            Worker.disableSaving();

            UserNotification.notify("Successfully deleted app folder");

            notifyOfLeftoverLogin();

            return ExitCode.OK;

        } catch (IOException e) {
            notifyAndLogDeleteFailure(e);

            return ExitCode.SOFTWARE;
        }
    }

    /**
     * Reports and logs a failed delete, naming the file that couldn't be deleted when the exception says which.
     *
     * @param e why the delete failed.
     */
    private void notifyAndLogDeleteFailure(IOException e) {
        if (e instanceof FileSystemException fse) {

            String file = fse.getFile();

            String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

            String message;

            if (file == null) {
                message = interpolate("Unable to delete one or more files: {}", reason);

            } else {
                message = interpolate("Unable to delete '{}': {}", file, reason);
            }

            notifyAndLogFailureWithException(message, e);

        } else {
            notifyAndLogFailureWithException("Failed to delete one or more files", e);
        }
    }

    /**
     * Points at {@code logout} when a sync folder exists, since {@code purge} never deletes
     * {@link FilePaths#SYNC_SECURE_DIR}. It only checks that the folder exists, so {@code purge} still runs no sync
     * code. The folder may hold only a {@code credentials.json}, hence "credentials" rather than "login".
     */
    private void notifyOfLeftoverLogin() {
        if (Files.exists(FilePaths.SYNC_SECURE_DIR)) {
            UserNotification.notify("Sync credentials in '{}' are kept. To sign out, use 'dotfiles logout'",
                    FilePaths.SYNC_SECURE_DIR);
        }
    }

    /**
     * Deletes the temporary file a crash mid-save may have left beside {@code file}. It holds a copy of the saved
     * dotfiles, so leaving it would keep what the user asked to purge. A failure is only logged, since the file
     * itself is already gone and the next save overwrites the temporary one.
     *
     * @param file the file whose temporary file to delete.
     */
    private void deleteLeftoverTempFile(Path file) {
        Path tempFile = IOTasks.tempFileFor(file);

        try {
            Files.deleteIfExists(tempFile);

        } catch (IOException e) {
            LOGGER.warn("Unable to delete leftover temporary file '{}'", tempFile, e);
        }
    }

    /**
     * Deletes the sync snapshot, a temporary file a crash may have left beside it, and the backups folder. Either may
     * be missing, as before a first sync.
     *
     * @throws IOException if the snapshot or the backups folder exists but could not be deleted.
     */
    private void deleteSyncData() throws IOException {
        try {
            IOTasks.deleteFile(FilePaths.SYNC_SNAPSHOT_FILE);

        } catch (NoSuchFileException _) {
            //Sync snapshot doesn't exist, don't throw
        }

        deleteLeftoverTempFile(FilePaths.SYNC_SNAPSHOT_FILE);

        try {
            IOTasks.deleteFolder(FilePaths.BACKUPS_DIR);

        } catch (NoSuchFileException _) {
            //Backups folder doesn't exist, don't throw
        }
    }
}

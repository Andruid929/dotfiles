package io.druidlabs.dotfiles;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.persistence.JsonPersistence;
import io.druidlabs.dotfiles.persistence.Persistence;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine;

/**
 * The orchestrator for managing dotfiles in memory and coordinating persistence.
 * This class handles loading from and saving to the local storage.
 */
public final class Worker {

    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    private static final Persistence persistence = new JsonPersistence();

    private static final Set<Dotfile> loadedDotFilesSet = new HashSet<>();

    private Worker() {
    }

    public static @Nullable Dotfile getDotfile(@NotNull String key) {
        return loadedDotFilesSet.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public static void addDotfile(Dotfile dotfile) {
        loadedDotFilesSet.add(dotfile);
    }

    public static boolean removeDotfile(@NotNull String key) {
        return loadedDotFilesSet.removeIf(dotfile -> dotfile.getKey().equalsIgnoreCase(key));
    }

    public static void updateDotfile(@NotNull String key, Consumer<Dotfile> operation) {
        loadedDotFilesSet.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .ifPresentOrElse(operation, () -> UserNotification.notifyUser("File with key '{}' not found", key));
    }

    /**
     * Initializes the worker, sets up a shutdown hook for automatic saving,
     * and loads dotfiles from storage.
     */
    public static void start() {
        Set<Dotfile> savedDotfiles = new HashSet<>();

        try {
            IOTasks.validateAppPaths();

            savedDotfiles = persistence.load();

        } catch (IOException ioe) {
            String message;

            if (ioe instanceof FileSystemException fse) {

                String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

                String failedFile = fse.getFile();

                if (failedFile != null) {
                    message = interpolate("Unable to create '{}' because {}. Exiting", failedFile, reason.toLowerCase());

                } else {
                    message = reason;
                }

                UserNotification.notifyUserOfError(message);

                System.exit(CommandLine.ExitCode.SOFTWARE);

            } else if (ioe instanceof JsonProcessingException) {
                message = "Failed to process saved dotfiles";

            } else {
                message = "Error occurred initialising program";
            }

            UserNotification.notifyUserOfError(message);

            logger.error(message, ioe);

            System.exit(CommandLine.ExitCode.SOFTWARE);
        }

        loadedDotFilesSet.addAll(savedDotfiles);
    }

    /**
     * Shuts down the worker and ensures all current dotfiles are saved.
     */
    public static void dismiss() {
        try {
            persistence.save(loadedDotFilesSet);

        } catch (IOException e) {
            String message;

            if (e instanceof JsonProcessingException jpe) {
                message = "Failed to process dotfiles for saving";

            } else {
                message = "Failed to save dotfiles";
            }

            UserNotification.notifyUserOfError(message);

            logger.error(message, e);
        }
    }

    public static Set<Dotfile> getLoadedDotfilesSet() {
        return loadedDotFilesSet;
    }
}

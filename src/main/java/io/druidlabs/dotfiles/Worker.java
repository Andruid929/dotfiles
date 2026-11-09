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

import io.druidlabs.dotfiles.config.Configuration;
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

    /**
     * Where the dotfiles are loaded from by {@link #start()} and saved to by {@link #dismiss()}.
     */
    private static final Persistence persistence = new JsonPersistence();

    /**
     * The dotfiles for this run. {@link Dotfile} equality is on the key alone, so this never holds two entries with
     * the same key.
     */
    private static final Set<Dotfile> loadedDotFilesSet = new HashSet<>();

    /**
     * Whether {@link #dismiss()} saves, turned off by {@link #disableSaving()}.
     */
    private static boolean savingEnabled = true;

    /**
     * The configs for this run, loaded by {@link #start()}.
     */
    private static Configuration configuration;

    private Worker() {
    }

    /**
     * @param key the key to look up, matched case-insensitively.
     * @return the dotfile with that key, or {@code null} if none is saved.
     */
    public static @Nullable Dotfile getDotfile(@NotNull String key) {
        return loadedDotFilesSet.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a dotfile to this run's set. If one with the same key is already saved, the set keeps the existing one
     * and this does nothing.
     *
     * @param dotfile the dotfile to add.
     */
    public static void addDotfile(Dotfile dotfile) {
        loadedDotFilesSet.add(dotfile);
    }

    /**
     * @param key the key of the dotfile to remove, matched case-insensitively.
     * @return {@code true} if a dotfile was removed.
     */
    public static boolean removeDotfile(@NotNull String key) {
        return loadedDotFilesSet.removeIf(dotfile -> dotfile.getKey().equalsIgnoreCase(key));
    }

    /**
     * Runs {@code operation} on the dotfile with {@code key}, or tells the user no such dotfile is saved.
     *
     * @param key       the key of the dotfile to update, matched case-insensitively.
     * @param operation the changes to make to the dotfile.
     */
    public static void updateDotfile(@NotNull String key, Consumer<Dotfile> operation) {
        loadedDotFilesSet.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .ifPresentOrElse(operation, () -> UserNotification.notifyUser("File with key '{}' not found", key));
    }

    /**
     * Initialises the worker, sets up a shutdown hook for automatic saving,
     * and loads dotfiles from storage.
     */
    public static void start() {
        Set<Dotfile> savedDotfiles = new HashSet<>();

        configuration = Configuration.createInstance();

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
     * Shuts down the worker and ensures all current dotfiles and configs are saved, unless saving was disabled.
     */
    public static void dismiss() {
        if (!savingEnabled) {
            return;
        }

        configuration.saveConfigs();

        try {
            persistence.save(loadedDotFilesSet);

        } catch (IOException e) {
            String message;

            if (e instanceof JsonProcessingException) {
                message = "Failed to process dotfiles for saving";

            } else {
                message = "Failed to save dotfiles";
            }

            UserNotification.notifyUserOfError(message);

            logger.error(message, e);
        }
    }

    /**
     * Removes every dotfile from this run's set. The save file is not touched until {@link #dismiss()}.
     */
    public static void clearAllDotfiles() {
        loadedDotFilesSet.clear();
    }

    /**
     * Stops {@link #dismiss()} from saving dotfiles and configs, for when their files have just been deleted.
     */
    public static void disableSaving() {
        savingEnabled = false;
    }

    /**
     * @return {@code false} once {@link #disableSaving()} has been called.
     */
    public static boolean isSavingEnabled() {
        return savingEnabled;
    }

    /**
     * @return the live set of dotfiles for this run. Changes to it are saved by {@link #dismiss()}.
     */
    public static Set<Dotfile> getLoadedDotfilesSet() {
        return loadedDotFilesSet;
    }

    /**
     * @return the configs loaded by {@link #start()}, or {@code null} before it has run.
     */
    public static Configuration getConfiguration() {
        return configuration;
    }
}

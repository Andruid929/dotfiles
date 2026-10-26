package io.druidlabs.dotfiles;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.io.PathReader;
import io.druidlabs.dotfiles.io.PathWriter;
import io.druidlabs.dotfiles.transformer.JsonTransformer;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine;

/**
 * The orchestrator for managing dotfiles in memory and coordinating persistence.
 * This class handles loading from and saving to the local storage.
 */
public final class Worker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

    private static Set<Dotfile> DOTFILES_SET;

    private Worker() {
    }


    public static @Nullable Dotfile getDotfile(@NotNull String key) {
        return DOTFILES_SET.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
    }

    public static void addDotfile(Dotfile dotfile) {
        DOTFILES_SET.add(dotfile);
    }

    public static boolean removeDotfile(@NotNull String key) {
        return DOTFILES_SET.removeIf(dotfile -> dotfile.getKey().equalsIgnoreCase(key));
    }

    public static void updateDotfile(@NotNull String key, Consumer<Dotfile> operation) {
        DOTFILES_SET.stream()
                .filter(dotfile -> dotfile.getKey().equalsIgnoreCase(key))
                .findFirst()
                .ifPresentOrElse(operation, () -> LOGGER.info("File with key '{}' not found", key));
    }

    /**
     * Initializes the worker, sets up a shutdown hook for automatic saving,
     * and loads dotfiles from storage.
     */
    public static void start() {
        try {
            IOTasks.validateAppPaths();

        } catch (IOException ioe) {
            if (ioe instanceof FileSystemException fse) {

                String reason = ExceptionUtil.getFileSystemExceptionReason(fse);

                String failedFile = fse.getFile();

                String message;

                if (failedFile != null) {
                    message = interpolate("Unable to create '{}' because '{}'. Exiting", failedFile, reason);

                } else {
                    message = reason;
                }

                UserNotification.notifyUserOfError(message);

                System.exit(CommandLine.ExitCode.SOFTWARE);
            }

            UserNotification.notifyUserOfError("Error occurred creating program files");

            System.exit(CommandLine.ExitCode.SOFTWARE);
        }

        DOTFILES_SET = ConcurrentHashMap.newKeySet();

        deserialise();
    }

    /**
     * Shuts down the worker and ensures all current dotfiles are saved.
     */
    public static void dismiss() {
        serialise();
    }

    public static Set<Dotfile> getLoadedDotfilesSet() {
        return DOTFILES_SET;
    }

    public static void serialise() {
        String json = JsonTransformer.serialiseToJsonString(DOTFILES_SET);

        if (json.isBlank()) {
            LOGGER.info("Unable to save");
            return;
        }

        PathWriter.writeToDotfiles(json);
    }

    public static void deserialise() {
        String json = PathReader.readDotfiles();

        if (json.isBlank()) {
            return;
        }

        Set<Dotfile> savedDotfiles = JsonTransformer.deserialiseToSet(json);

        if (!savedDotfiles.isEmpty()) {
            DOTFILES_SET.addAll(savedDotfiles);
        }

    }
}

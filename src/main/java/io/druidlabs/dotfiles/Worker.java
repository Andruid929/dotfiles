package io.druidlabs.dotfiles;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.druidlabs.dotfiles.io.IOUtil;
import io.druidlabs.dotfiles.io.PathReader;
import io.druidlabs.dotfiles.io.PathWriter;
import io.druidlabs.dotfiles.transformer.JsonTransformer;

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
        Runtime.getRuntime().addShutdownHook(gracefulShutdownthread());

        IOUtil.validateAppPaths();

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

    private static Thread gracefulShutdownthread() {
        return Thread.ofVirtual()
                .name("graceful-shutdown-worker")
                .uncaughtExceptionHandler((Thread _, Throwable e) -> LOGGER.error("Graceful shutdown not so graceful", e))
                .unstarted(Worker::serialise);
    }
}

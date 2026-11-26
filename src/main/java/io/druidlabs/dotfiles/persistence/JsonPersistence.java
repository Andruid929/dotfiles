package io.druidlabs.dotfiles.persistence;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.transformer.JsonHandler;

/**
 * {@link Persistence} implementation that reads and writes the dotfiles as a JSON array in one save file,
 * {@link FilePaths#SAVE_FILE} unless another is given.
 */
public final class JsonPersistence implements Persistence {

    /**
     * The file the dotfiles are loaded from and saved to.
     */
    private final Path saveFile;

    /**
     * Creates a persistence backed by {@link FilePaths#SAVE_FILE}.
     */
    public JsonPersistence() {
        this(FilePaths.SAVE_FILE);
    }

    /**
     * Creates a persistence backed by the given file, such as one in a test's temporary folder.
     *
     * @param saveFile the file to load from and save to.
     */
    public JsonPersistence(@NotNull Path saveFile) {
        this.saveFile = saveFile;
    }

    @Override
    public @NotNull Set<Dotfile> load() throws IOException {
        String jsonString = Files.readString(saveFile);

        if (jsonString.isBlank() || jsonString.trim().equals("[]")) return new HashSet<>();

        return JsonHandler.deserialiseToSet(jsonString);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Saved through {@link IOTasks#atomicSave(Path, String)}, so a crash or power cut mid-save leaves the previous
     * save or the new one, never part of each.
     */
    @Override
    public void save(Set<Dotfile> setOfDotfiles) throws IOException {
        String jsonString = JsonHandler.serialiseToJsonString(setOfDotfiles);

        IOTasks.atomicSave(saveFile, jsonString);
    }
}

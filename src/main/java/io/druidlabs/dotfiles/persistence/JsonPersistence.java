package io.druidlabs.dotfiles.persistence;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.transformer.JsonHandler;

/**
 * {@link Persistence} implementation that reads and writes the dotfiles as JSON in {@link FilePaths#SAVE_FILE}.
 */
public final class JsonPersistence implements Persistence {

    public JsonPersistence() {
    }

    @Contract(pure = true)
    @Override
    public @NotNull Set<Dotfile> load() throws IOException {
        String jsonString = Files.readString(FilePaths.SAVE_FILE);

        if (jsonString.isBlank() || jsonString.trim().equals("[]")) return new HashSet<>();

        return JsonHandler.deserialiseToSet(jsonString);
    }

    @Override
    public void save(Set<Dotfile> setOfDotfiles) throws IOException {
        String jsonString = JsonHandler.serialiseToJsonString(setOfDotfiles);

        Files.writeString(FilePaths.SAVE_FILE, jsonString);
    }
}

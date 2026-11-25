package io.druidlabs.dotfiles.persistence;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import io.druidlabs.dotfiles.io.IOTasks;
import io.druidlabs.dotfiles.sync.SyncSnapshot;
import io.druidlabs.dotfiles.transformer.JsonHandler;

public final class SyncSnapshotIO {

    public SyncSnapshotIO() {
    }

    public @NotNull Optional<SyncSnapshot> readSnapshot(@NotNull Path path) throws IOException {
        String jsonString;

        try {
            jsonString = Files.readString(path);

        } catch (NoSuchFileException e) {
            return Optional.empty();
        }

        SyncSnapshot snapshot = JsonHandler.getMapper().readValue(jsonString, SyncSnapshot.class);

        return Optional.of(snapshot);
    }

    public void saveSnapshot(@NotNull Path outputPath, @NotNull SyncSnapshot snapshot) throws IOException {
        String jsonString = JsonHandler.getMapper().writeValueAsString(snapshot);

        IOTasks.atomicSave(outputPath, jsonString);
    }
}

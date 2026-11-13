package io.druidlabs.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.github.andruid929.leutils.wora.PathFinder;

class JsonHandlerTest {

    @Test
    void roundTrip_shouldPreserveData() throws JsonProcessingException {
        String path = interpolate("name{}andrew", File.separator);

        var dotfile = new Dotfile("test file", "Hello world",
                path, "file.sql");

        Set<Dotfile> dotfilesSet = Set.of(dotfile);

        String jsonString = JsonHandler.serialiseToJsonString(dotfilesSet);

        Set<Dotfile> deserialisedSet = JsonHandler.deserialiseToSet(jsonString);

        assertEquals(dotfilesSet, deserialisedSet);

        Dotfile dotfileFromDeserialisedSet = deserialisedSet.stream().toList().getFirst();

        assertEquals("Hello world", dotfileFromDeserialisedSet.getFileContents());
        assertEquals(path, dotfileFromDeserialisedSet.getFilePath());
    }

    @Test
    void serialise_shouldStoreShortenedPath() throws JsonProcessingException {
        String path = Path.of(PathFinder.USER_HOME, ".config", "nvim").toString();

        var dotfile = new Dotfile("nvim", "Hello world", path, "init.lua");

        String jsonString = JsonHandler.serialiseToJsonString(Set.of(dotfile));

        JsonNode entry = JsonHandler.getMapper().readTree(jsonString).get(0);

        assertEquals("~/.config/nvim", entry.get("filePath").asText());
        assertEquals("init.lua", entry.get("filename").asText());
    }

    @Test
    void deserialise_shouldNormaliseBackslashTilde() throws JsonProcessingException {
        String jsonString = """
                [{"key": "nvim", "fileContents": "Hello world", "filePath": "~\\\\.config\\\\nvim", "filename": "init.lua"}]
                """;

        Dotfile dotfile = JsonHandler.deserialiseToSet(jsonString).stream().toList().getFirst();

        assertEquals("~/.config/nvim", dotfile.getShortFilePath());
        assertEquals(Path.of(PathFinder.USER_HOME, ".config", "nvim").toString(), dotfile.getFilePath());
    }

}

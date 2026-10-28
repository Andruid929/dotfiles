package io.druidlabs.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

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

}

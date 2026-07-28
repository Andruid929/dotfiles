package io.github.andruid929.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import io.github.andruid929.dotfiles.Dotfile;

class JsonTransformerTest {

    @Test
    void roundTrip_shouldPreserveData() {
        String path = interpolate("name{}andrew", File.separator);

        var dotfile = new Dotfile("test file", "Hello world",
                path, "file.sql");

        Set<Dotfile> dotfilesSet = Set.of(dotfile);

        String jsonString = JsonTransformer.serialiseToJsonString(dotfilesSet);

        Set<Dotfile> deserialisedSet = JsonTransformer.deserialiseToSet(jsonString);

        assertEquals(dotfilesSet, deserialisedSet);

        Dotfile dotfileFromDeserialisedSet = deserialisedSet.stream().toList().getFirst();

        assertEquals("Hello world", dotfileFromDeserialisedSet.getFileContents());
        assertEquals(path, dotfileFromDeserialisedSet.getFilePath());
    }

}

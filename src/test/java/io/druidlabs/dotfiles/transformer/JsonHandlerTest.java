package io.druidlabs.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void roundTrip_shouldKeepAwkwardContentsExactly() throws JsonProcessingException {
        String contents = "# Windows line endings\r\n\tindented\r\n"
                + "PS1='\\[\\e[32m\\]\\u\\$ '\n"
                + "{\"editor.fontSize\": 14, \"path\": \"C:\\\\Users\"}\n"
                + "unicode: café ☕ 日本語\n"
                + "no newline at the end";

        var dotfile = new Dotfile("awkward", contents, "~", ".awkwardrc");

        String jsonString = JsonHandler.serialiseToJsonString(Set.of(dotfile));

        Dotfile reloaded = JsonHandler.deserialiseToSet(jsonString).stream().toList().getFirst();

        assertEquals(contents, reloaded.getFileContents());
    }

    @Test
    void deserialise_ofIndentedFileWrittenOnWindows_shouldLoad() throws JsonProcessingException {
        String jsonString = """
                [ {
                  "key" : "vimrc",
                  "fileContents" : "set number",
                  "filePath" : "~",
                  "filename" : ".vimrc"
                } ]
                """.replace("\n", "\r\n");

        Set<Dotfile> dotfiles = JsonHandler.deserialiseToSet(jsonString);

        assertEquals("set number", dotfiles.stream().toList().getFirst().getFileContents());
    }

    @Test
    void deserialise_withFieldFromNewerVersion_shouldLoad() throws JsonProcessingException {
        String jsonString = """
                [{"key": "vimrc", "fileContents": "set number", "filePath": "~", "filename": ".vimrc", "savedAt": "2027-01-01T10:00:00Z"}]
                """;

        Set<Dotfile> dotfiles = JsonHandler.deserialiseToSet(jsonString);

        assertEquals(1, dotfiles.size());
    }

    /**
     * A power cut while {@code .files} is written can leave it cut off. {@code Worker.start()} only reports
     * {@code IOException}s, so anything else would surface as "Unexpected error occurred".
     */
    @Test
    void deserialise_ofFileCutOffMidWrite_shouldThrowJsonProcessingException() throws JsonProcessingException {
        var dotfile = new Dotfile("vimrc", "set number\nset expandtab", "~", ".vimrc");

        String jsonString = JsonHandler.serialiseToJsonString(Set.of(dotfile));

        String cutOff = jsonString.substring(0, jsonString.length() / 2);

        assertThrows(JsonProcessingException.class, () -> JsonHandler.deserialiseToSet(cutOff));
    }

    @Test
    void deserialise_withContentsEmptiedByHand_shouldThrowJsonProcessingException() {
        String jsonString = """
                [{"key": "vimrc", "fileContents": "", "filePath": "~", "filename": ".vimrc"}]
                """;

        assertThrows(JsonProcessingException.class, () -> JsonHandler.deserialiseToSet(jsonString));
    }

    @Test
    void deserialise_withKeyMissingAfterHandEdit_shouldThrowJsonProcessingException() {
        String jsonString = """
                [{"fileContents": "set number", "filePath": "~", "filename": ".vimrc"}]
                """;

        assertThrows(JsonProcessingException.class, () -> JsonHandler.deserialiseToSet(jsonString));
    }

}

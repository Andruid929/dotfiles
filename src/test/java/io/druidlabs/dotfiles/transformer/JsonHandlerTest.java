package io.druidlabs.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.exceptions.DuplicateKeysException;
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

    /**
     * Builds before 2026-07-28 kept keys as typed, so their {@code .files} could hold {@code Bashrc} and
     * {@code bashrc} as two dotfiles. Keys are now lower-cased, which makes them the same key.
     */
    @Test
    void deserialise_ofEarlyV1FileWithKeysDifferingInCase_shouldNameTheKey() {
        String jsonString = """
                [ {
                  "key" : "Bashrc",
                  "fileContents" : "alias ll='ls -l'",
                  "filePath" : "~",
                  "filename" : ".bashrc"
                }, {
                  "key" : "bashrc",
                  "fileContents" : "export EDITOR=vim",
                  "filePath" : "~/backup",
                  "filename" : ".bashrc"
                } ]
                """;

        var e = assertThrows(DuplicateKeysException.class, () -> JsonHandler.deserialiseToSet(jsonString));

        assertEquals(List.of("bashrc"), e.getDuplicateKeys());
        assertTrue(e.getMessage().contains("bashrc"), e.getMessage());
    }

    @Test
    void deserialise_withEntryCopiedAndKeyLeftUnchanged_shouldThrow() {
        String jsonString = """
                [{"key": "vimrc", "fileContents": "set number", "filePath": "~", "filename": ".vimrc"},
                 {"key": "vimrc", "fileContents": "set number", "filePath": "~/.config/nvim", "filename": "init.vim"}]
                """;

        var e = assertThrows(DuplicateKeysException.class, () -> JsonHandler.deserialiseToSet(jsonString));

        assertEquals(List.of("vimrc"), e.getDuplicateKeys());
    }

    /**
     * Three machines' {@code .files} pasted into one array, each with its own {@code bashrc}.
     */
    @Test
    void deserialise_ofFilesMergedByHand_shouldNameEachDuplicateOnce() {
        String jsonString = """
                [{"key": "bashrc", "fileContents": "laptop", "filePath": "~", "filename": ".bashrc"},
                 {"key": "vimrc", "fileContents": "laptop", "filePath": "~", "filename": ".vimrc"},
                 {"key": "bashrc", "fileContents": "desktop", "filePath": "~", "filename": ".bashrc"},
                 {"key": "vimrc", "fileContents": "desktop", "filePath": "~", "filename": ".vimrc"},
                 {"key": "tmux", "fileContents": "desktop", "filePath": "~", "filename": ".tmux.conf"},
                 {"key": "bashrc", "fileContents": "server", "filePath": "~", "filename": ".bashrc"}]
                """;

        var e = assertThrows(DuplicateKeysException.class, () -> JsonHandler.deserialiseToSet(jsonString));

        assertEquals(2, e.getDuplicateKeys().size(), e.getDuplicateKeys().toString());
        assertEquals(Set.of("bashrc", "vimrc"), Set.copyOf(e.getDuplicateKeys()));
    }

    /**
     * Duplicates are by key, so dotfiles that share a filename in different folders are fine.
     */
    @Test
    void deserialise_withSameFilenameInDifferentFolders_shouldLoad() throws JsonProcessingException {
        String jsonString = """
                [{"key": "nvim", "fileContents": "vim.opt.number = true", "filePath": "~/.config/nvim", "filename": "init.lua"},
                 {"key": "lvim", "fileContents": "lvim.colorscheme = 'tokyonight'", "filePath": "~/.config/lvim", "filename": "init.lua"}]
                """;

        assertEquals(2, JsonHandler.deserialiseToSet(jsonString).size());
    }

    /**
     * A {@code null} left where an entry was removed. It used to load as a {@code null} dotfile and fail later,
     * outside {@code Worker.start()}'s handling.
     */
    @Test
    void deserialise_withNullLeftByHandEdit_shouldThrowJsonProcessingException() {
        String jsonString = """
                [{"key": "vimrc", "fileContents": "set number", "filePath": "~", "filename": ".vimrc"}, null]
                """;

        assertThrows(JsonProcessingException.class, () -> JsonHandler.deserialiseToSet(jsonString));
    }

    /**
     * Two {@code null}s used to throw a {@link NullPointerException} while looking for duplicates, which ends in
     * "Unexpected error occurred" rather than the message for a bad file.
     */
    @Test
    void deserialise_withTwoNullsLeftByHandEdit_shouldThrowJsonProcessingException() {
        String jsonString = """
                [null, {"key": "vimrc", "fileContents": "set number", "filePath": "~", "filename": ".vimrc"}, null]
                """;

        assertThrows(JsonProcessingException.class, () -> JsonHandler.deserialiseToSet(jsonString));
    }

}

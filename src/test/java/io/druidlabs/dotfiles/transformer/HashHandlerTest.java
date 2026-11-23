package io.druidlabs.dotfiles.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

/**
 * Sync decides which side changed by comparing hashes, so a hash that changes when nothing did reports a false
 * conflict, and one that stays the same after a real edit loses that edit.
 */
class HashHandlerTest {

    private static final String BASHRC = """
            # ~/.bashrc — café ☕
            export PS1='\\[\\e[32m\\]\\u@\\h\\[\\e[0m\\] \\w \\$ '
            alias ll='ls -alF'
            \tcomplete -F _git "g"
            """;

    private static final String VIMRC = """
            set number
            set expandtab tabstop=4
            """;

    private static final String SETTINGS_JSON = """
            {
              "editor.fontSize": 14,
              "files.eol": "\\n"
            }
            """;

    private static List<Dotfile> sampleSetup() {
        return List.of(
                new Dotfile("bashrc", BASHRC, "~", ".bashrc"),
                new Dotfile("vimrc", VIMRC, "~", ".vimrc"),
                new Dotfile("vscode", SETTINGS_JSON, "~/.config/Code/User", "settings.json"));
    }

    @Test
    void hashDotfiles_afterSavingAndReloading_shouldNotChange() throws JsonProcessingException {
        List<Dotfile> setup = sampleSetup();

        String saved = JsonHandler.serialiseToJsonString(Set.copyOf(setup));

        Set<Dotfile> reloaded = JsonHandler.deserialiseToSet(saved);

        assertEquals(HashHandler.hashDotfiles(setup), HashHandler.hashDotfiles(reloaded));
    }

    @Test
    void hashDotfiles_ofIndentedFilesFromOlderVersion_shouldMatchCurrentSave() throws JsonProcessingException {
        String indentedWithCrlf = """
                [ {
                  "key" : "vimrc",
                  "fileContents" : "set number\\nset expandtab tabstop=4\\n",
                  "filePath" : "~",
                  "filename" : ".vimrc"
                } ]
                """.replace("\n", "\r\n");

        Set<Dotfile> fromOldFile = JsonHandler.deserialiseToSet(indentedWithCrlf);

        Dotfile current = new Dotfile("vimrc", VIMRC, "~", ".vimrc");

        assertEquals(HashHandler.hashDotfiles(List.of(current)), HashHandler.hashDotfiles(fromOldFile));
    }

    @Test
    void hashDotfiles_shouldIgnoreOrder() throws JsonProcessingException {
        List<Dotfile> setup = sampleSetup();

        List<Dotfile> reversed = setup.reversed();

        assertEquals(HashHandler.hashDotfiles(setup), HashHandler.hashDotfiles(reversed));
    }

    @Test
    void hashDotfiles_withPathSavedOnWindows_shouldMatchLinux() throws JsonProcessingException {
        Dotfile onLinux = new Dotfile("nvim", "set number", "~/.config/nvim", "init.vim");
        Dotfile onWindows = new Dotfile("nvim", "set number", "~\\.config\\nvim", "init.vim");

        assertEquals(HashHandler.hashDotfiles(List.of(onLinux)), HashHandler.hashDotfiles(List.of(onWindows)));
    }

    @Test
    void hashDotfiles_withKeyTypedInDifferentCase_shouldMatch() throws JsonProcessingException {
        Dotfile typedLower = new Dotfile("bashrc", BASHRC, "~", ".bashrc");
        Dotfile typedUpper = new Dotfile("Bashrc", BASHRC, "~", ".bashrc");

        assertEquals(HashHandler.hashDotfiles(List.of(typedLower)), HashHandler.hashDotfiles(List.of(typedUpper)));
    }

    @Test
    void hashDotfiles_shouldDetectOneCharacterEdit() throws JsonProcessingException {
        Dotfile before = new Dotfile("bashrc", BASHRC, "~", ".bashrc");
        Dotfile after = new Dotfile("bashrc", BASHRC.replace("-alF", "-alf"), "~", ".bashrc");

        assertNotEquals(HashHandler.hashDotfiles(List.of(before)), HashHandler.hashDotfiles(List.of(after)));
    }

    @Test
    void hashDotfiles_shouldDetectFileMovedWithoutEdit() throws JsonProcessingException {
        Dotfile before = new Dotfile("nvim", "set number", "~/.vim", "init.vim");
        Dotfile after = new Dotfile("nvim", "set number", "~/.config/nvim", "init.vim");

        assertNotEquals(HashHandler.hashDotfiles(List.of(before)), HashHandler.hashDotfiles(List.of(after)));
    }

    @Test
    void hashDotfiles_shouldDetectRemovedDotfile() throws JsonProcessingException {
        List<Dotfile> setup = sampleSetup();

        List<Dotfile> withoutVscode = setup.subList(0, 2);

        assertNotEquals(HashHandler.hashDotfiles(setup), HashHandler.hashDotfiles(withoutVscode));
    }

    @Test
    void hashDotfiles_shouldDetectLineMovedBetweenFiles() throws JsonProcessingException {
        List<Dotfile> before = List.of(
                new Dotfile("bashrc", "export EDITOR=vim\nalias ll='ls -l'\n", "~", ".bashrc"),
                new Dotfile("aliases", "alias gs='git status'\n", "~", ".bash_aliases"));

        List<Dotfile> after = List.of(
                new Dotfile("bashrc", "export EDITOR=vim\n", "~", ".bashrc"),
                new Dotfile("aliases", "alias ll='ls -l'\nalias gs='git status'\n", "~", ".bash_aliases"));

        assertNotEquals(HashHandler.hashDotfiles(before), HashHandler.hashDotfiles(after));
    }

    @Test
    void hashDotfiles_ofTwoEmptySetups_shouldMatch() throws JsonProcessingException {
        Set<Dotfile> freshInstall = JsonHandler.deserialiseToSet("[]");

        assertEquals(HashHandler.hashDotfiles(List.of()), HashHandler.hashDotfiles(freshInstall));
    }

    /**
     * Kept apart from {@link #sampleSetup()} so that changing the shared sample can't change the pinned values.
     */
    private static List<Dotfile> pinnedSetup() {
        return List.of(
                new Dotfile("vscode", "{\r\n  \"editor.fontSize\": 14,\r\n  \"files.eol\": \"\\n\"\r\n}\r\n",
                        "~/.config/Code/User", "settings.json"),
                new Dotfile("bashrc", "# café ☕ 日本語\nexport PS1='\\[\\e[32m\\]\\u@\\h\\[\\e[0m\\] \\w \\$ '\n"
                        + "\tcomplete -F _git \"g\"\n", "~", ".bashrc"),
                new Dotfile("Nvim", "set number", "~\\.config\\nvim", "init.vim"));
    }

    /**
     * Every test above compares two hashes made by the same build, so a dependency upgrade that changes the output
     * (Jackson escaping differently, a field order change) would pass them all. A snapshot stored in {@code .sync} by
     * the previous version would then never match again, and every user's next sync would report a false conflict.
     * <p>
     * If this fails after an upgrade, don't just update the expected value: the change needs handling for existing
     * snapshots first.
     */
    @Test
    void canonicalForm_shouldMatchWhatEarlierVersionsWrote() throws JsonProcessingException {
        String expected = "[{\"key\":\"bashrc\",\"fileContents\":\"# café ☕ 日本語\\nexport PS1='\\\\[\\\\e[32m\\\\]\\\\u@\\\\h\\\\[\\\\e[0m\\\\] \\\\w \\\\$ '\\n\\tcomplete -F _git \\\"g\\\"\\n\",\"filePath\":\"~\",\"filename\":\".bashrc\"}," +
                "{\"key\":\"nvim\",\"fileContents\":\"set number\",\"filePath\":\"~/.config/nvim\",\"filename\":\"init.vim\"}," +
                "{\"key\":\"vscode\",\"fileContents\":\"{\\r\\n  \\\"editor.fontSize\\\": 14,\\r\\n  \\\"files.eol\\\": \\\"\\\\n\\\"\\r\\n}\\r\\n\",\"filePath\":\"~/.config/Code/User\",\"filename\":\"settings.json\"}]";

        assertEquals(expected, JsonHandler.canonicalSerialiseToJsonString(pinnedSetup()));
    }

    /**
     * See {@link #canonicalForm_shouldMatchWhatEarlierVersionsWrote()}. This one also catches a change to the hash
     * itself, such as the algorithm or the text encoding.
     */
    @Test
    void hashDotfiles_shouldMatchWhatEarlierVersionsStored() throws JsonProcessingException {
        assertEquals("7f16cf86d1391b63b64cf5eb396809832a4ca2c7ffece1d07e5b873aa95d3882",
                HashHandler.hashDotfiles(pinnedSetup()));
    }

    /**
     * Most users start with nothing saved, so this is the snapshot most first syncs store.
     */
    @Test
    void hashDotfiles_ofEmptySetup_shouldMatchWhatEarlierVersionsStored() throws JsonProcessingException {
        assertEquals("4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945",
                HashHandler.hashDotfiles(List.of()));
    }
}

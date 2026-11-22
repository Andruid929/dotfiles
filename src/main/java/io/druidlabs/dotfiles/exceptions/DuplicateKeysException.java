package io.druidlabs.dotfiles.exceptions;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exception thrown when more than one dotfile has the same key. Keys are lower-cased, so {@code Bashrc} and
 * {@code bashrc} are the same key. Loading into a set would keep only one dotfile per key and silently lose the others,
 * so duplicates fail instead. It is a {@link JsonProcessingException} so that anything already handling a file that
 * can't be parsed handles duplicates the same way.
 */
public class DuplicateKeysException extends JsonProcessingException {

    /**
     * The keys that appear more than once, each listed once.
     */
    private final List<String> duplicateKeys;

    /**
     * @param duplicateKeys the keys that appear more than once, each listed once. The message names them, such as
     *                      {@code Duplicate keys: vimrc, bashrc}.
     */
    public DuplicateKeysException(@NotNull List<String> duplicateKeys) {
        this.duplicateKeys = List.copyOf(duplicateKeys);

        String noun = (duplicateKeys.size() == 1) ? "key" : "keys";

        String message = interpolate("Duplicate {}: {}", noun, String.join(", ", duplicateKeys));

        super(message);
    }

    /**
     * @return the keys that appear more than once, each listed once.
     */
    public List<String> getDuplicateKeys() {
        return duplicateKeys;
    }
}

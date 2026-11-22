package io.druidlabs.dotfiles.util;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.exceptions.DuplicateKeysException;

/**
 * Utility class for checks on a collection of dotfiles before it is used.
 */
public final class DotfilesUtil {

    private DotfilesUtil() {
        //Utility classes cannot be instantiated
    }

    /**
     * Checks that every entry is a dotfile and that no two share a key. A {@code null} entry (a {@code null} in the
     * JSON array) is rejected here, since it would otherwise fail later, outside any handling for a bad file.
     *
     * @param dotfileList the dotfiles to check, as read, before anything has dropped duplicates.
     * @return a new, mutable set of the dotfiles.
     * @throws DuplicateKeysException  if more than one dotfile has the same key.
     * @throws JsonProcessingException if an entry is {@code null}.
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Set<Dotfile> validateDotfiles(@NotNull List<Dotfile> dotfileList) throws JsonProcessingException {
        int emptyEntry = dotfileList.indexOf(null);

        if (emptyEntry != -1) {
            throw new JsonMappingException(null, interpolate("Entry {} is empty", emptyEntry + 1));
        }

        List<String> duplicateKeys = findDuplicateKeys(dotfileList);

        if (duplicateKeys.isEmpty()) {
            return new HashSet<>(dotfileList);
        }

        throw new DuplicateKeysException(duplicateKeys);
    }

    /**
     * @param dotfiles the dotfiles to check, none of them {@code null}.
     * @return the keys that appear more than once, each listed once, in the order of their second appearance. Empty if
     * there are none.
     */
    private static @NotNull List<String> findDuplicateKeys(@NotNull List<Dotfile> dotfiles) {
        Set<String> seenKeys = new HashSet<>();

        Set<String> duplicateKeys = new LinkedHashSet<>();

        for (Dotfile dotfile : dotfiles) {
            String key = dotfile.getKey();

            if (!seenKeys.add(key)) {
                duplicateKeys.add(key);
            }
        }

        return List.copyOf(duplicateKeys);
    }

}

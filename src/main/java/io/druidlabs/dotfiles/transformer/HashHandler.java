package io.druidlabs.dotfiles.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.regex.Pattern;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.exceptions.BlankValueException;

/**
 * Hashes dotfiles for sync, which compares hashes rather than contents to tell whether either side changed.
 */
public final class HashHandler {

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private HashHandler() {
        //Utility classes cannot be instantiated
    }

    /**
     * @param dotfiles the dotfiles to hash.
     * @return the SHA-256 hash of their canonical form (see
     * {@link JsonHandler#canonicalSerialiseToJsonString(Collection)}), as lower-case hex. The same dotfiles in any
     * order give the same hash.
     * @throws JsonProcessingException if they could not be serialised.
     */
    public static String hashDotfiles(Collection<Dotfile> dotfiles) throws JsonProcessingException {
        String canonicalised = JsonHandler.canonicalSerialiseToJsonString(dotfiles);

        return hash(canonicalised);
    }

    /**
     * @param text the text to hash, read as UTF-8.
     * @return its SHA-256 hash, as lower-case hex.
     * @throws IllegalStateException if SHA-256 is unavailable, which every Java runtime is required to support.
     */
    public static String hash(@NotNull String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        try {
            byte[] sha256Hash = MessageDigest.getInstance(HASH_ALGORITHM)
                    .digest(bytes);

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(sha256Hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is supposed to be supported", e);
        }
    }

    /**
     * Checks a hash read back from disk is one this class produces. Backups are named after their hash, so this is
     * also what keeps a hand-edited {@code .sync} from pointing outside the backups folder.
     *
     * @param hash the hash to check.
     * @param name what the hash is, for the message.
     * @throws BlankValueException      if the hash is blank.
     * @throws IllegalArgumentException if it isn't 64 lower-case hex characters.
     */
    public static void requireHash(String hash, String name) {
        if (hash == null || hash.isBlank()) {
            throw new BlankValueException(name + " is required");
        }

        if (!HASH_PATTERN.matcher(hash).matches()) {
            throw new IllegalArgumentException(name + " must be a SHA-256 hash of 64 lower-case hex characters");
        }
    }
}

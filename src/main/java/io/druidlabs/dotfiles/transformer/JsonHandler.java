package io.druidlabs.dotfiles.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

/**
 * Utility class for converting between Dotfile objects and JSON strings.
 * Uses Jackson for JSON processing.
 */
public final class JsonHandler {

    /**
     * The Jackson type for reading a set of dotfiles.
     */
    public static final TypeReference<Set<Dotfile>> DOT_FILES_TYPE = new TypeReference<>() {
    };

    /**
     * The shared mapper: compact output (on one line), dates as ISO strings rather than timestamps, unknown
     * properties ignored, and the source included in parse error locations.
     */
    private static final JsonMapper mapper;

    static {
        mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private JsonHandler() {
        //Utility classes cannot be instantiated
    }

    /**
     * @return the shared mapper.
     */
    public static JsonMapper getMapper() {
        return mapper;
    }

    /**
     * @param collectionOfDotfiles the dotfiles to serialise.
     * @return the dotfiles as a compact JSON array, in the collection's iteration order.
     * @throws JsonProcessingException if they could not be serialised.
     */
    public static String serialiseToJsonString(Collection<Dotfile> collectionOfDotfiles) throws JsonProcessingException {
        return mapper.writeValueAsString(collectionOfDotfiles);
    }

    /**
     * Serialises the dotfiles in their canonical form: sorted by key, so the same dotfiles always give the same string
     * whatever order they came in. Sync hashes this form, so any change to it (the mapper's settings, or the
     * properties {@link Dotfile} serialises) changes every hash and makes the next sync report a conflict.
     *
     * @param dotfiles the dotfiles to serialise.
     * @return the dotfiles as a compact JSON array, sorted by key.
     * @throws JsonProcessingException if they could not be serialised.
     */
    public static String canonicalSerialiseToJsonString(@NotNull Collection<Dotfile> dotfiles) throws JsonProcessingException {
        List<Dotfile> sortedByKey = dotfiles.stream()
                .sorted(Comparator.comparing(Dotfile::getKey))
                .toList();

        return serialiseToJsonString(sortedByKey);
    }

    /**
     * @param jsonString a JSON array of dotfiles.
     * @return a new, mutable set of the dotfiles.
     * @throws JsonProcessingException if {@code jsonString} is not a valid array of dotfiles, including when a
     *                                 dotfile has a blank key or blank contents.
     */
    public static @NotNull Set<Dotfile> deserialiseToSet(@NotNull String jsonString) throws JsonProcessingException {
        Set<Dotfile> savedDotfiles = mapper.readValue(jsonString, DOT_FILES_TYPE);

        return new HashSet<>(savedDotfiles);
    }

    /**
     * @param jsonString the JSON to read.
     * @param reference  the type to read it as.
     * @param <T>        the type to read it as.
     * @return the parsed value.
     * @throws JsonProcessingException if {@code jsonString} is not valid JSON for that type.
     */
    public static <T> T deserialise(String jsonString, TypeReference<T> reference) throws JsonProcessingException {
        return mapper.readValue(jsonString, reference);
    }
}

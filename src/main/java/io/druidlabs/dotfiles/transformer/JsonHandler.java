package io.druidlabs.dotfiles.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

/**
 * Utility class for converting between Dotfile objects and JSON strings.
 * Uses Jackson for JSON processing.
 */
public final class JsonHandler {

    public static final TypeReference<Set<Dotfile>> DOT_FILES_TYPE = new TypeReference<>() {
    };

    private static final JsonMapper mapper;

    static {
        mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private JsonHandler() {
        //Utility classes cannot be instantiated
    }

    public static JsonMapper getMapper() {
        return mapper;
    }

    public static String serialiseToJsonString(Set<Dotfile> setOfDotfiles) throws JsonProcessingException {
        return mapper.writeValueAsString(setOfDotfiles);
    }

    public static @NotNull Set<Dotfile> deserialiseToSet(@NotNull String jsonString) throws JsonProcessingException {
        Set<Dotfile> savedDotfiles = mapper.readValue(jsonString, DOT_FILES_TYPE);

        return new HashSet<>(savedDotfiles);
    }

    public static <T> T deserialise(String jsonString, TypeReference<T> reference) throws JsonProcessingException {
        return mapper.readValue(jsonString, reference);
    }
}

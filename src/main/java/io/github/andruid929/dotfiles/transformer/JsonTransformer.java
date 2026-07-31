package io.github.andruid929.dotfiles.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import io.github.andruid929.dotfiles.Dotfile;
import io.github.andruid929.dotfiles.annotations.Blankable;

/**
 * Utility class for converting between Dotfile objects and JSON strings.
 * Uses Jackson for JSON processing.
 */
public final class JsonTransformer {

    public static final TypeReference<Set<Dotfile>> DOT_FILES_TYPE = new TypeReference<>() {
    };
    public static final JsonMapper mapper;
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTransformer.class);

    static {
        mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private JsonTransformer() {
    }

    @Blankable
    public static String serialiseToJsonString(Set<Dotfile> setOfDotfiles) {
        try {
            return mapper.writeValueAsString(setOfDotfiles);

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to transform loaded dotfiles", e);

            return "";
        }
    }

    @Unmodifiable
    public static Set<Dotfile> deserialiseToSet(@NotNull String jsonString) {
        try {
            Set<Dotfile> savedDotfiles = mapper.readValue(jsonString, DOT_FILES_TYPE);

            return Collections.unmodifiableSet(savedDotfiles);

        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to transform saved dotfiles", e);

            return Collections.emptySet();
        }
    }

}

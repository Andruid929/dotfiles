package io.druidlabs.dotfiles.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import io.druidlabs.dotfiles.sync.SyncProvider;

/**
 * The configs the app supports. Each has its key as written in the config file, a default value, a description
 * shown by the {@code config} command, and a validator that normalises values to a canonical form.
 */
public enum ConfigOption {

    /**
     * The provider to sync with: a {@link SyncProvider} ID, or {@link Configuration#EMPTY_CONFIG_VALUE} for none.
     */
    SYNC_PROVIDER("SyncProvider", Configuration.EMPTY_CONFIG_VALUE, "Online storage provider",
            Validators.SYNC_PROVIDER_VALIDATOR),

    /**
     * Whether config errors are written to the log file. Off by default so every command does not log the same
     * errors again.
     */
    LOG_CONFIG_ERRORS("LogConfigErrors", "false", "Write config errors to the log file (the config command always shows them)",
            Validators.BOOLEAN_VALIDATOR);

    private final String configKey;
    private final String defaultValue;
    private final String description;

    /**
     * Returns a value in its canonical form, or throws {@link InvalidConfigException} if it is not valid.
     */
    private final Function<String, String> validator;

    ConfigOption(String configKey, String defaultValue, String description, Function<String, String> validator) {
        this.configKey = configKey;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validator = validator;
    }

    /**
     * @param input a config key as typed, matched case-insensitively and ignoring surrounding whitespace.
     * @return the matching option, or {@code null} if there is none.
     */
    public static @Nullable ConfigOption fromInput(@NotNull String input) {
        for (ConfigOption option : values()) {

            if (option.getConfigKey().equalsIgnoreCase(input.trim())) return option;
        }

        return null;
    }

    /**
     * @return the key as written in the config file, in its canonical case.
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * @return the description shown by the {@code config} command.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the value used when the config is not set.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Validates a value and returns it in canonical form, such as {@code true} for {@code on}.
     *
     * @param input the value as typed or read from the config file.
     * @return the canonical value.
     * @throws InvalidConfigException if {@code input} is {@code null} or not a valid value for this config.
     */
    public @NotNull String normalise(String input) throws InvalidConfigException {
        if (input == null) {
            throw new InvalidConfigException("Missing config value");
        }

        return validator.apply(input);
    }

    @Override
    public String toString() {
        return this.configKey;
    }

    /**
     * Validators that normalise a raw config value or throw {@link InvalidConfigException}.
     */
    private static class Validators {

        /**
         * Accepts any value, trimmed.
         */
        final static Function<String, String> SIMPLE_STRING_VALIDATOR = String::trim;

        /**
         * Accepts {@code on}, {@code 1}, {@code true} or {@code t} as {@code true}, and {@code off}, {@code 0},
         * {@code false} or {@code f} as {@code false}, in any case.
         */
        final static Function<String, String> BOOLEAN_VALIDATOR = s -> {
            String trimmed = SIMPLE_STRING_VALIDATOR.apply(s);

            return switch (trimmed.toLowerCase()) {
                case "on", "1", "true", "t" -> "true";
                case "off", "0", "false", "f" -> "false";
                default -> throw new InvalidConfigException("'{}' is not a supported true/false value", trimmed);
            };
        };

        /**
         * Accepts {@link Configuration#EMPTY_CONFIG_VALUE} or a {@link SyncProvider} ID in any case, returning the
         * ID in its canonical case.
         */
        final static Function<String, String> SYNC_PROVIDER_VALIDATOR = s -> {
            if (s.trim().equalsIgnoreCase(Configuration.EMPTY_CONFIG_VALUE)) {
                return Configuration.EMPTY_CONFIG_VALUE;
            }

            SyncProvider provider = SyncProvider.fromString(s);

            if (provider == null) {
                throw new InvalidConfigException("Unknown sync provider '{}'", s);
            }

            return provider.getId();
        };
    }
}

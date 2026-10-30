package io.druidlabs.dotfiles.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import io.druidlabs.dotfiles.sync.SyncProvider;

public enum ConfigOption {

    SYNC_PROVIDER("SyncProvider", Configuration.EMPTY_CONFIG_VALUE, "Online storage provider",
            Validators.SYNC_PROVIDER_VALIDATOR),

    LOG_CONFIG_ERRORS("LogConfigErrors", "false", "Write config errors to the log file (the config command always shows them)",
            Validators.BOOLEAN_VALIDATOR);

    private final String configKey;
    private final String defaultValue;
    private final String description;
    private final Function<String, String> validator;

    ConfigOption(String configKey, String defaultValue, String description, Function<String, String> validator) {
        this.configKey = configKey;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validator = validator;
    }

    public static @Nullable ConfigOption fromInput(@NotNull String input) {
        for (ConfigOption option : values()) {

            if (option.getConfigKey().equalsIgnoreCase(input.trim())) return option;
        }

        return null;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

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

    private static class Validators {

        final static Function<String, String> SIMPLE_STRING_VALIDATOR = String::trim;

        final static Function<String, String> BOOLEAN_VALIDATOR = s -> {
            String trimmed = SIMPLE_STRING_VALIDATOR.apply(s);

            return switch (trimmed.toLowerCase()) {
                case "on", "1", "true", "t" -> "true";
                case "off", "0", "false", "f" -> "false";
                default -> throw new InvalidConfigException("'{}' is not a supported true/false value", trimmed);
            };
        };

        final static Function<String, String> SYNC_PROVIDER_VALIDATOR = s -> {
            SyncProvider provider = SyncProvider.fromString(s);

            if (provider == null) {
                throw new InvalidConfigException("Unknown sync provider '{}'", s);
            }

            return provider.getId();
        };
    }
}

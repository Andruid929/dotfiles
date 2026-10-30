package io.druidlabs.dotfiles.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import io.github.andruid929.leutils.config.Config;

public final class Configuration {

    public static final String EMPTY_CONFIG_VALUE = "none";

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private static boolean hasUnsavedChanges = false;

    private final List<String> configMessages = new ArrayList<>();

    private final boolean savingBlocked;

    private Configuration(@Nullable Config savedConfigs, boolean savingBlocked, String @NotNull ... configMessages) {
        this.savingBlocked = savingBlocked;

        loadDefaults();

        if (configMessages.length != 0) {
            this.configMessages.addAll(Arrays.asList(configMessages));
        }

        if (savedConfigs != null) {
            if (!savedConfigs.getInvalidConfigs().isEmpty()) {
                this.configMessages.addAll(savedConfigs.getInvalidConfigs());
            }

            for (String key : savedConfigs.getAllSavedConfigs().keySet()) {
                ConfigOption option = ConfigOption.fromInput(key);

                if (option == null) {
                    continue;
                }

                String value = savedConfigs.getString(key);

                try {
                    loadConfig(option, value);

                } catch (InvalidConfigException e) {
                    this.configMessages.add(e.getMessage());
                }
            }
        }

        if (!this.configMessages.isEmpty()) {
            // Don't let an ordinary command rewrite a file that had lines we couldn't load.
            // Later changes from the config command set the flag again and still save.
            hasUnsavedChanges = false;
        }

        boolean logConfigErrors = Boolean.parseBoolean(getConfig(ConfigOption.LOG_CONFIG_ERRORS));

        if (logConfigErrors) {
            this.configMessages.forEach(logger::error);
        }
    }

    /**
     * @param configKey the config key to check, as a user would type it.
     * @return {@code true} if {@code configKey} matches a known {@link ConfigOption}.
     */
    public static boolean isExistingConfig(@NotNull String configKey) {
        return ConfigOption.fromInput(configKey) != null;
    }

    @Contract(" -> new")
    public static @NotNull Configuration createInstance() {
        if (Files.notExists(FilePaths.CONFIG_FILE)) {
            return new Configuration(null, false);
        }

        Config config;

        try {
            config = Config.readConfig(FilePaths.CONFIG_FILE);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e,
                    "Failed to read config file because {}. Using default configs",
                    "Failed to read config file. Using default configs");

            logger.error("Failed to read config file", e);

            return new Configuration(null, true, message);
        }

        return new Configuration(config, false);
    }

    private void loadDefaults() {
        setDefaultConfig(ConfigOption.SYNC_PROVIDER);
        setDefaultConfig(ConfigOption.LOG_CONFIG_ERRORS);
    }

    private void setDefaultConfig(@NotNull ConfigOption option) {
        Config.add(option.getConfigKey(), option.getDefaultValue());
    }

    public void setConfig(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = normaliseValue(key, value);

        String configKey = key.getConfigKey();

        String currentValue = Config.get(configKey);

        if (!currentValue.equals(normalised)) {
            Config.add(configKey, normalised);

            hasUnsavedChanges = true;
        }
    }

    private void loadConfig(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = normaliseValue(key, value);

        String configKey = key.getConfigKey();

        if (!normalised.equals(value)) {
            hasUnsavedChanges = true;
        }

        Config.add(configKey, normalised);
    }

    private @NotNull String normaliseValue(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = key.normalise(value);

        if (normalised.isBlank()) {
            throw new InvalidConfigException("Value for config {} is blank", key);
        }

        return normalised;
    }

    /**
     * Resets a single config back to its default value, only persisting the change if it was not
     * already at its default.
     *
     * @param option the config to reset.
     */
    public void resetToDefault(@NotNull ConfigOption option) {
        String configKey = option.getConfigKey();
        String defaultValue = option.getDefaultValue();

        String currentValue = Config.get(configKey);

        if (!currentValue.equals(defaultValue)) {
            Config.add(configKey, defaultValue);

            hasUnsavedChanges = true;
        }
    }

    /**
     * Resets every known config back to its default value.
     */
    public void resetToDefault() {
        for (ConfigOption option : ConfigOption.values()) {
            resetToDefault(option);
        }
    }

    public @Nullable String getConfig(@NotNull ConfigOption option) {
        String key = option.getConfigKey();

        return Config.get(key);
    }

    /**
     * @return every currently loaded config, keyed by its config key.
     */
    public @NotNull @Unmodifiable Map<String, String> getAllConfigs() {
        return Map.copyOf(Config.getLoadedConfigs());
    }

    public void saveConfigs() {
        if (!hasUnsavedChanges) {
            return;
        }

        if (savingBlocked) {
            UserNotification.notifyUserOfError("Configs were not saved because the config file could not be read. "
                    + "Fix or remove it and try again");

            return;
        }

        try {
            Config.persistConfig(FilePaths.CONFIG_FILE, false);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e, "Unable to save configs because {}",
                    "Unexpected error occurred while saving configs");

            UserNotification.notifyUserOfError(message);

            logger.error(message, e);
        }
    }

    /**
     * @return {@code true} if the config file exists but could not be read, in which case configs are never saved.
     */
    public boolean isSavingBlocked() {
        return savingBlocked;
    }

    @Contract(pure = true)
    public @NotNull @Unmodifiable List<String> getConfigMessages() {
        return List.copyOf(configMessages);
    }
}

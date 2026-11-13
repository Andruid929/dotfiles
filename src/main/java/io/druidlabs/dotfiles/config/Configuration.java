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

/**
 * The app's configs, loaded from {@link FilePaths#CONFIG_FILE} on top of each option's default.
 * Values live in le-utils' static {@link Config} store, so only one instance is meant to exist per run, created by
 * {@link io.druidlabs.dotfiles.Worker#start()}.
 */
public final class Configuration {

    /**
     * The value for a config that is deliberately unset, such as having no sync provider.
     */
    public static final String EMPTY_CONFIG_VALUE = "none";

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    /**
     * Whether {@link #saveConfigs()} has anything to write. Static, like the {@link Config} store it tracks.
     */
    private static boolean hasUnsavedChanges = false;

    /**
     * Problems found while loading the config file, in the order they were found.
     */
    private final List<String> configMessages = new ArrayList<>();

    /**
     * Set when the config file exists but could not be read, so saving can't replace it with defaults.
     */
    private final boolean savingBlocked;

    /**
     * Loads the defaults, then every valid config from {@code savedConfigs}. Unknown keys are ignored, and invalid
     * values keep their default and are recorded as config messages. If there are any messages, the file is not
     * rewritten unless a later change asks for it.
     *
     * @param savedConfigs   the configs read from the file, or {@code null} to use the defaults only.
     * @param savingBlocked  whether the config file exists but could not be read.
     * @param configMessages problems already found while reading the file.
     */
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

    /**
     * Loads the config file, or the defaults only if it doesn't exist. If it exists but could not be read, the
     * defaults are used and saving is blocked so the file is never overwritten.
     *
     * @return the loaded configs.
     */
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

    /**
     * Puts every option's default value into the {@link Config} store.
     */
    private void loadDefaults() {
        for (ConfigOption option : ConfigOption.values()) {
            
            setDefaultConfig(option);
        }
    }

    /**
     * Puts {@code option}'s default value into the {@link Config} store.
     */
    private void setDefaultConfig(@NotNull ConfigOption option) {
        Config.add(option.getConfigKey(), option.getDefaultValue());
    }

    /**
     * Sets a config after normalising its value, marking it for saving if the value changed.
     *
     * @param key   the config to set.
     * @param value the new value as typed.
     * @throws InvalidConfigException if {@code value} is not valid for the config, or is blank.
     */
    public void setConfig(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = normaliseValue(key, value);

        String configKey = key.getConfigKey();

        String currentValue = Config.get(configKey);

        if (!currentValue.equals(normalised)) {
            Config.add(configKey, normalised);

            hasUnsavedChanges = true;
        }
    }

    /**
     * Loads a value read from the config file. If normalising changed it, the file is marked for saving so it is
     * rewritten in canonical form.
     *
     * @throws InvalidConfigException if {@code value} is not valid for the config, or is blank.
     */
    private void loadConfig(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = normaliseValue(key, value);

        String configKey = key.getConfigKey();

        if (!normalised.equals(value)) {
            hasUnsavedChanges = true;
        }

        Config.add(configKey, normalised);
    }

    /**
     * @return {@code value} in canonical form.
     * @throws InvalidConfigException if {@code value} is not valid for the config, or is blank.
     */
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

    /**
     * @param option the config to read.
     * @return the config's current value, which is its default unless it was set or loaded from the file.
     */
    public String getConfig(@NotNull ConfigOption option) {
        String key = option.getConfigKey();

        return Config.get(key);
    }

    /**
     * @return every currently loaded config, keyed by its config key.
     */
    public @NotNull @Unmodifiable Map<String, String> getAllConfigs() {
        return Map.copyOf(Config.getLoadedConfigs());
    }

    /**
     * Writes the configs to {@link FilePaths#CONFIG_FILE} if anything changed. When saving is blocked, the user is
     * told instead. Failures are reported to the user rather than thrown.
     */
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

    /**
     * @return the problems found while loading the config file, in the order they were found.
     */
    @Contract(pure = true)
    public @NotNull @Unmodifiable List<String> getConfigMessages() {
        return List.copyOf(configMessages);
    }
}

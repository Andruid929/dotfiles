package io.druidlabs.dotfiles.config;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.druidlabs.dotfiles.constants.FilePaths;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import io.github.andruid929.leutils.config.Config;

public final class Configuration {

    public static final String EMPTY_CONFIG_VALUE = "none";

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static boolean hasUnsavedChanges = false;

    private final List<String> configMessages = new ArrayList<>();

    private Configuration(@Nullable Config savedConfigs, String @NotNull ... configMessages) {
        loadDefaults(true);

        if (configMessages.length != 0) {
            this.configMessages.addAll(Arrays.asList(configMessages));
        }

        if (savedConfigs == null) {
            return;
        }

        if (!savedConfigs.getInvalidConfigs().isEmpty()) {
            this.configMessages.addAll(savedConfigs.getInvalidConfigs());
        }

        if (savedConfigs.size() == 0) {
            return;
        }

        for (String key : savedConfigs.getAllSavedConfigs().keySet()) {
            ConfigOption option = ConfigOption.fromInput(key);

            if (option == null) {
                continue;
            }

            String value = savedConfigs.getString(key);

            try {
                setConfig(option, value);

            } catch (InvalidConfigException e) {
                this.configMessages.add(e.getMessage());
            }
        }
    }

    public static void loadDefaults(boolean isFirstRun) {
        setDefaultConfig(ConfigOption.SYNC_PROVIDER);
        setDefaultConfig(ConfigOption.HIDE_CONFIG_ERRORS);

        if (!isFirstRun) {
            hasUnsavedChanges = true;
        }
    }

    public static void setDefaultConfig(@NotNull ConfigOption option) {
        Config.add(option.getConfigKey(), option.getDefaultValue());
    }

    public void setConfig(@NotNull ConfigOption key, String value) throws InvalidConfigException {
        String normalised = key.normalise(value);

        if (normalised.isBlank()) {
            throw new InvalidConfigException("Value for config {} is blank", key);
        }

        String configKey = key.getConfigKey();

        String currentValue = Config.get(configKey);

        if (!currentValue.equalsIgnoreCase(normalised)) {
            Config.add(configKey, normalised);

            hasUnsavedChanges = true;
        }
    }

    public @Nullable String getConfig(@NotNull ConfigOption option) {
        String key = option.getConfigKey();

        return Config.get(key);
    }

    public void saveConfigs() {
        if (!hasUnsavedChanges) {
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

    public List<String> getConfigMessages() {
        return configMessages;
    }

    @Contract(" -> new")
    public static @NotNull Configuration createInstance() {
        if (Files.notExists(FilePaths.CONFIG_FILE)) {
            return new Configuration(null);
        }

        Config config;

        try {
            config = Config.readConfig(FilePaths.CONFIG_FILE);

        } catch (IOException e) {
            String message = ExceptionUtil.describeIOFailure(e, "Failed to get read config file because {}. Using default",
                    "Failed to read config file, Using default configs");

            logger.error("Failed to read config file", e);

            return new Configuration(null, message);
        }

        return new Configuration(config);
    }
}

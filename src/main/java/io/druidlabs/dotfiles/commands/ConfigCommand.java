package io.druidlabs.dotfiles.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.config.ConfigOption;
import io.druidlabs.dotfiles.config.Configuration;
import io.druidlabs.dotfiles.config.InvalidConfigException;
import io.druidlabs.dotfiles.sync.SyncProvider;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

/**
 * Command to view and change the application's configs.
 * With no flags, lists every config; otherwise resets or sets a single one.
 */
@Command(name = "config", description = "Set and view configs")
public final class ConfigCommand extends CommandAction {

    @ArgGroup(exclusive = true)
    Action action;

    @Override
    public Integer call() {
        Configuration configuration = Worker.getConfiguration();

        reportConfigMessages(configuration);

        if (action == null) {
            listConfigs(configuration);

            return ExitCode.OK;
        }

        if (configuration.isSavingBlocked()) {
            UserNotification.notifyUserOfError("The config file could not be read, so configs can't be changed. "
                    + "Fix or remove it and try again");

            return ExitCode.SOFTWARE;
        }

        if (action.reset != null) {
            return handleReset(configuration, action.reset.trim());
        }

        return handleSet(configuration, action.set);
    }

    /**
     * Shows any problems found while loading the config file, before the command's own output.
     */
    private void reportConfigMessages(@NotNull Configuration configuration) {
        List<String> messages = configuration.getConfigMessages();

        if (messages.isEmpty()) {
            return;
        }

        String noun = messages.size() == 1 ? "Issue" : "Issues";

        UserNotification.notifyUser("{} with loading configs", noun);

        messages.forEach(UserNotification::notifyUser);

        UserNotification.notifyUser("");
    }

    /**
     * Prints every config with its current value and description.
     */
    private void listConfigs(@NotNull Configuration configuration) {
        UserNotification.notifyUser("Dotfiles configs");
        UserNotification.notifyUser("");

        for (ConfigOption option : ConfigOption.values()) {
            UserNotification.notifyUser("{} -> {}: {}", option.getConfigKey(), configuration.getConfig(option),
                    option.getDescription());
        }
    }

    /**
     * Resets the config named by {@code resetKey}, or every config for {@code all}, then warns if that changed the
     * sync provider.
     *
     * @param resetKey the config key or {@code all}, already trimmed.
     * @return {@link ExitCode#USAGE} for a blank or unknown key, otherwise {@link ExitCode#OK}.
     */
    private int handleReset(@NotNull Configuration configuration, @NotNull String resetKey) {
        if (resetKey.isBlank()) {
            UserNotification.notifyUserOfError("--reset needs a config key, or 'all' to reset everything");

            return ExitCode.USAGE;
        }

        String previousProvider = configuration.getConfig(ConfigOption.SYNC_PROVIDER);

        if (resetKey.equalsIgnoreCase("all")) {
            Map<String, String> before = configuration.getAllConfigs();

            configuration.resetToDefault();

            if (before.equals(configuration.getAllConfigs())) {
                UserNotification.notifyUser("All configs are already using their defaults");

            } else {
                notifyAndLog("Reset all configs to their defaults");
            }

            warnIfProviderChanged(configuration, previousProvider);

            return ExitCode.OK;
        }

        ConfigOption option = ConfigOption.fromInput(resetKey);

        if (option == null) {
            reportUnknownConfig(resetKey);

            return ExitCode.USAGE;
        }

        String configKey = option.getConfigKey();
        String defaultValue = option.getDefaultValue();

        if (defaultValue.equals(configuration.getConfig(option))) {
            UserNotification.notifyUser("Config {} is already using its default", configKey);

            return ExitCode.OK;
        }

        configuration.resetToDefault(option);

        notifyAndLog("Set config {} to its default: {}", configKey, defaultValue);

        warnIfProviderChanged(configuration, previousProvider);

        return ExitCode.OK;
    }

    /**
     * Sets a config from a {@code key:value} pair, then warns if that changed the sync provider. Only the first
     * colon splits the pair, so the value may contain colons.
     *
     * @param keyValue the pair as typed.
     * @return {@link ExitCode#USAGE} for a malformed pair, an unknown key or an invalid value, otherwise
     * {@link ExitCode#OK}.
     */
    private int handleSet(@NotNull Configuration configuration, @NotNull String keyValue) {
        String[] keyAndValue = keyValue.split(":", 2);

        if (keyAndValue.length != 2 || keyAndValue[0].isBlank()) {
            UserNotification.notifyUserOfError("--set needs a key:value pair, for example SyncProvider:GDrive");

            return ExitCode.USAGE;
        }

        String key = keyAndValue[0].trim();

        ConfigOption option = ConfigOption.fromInput(key);

        if (option == null) {
            reportUnknownConfig(key);

            return ExitCode.USAGE;
        }

        String configKey = option.getConfigKey();

        String previousValue = configuration.getConfig(option);
        String previousProvider = configuration.getConfig(ConfigOption.SYNC_PROVIDER);

        try {
            configuration.setConfig(option, keyAndValue[1]);

        } catch (InvalidConfigException e) {
            UserNotification.notifyUserOfError(e.getMessage());

            return ExitCode.USAGE;
        }

        String newValue = configuration.getConfig(option);

        if (Objects.equals(previousValue, newValue)) {
            UserNotification.notifyUser("Config {} is already set to '{}'", configKey, newValue);

            return ExitCode.OK;
        }

        notifyAndLog("Set config {} to '{}'", configKey, newValue);

        warnIfProviderChanged(configuration, previousProvider);

        return ExitCode.OK;
    }

    /**
     * Tells the user {@code key} is not a config and lists the supported ones.
     */
    private void reportUnknownConfig(@NotNull String key) {
        String supportedKeys = Arrays.stream(ConfigOption.values())
                .map(ConfigOption::getConfigKey)
                .collect(Collectors.joining(", "));

        UserNotification.notifyUserOfError("'{}' is not a supported config. Supported configs: {}", key, supportedKeys);
    }

    /**
     * Tells the user that the previous provider's credentials will be revoked on the next sync command, when the
     * sync provider has just changed away from a real provider. The revoke itself happens there, not here.
     */
    private void warnIfProviderChanged(@NotNull Configuration configuration, @Nullable String previousProvider) {
        String currentProvider = configuration.getConfig(ConfigOption.SYNC_PROVIDER);

        if (Objects.equals(previousProvider, currentProvider)) {
            return;
        }

        SyncProvider oldProvider = previousProvider == null ? null : SyncProvider.fromString(previousProvider);

        if (oldProvider == null) {
            return;
        }

        UserNotification.notifyUser("{}'s credentials will be revoked the next time you run a sync command",
                oldProvider.getName());
    }

    /**
     * The mutually exclusive {@code --reset} and {@code --set} options. picocli leaves this {@code null} when
     * neither is given.
     */
    static final class Action {

        @Option(names = {"-r", "--reset"}, paramLabel = "<key|all>",
                description = "Reset a specific config, or 'all' to reset everything")
        String reset;

        @Option(names = {"-s", "--set"}, paramLabel = "<key:value>", description = "Set a config as key:value")
        String set;
    }

}

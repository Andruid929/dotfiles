package io.druidlabs.dotfiles.commands.sync;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.druidlabs.dotfiles.commands.CommandAction;
import io.druidlabs.dotfiles.sync.LogoutResult;
import io.druidlabs.dotfiles.sync.RemoteStore;
import io.druidlabs.dotfiles.sync.SyncProvider;
import io.druidlabs.dotfiles.sync.SyncWorker;
import io.druidlabs.dotfiles.sync.exceptions.MissingSyncProviderException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;
import io.druidlabs.dotfiles.util.ExceptionUtil;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

/**
 * Command to log out of the sync provider set in the config, revoking the stored credential with the provider before
 * deleting it locally. If the revoke fails, the local credential is kept. {@code --force} deletes it without revoking,
 * for when the provider can't be reached or the stored credential can't be read, and warns that the login is still
 * active online. Never prompts. Exits with {@code USAGE} when no provider is set, since that is for the user to fix.
 */
@Command(name = "logout", description = "Revoke access to the sync provider")
public final class LogoutCommand extends CommandAction {

    @Option(names = {"-f", "--force"}, description = "Skip revoking the online credential and just clear the local one")
    boolean forceLogout;

    @Override
    public Integer call() {
        RemoteStore store;

        try {
            store = SyncWorker.getRemoteStore();

        } catch (MissingSyncProviderException e) {
            notifyAndLogFailure("Failed to logout. {}", e.getMessage());

            return ExitCode.USAGE;
        }

        if (forceLogout) {
            boolean credentialStored = holdsAnything(store.tokensPath());

            try {
                store.discardLocalCredential();

            } catch (IOException e) {
                String message = ExceptionUtil.describeIOFailure(e, "Failed to delete local credential because {}",
                        "Failed to delete local credential");

                notifyAndLogFailureWithException(message, e);

                return ExitCode.SOFTWARE;
            }

            if (!credentialStored) {
                UserNotification.notify("You are not logged in");

                return ExitCode.OK;
            }

            SyncProvider syncProvider = store.getSyncProvider();

            String warning = "Local credential deleted without revoking it. Your login is still active on {}, revoke it at {}";

            notifyAndLog(warning, syncProvider.getName(), syncProvider.getPermissionsUrl());

            return ExitCode.OK;
        }

        LogoutResult result;

        try {
            result = store.logout();

        } catch (SyncException e) {
            notifyAndLogFailureWithException(e.getMessage(), e);

            return ExitCode.SOFTWARE;
        }

        String message = getResultMessage(result);

        if (result.equals(LogoutResult.NOT_LOGGED_IN)) {
            UserNotification.notify(message);

        } else {
            notifyAndLog(message);
        }

        return ExitCode.OK;
    }

    /**
     * Checks whether {@code folder} exists and holds anything, so {@code --force} only warns about a live login
     * when there was a credential to delete. An empty folder, such as one left by a cancelled login, holds none.
     *
     * @param folder the folder to check.
     * @return {@code true} if the folder holds anything, or could not be listed, so a credential is assumed.
     */
    private boolean holdsAnything(@NotNull Path folder) {
        if (Files.notExists(folder)) {
            return false;
        }

        try (Stream<Path> entries = Files.list(folder)) {
            return entries.findAny().isPresent();

        } catch (IOException e) {
            return true;
        }
    }

    @Contract(pure = true)
    private @NotNull String getResultMessage(@NotNull LogoutResult result) {
        return switch (result) {
            case REVOKED -> "Successfully revoked online credential and deleted local credential";
            case DISCARDED_WITHOUT_REVOKE -> "Nothing to revoke found in local credential so it was removed";
            case NOT_LOGGED_IN -> "You are not logged in, there is nothing to revoke";
        };
    }
}

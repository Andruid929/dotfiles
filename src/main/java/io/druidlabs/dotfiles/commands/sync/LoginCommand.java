package io.druidlabs.dotfiles.commands.sync;

import io.druidlabs.dotfiles.commands.CommandAction;
import io.druidlabs.dotfiles.sync.RemoteStore;
import io.druidlabs.dotfiles.sync.SyncWorker;
import io.druidlabs.dotfiles.sync.exceptions.MissingCredentialsException;
import io.druidlabs.dotfiles.sync.exceptions.MissingSyncProviderException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

/**
 * Command to log in to the sync provider set in the config, storing a credential for later sync commands.
 * Does nothing if a credential is already stored. Exits with {@code USAGE} when no provider is set or its client
 * credentials are missing, since both are for the user to fix.
 */
@Command(name = "login", description = "Authorise this app to the set sync provider")
public final class LoginCommand extends CommandAction {

    @Override
    public Integer call() {
        RemoteStore store;

        try {
            store = SyncWorker.getRemoteStore();

        } catch (MissingSyncProviderException e) {
            notifyAndLogError("Failed to login. {}", e.getMessage());

            return ExitCode.USAGE;
        }

        if (store.isLoggedIn()) {
            UserNotification.notifyUser("You are already logged in");

            return ExitCode.OK;
        }

        try {
            store.login();

        } catch (SyncException e) {

            if (e instanceof MissingCredentialsException) {
                notifyAndLogError("Failed to login. {}", e.getMessage());

                return ExitCode.USAGE;

            } else {
                notifyAndLogErrorWithException(e.getMessage(), e);

                return ExitCode.SOFTWARE;
            }
        }

        notifyAndLog("Successfully logged in to {}", store.getSyncProvider().getName());

        return ExitCode.OK;
    }
}

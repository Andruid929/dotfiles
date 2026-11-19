package io.druidlabs.dotfiles.commands.sync;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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
 * Command to report the local sync state: the sync provider set in the config, whether a login is stored, and whether
 * the client credentials are usable. Everything is checked locally, so it never contacts the provider, never prompts
 * and never changes anything on disk. A stored login is not proof the login still works, since a login revoked on the
 * provider's side only shows up at the next sync.
 * <p>
 * Every check runs, and the most severe problem decides the exit code: {@code SOFTWARE} for a stored login or
 * credentials file that can't be used, then {@code USAGE} for a missing provider or credentials file, otherwise
 * {@code OK}, including when simply logged out. Only the {@code SOFTWARE} faults are logged.
 */
@Command(name = "status", description = "Check the current local sync provider state")
public final class StatusCommand extends CommandAction {

    private int worstCheckCode = ExitCode.OK;

    @Override
    public Integer call() {
        RemoteStore store;

        try {
            store = SyncWorker.getRemoteStore();

            UserNotification.notify("Set sync provider: {}", store.getSyncProvider().getName());

        } catch (MissingSyncProviderException e) {
            UserNotification.notifyFailure(e.getMessage());

            return ExitCode.USAGE;
        }

        boolean isLoggedIn = store.isLoggedIn();

        checkForLoggedInIssues:
        {
            if (isLoggedIn) {
                UserNotification.notify("A login is stored on disk");

            } else {
                List<String> unloggedErrors = store.drainUnloggedErrors();

                if (unloggedErrors.isEmpty()) {
                    UserNotification.notify("You are not logged in, run 'dotfiles login' to login");

                    break checkForLoggedInIssues;
                }

                String toLog = buildErrorsString(unloggedErrors);

                notifyAndLogFailure(toLog);

                UserNotification.notifyFailure("Use 'dotfiles logout --force' to delete the stored login, then login again with 'dotfiles login'");

                evaluateExitCode(ExitCode.SOFTWARE);
            }
        }

        try {
            store.validateCredentials();

            UserNotification.notify("Local credentials are valid");

        } catch (MissingCredentialsException e) {
            UserNotification.notifyFailure(e.getMessage());

            evaluateExitCode(ExitCode.USAGE);

        } catch (SyncException e) {
            notifyAndLogFailureWithException(e.getMessage(), e);

            evaluateExitCode(ExitCode.SOFTWARE);
        }

        return worstCheckCode;
    }

    private @NotNull String buildErrorsString(@NotNull List<String> errors) {
        String lineSeparator = System.lineSeparator();

        String errorsString = String.join(lineSeparator, errors);

        return interpolate("{}{}{}", "Failed to verify login status", lineSeparator, errorsString);
    }

    private void evaluateExitCode(int newExitCode) {
        if (worstCheckCode == ExitCode.SOFTWARE) {
            return;
        }

        if (worstCheckCode == ExitCode.USAGE && newExitCode == ExitCode.OK) {
            return;
        }

        worstCheckCode = newExitCode;
    }
}

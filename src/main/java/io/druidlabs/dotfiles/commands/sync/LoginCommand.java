package io.druidlabs.dotfiles.commands.sync;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

import io.druidlabs.dotfiles.commands.CommandAction;
import io.druidlabs.dotfiles.sync.CredentialImportResult;
import io.druidlabs.dotfiles.sync.RemoteStore;
import io.druidlabs.dotfiles.sync.SyncWorker;
import io.druidlabs.dotfiles.sync.exceptions.CredentialImportException;
import io.druidlabs.dotfiles.sync.exceptions.MissingCredentialsException;
import io.druidlabs.dotfiles.sync.exceptions.MissingSyncProviderException;
import io.druidlabs.dotfiles.sync.exceptions.SyncException;
import io.druidlabs.dotfiles.util.UserNotification;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;

/**
 * Command to log in to the sync provider set in the config, storing a credential for later sync commands.
 * Does nothing if a credential is already stored. With {@code --credentials}, it first copies the client credentials
 * file the user downloaded into place, refusing while a login is stored, and refusing to replace a different file
 * unless {@code --force} is passed. Exits with {@code USAGE} for anything the user has to fix, such as no provider
 * set, missing client credentials or a file that can't be imported.
 */
@Command(name = "login", description = "Authorise this app to the set sync provider")
public final class LoginCommand extends CommandAction {

    @Option(names = {"-c", "--credentials"}, paramLabel = "<path>",
            description = "Copy the client credentials file downloaded from the provider into place, then login")
    String credentialsPath;

    @Option(names = {"-f", "--force"}, description = "With --credentials, replace a different credentials file already in place")
    boolean replaceCredentials;

    @Override
    public Integer call() {
        if (replaceCredentials && credentialsPath == null) {
            UserNotification.notifyFailure("Failed to login. --force only applies with --credentials, to replace a credentials file already in place");

            return ExitCode.USAGE;
        }

        RemoteStore store;

        try {
            store = SyncWorker.getRemoteStore();

        } catch (MissingSyncProviderException e) {
            notifyAndLogFailure("Failed to login. {}", e.getMessage());

            return ExitCode.USAGE;
        }

        if (credentialsPath != null) {
            int importExitCode = importCredentials(store);

            if (importExitCode != ExitCode.OK) {
                return importExitCode;
            }
        }

        if (store.isLoggedIn()) {
            UserNotification.notify("You are already logged in");

            return ExitCode.OK;
        }

        try {
            store.login();

        } catch (SyncException e) {

            if (e instanceof MissingCredentialsException) {
                notifyAndLogFailure("Failed to login. {}", e.getMessage());

                return ExitCode.USAGE;

            } else {
                notifyAndLogFailureWithException(e.getMessage(), e);

                return ExitCode.SOFTWARE;
            }
        }

        notifyAndLog("Successfully logged in to {}", store.getSyncProvider().getName());

        return ExitCode.OK;
    }

    /**
     * Copies the file given with {@code --credentials} into place, refusing while a login is stored or can't be read,
     * since a stored login belongs to the client credentials it was made with.
     *
     * @param store the store to import into.
     * @return {@code OK} if the credentials are in place and the login can go ahead, otherwise the exit code to return.
     */
    private int importCredentials(@NotNull RemoteStore store) {
        if (store.isLoggedIn()) {
            notifyAndLogFailure("Failed to import credentials, you are logged in. A stored login belongs to the credentials it was made with, so use 'dotfiles logout' first");

            return ExitCode.USAGE;
        }

        List<String> unloggedErrors = store.drainUnloggedErrors();

        if (!unloggedErrors.isEmpty()) {
            String lineSeparator = System.lineSeparator();

            String unloggedErrorsString = String.join(lineSeparator, unloggedErrors);

            notifyAndLogFailure("Failed to import credentials, the stored login could not be read{}{}", lineSeparator,
                    unloggedErrorsString);

            UserNotification.notifyFailure("Use 'dotfiles logout --force' to delete it, then try again");

            return ExitCode.SOFTWARE;
        }

        CredentialImportResult result;

        try {
            String sanitisedPath = sanitise(credentialsPath);

            result = store.importCredentials(Path.of(sanitisedPath), replaceCredentials);

        } catch (CredentialImportException e) {
            notifyAndLogFailure("Failed to import credentials. {}", e.getMessage());

            return ExitCode.USAGE;

        } catch (SyncException e) {
            notifyAndLogFailureWithException(e.getMessage(), e);

            return ExitCode.SOFTWARE;
        }

        switch (result) {
            case REFUSED -> {
                notifyAndLogFailure("Failed to import credentials, a different credentials file is already in place");

                UserNotification.notify("Replacing it can't be undone, pass --force to replace it");

                return ExitCode.USAGE;
            }
            case EXISTS -> UserNotification.notify("These credentials are already in place");
            case COPIED -> notifyAndLog("Copied the credentials file into place");
            case REPLACED -> notifyAndLog("Replaced the credentials file in place");
        }

        UserNotification.notify("You can delete the downloaded file now, it holds your client secret");

        return ExitCode.OK;
    }
}

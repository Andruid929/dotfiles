package io.druidlabs.dotfiles.sync;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import io.druidlabs.dotfiles.Dotfile;
import io.druidlabs.dotfiles.Worker;
import io.druidlabs.dotfiles.config.ConfigOption;
import io.druidlabs.dotfiles.config.Configuration;
import io.druidlabs.dotfiles.sync.exceptions.MissingSyncProviderException;
import io.druidlabs.dotfiles.sync.gdrive.GoogleDriveRemoteStore;
import io.druidlabs.dotfiles.transformer.HashHandler;

/**
 * Picks the {@link RemoteStore} for the sync provider set in the config, and takes the snapshots sync compares
 * against. Unlike {@link Worker}, this holds no state: each call builds a new store.
 */
public final class SyncWorker {

    private SyncWorker() {
        //Utility classes cannot be instantiated
    }

    /**
     * Builds the store for the provider in {@link ConfigOption#SYNC_PROVIDER}. Nothing is read from disk or the
     * network until a method of the store needs it.
     *
     * @return a new store for the configured provider.
     * @throws MissingSyncProviderException if no provider is set.
     * @throws IllegalStateException        if the configured provider is unknown, which loading the config should
     *                                      have prevented.
     */
    public static @NotNull RemoteStore getRemoteStore() throws MissingSyncProviderException, IllegalStateException {
        Configuration configuration = Worker.getConfiguration();

        String syncProviderId = configuration.getConfig(ConfigOption.SYNC_PROVIDER);

        if (syncProviderId.equalsIgnoreCase(Configuration.EMPTY_CONFIG_VALUE)) {
            throw new MissingSyncProviderException("No sync provider set. Use 'dotfiles config --set SyncProvider:GDrive' to use Google drive");
        }

        SyncProvider provider = SyncProvider.fromString(syncProviderId);

        if (provider == null) {
            String message = interpolate("Unsupported sync provider: {}", syncProviderId);

            throw new IllegalStateException(message);
        }

        return switch (provider) {
            case GOOGLE_DRIVE -> new GoogleDriveRemoteStore();
        };
    }

    /**
     * Takes a snapshot of the dotfiles as they are, to compare with the saved one. It has no undo record: the snapshot
     * saved as {@code .sync} after a push or pull adds one, since only then is it known what was replaced.
     *
     * @param dotfiles the dotfiles, local or parsed from the remote file.
     * @param store    the store being synced with, whose provider the snapshot records.
     * @return a new snapshot of the dotfiles, with no undo record.
     * @throws JsonProcessingException if the dotfiles could not be serialised.
     */
    @Contract("_,_ -> new")
    public static @NotNull SyncSnapshot computeSnapshot(@NotNull Collection<Dotfile> dotfiles, @NotNull RemoteStore store)
            throws JsonProcessingException {
        String dotfilesHash = HashHandler.hashDotfiles(dotfiles);

        SyncProvider syncProvider = store.getSyncProvider();

        return new SyncSnapshot(syncProvider.getId(), dotfilesHash, null);
    }
}

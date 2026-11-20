package io.druidlabs.dotfiles.sync;

/**
 * What the sync snapshot file ({@code .sync}) stores: the state of the dotfiles at the last successful sync, which
 * push and pull compare both sides against to tell which one changed.
 *
 * @param providerId the ID of the sync provider the snapshot was taken with. A snapshot from another provider doesn't
 *                   count, since it describes a different remote file.
 * @param hash       the dotfiles' hash, from {@link io.druidlabs.dotfiles.transformer.HashHandler#hashDotfiles}.
 */
public record SyncSnapshot(String providerId, String hash) {
}

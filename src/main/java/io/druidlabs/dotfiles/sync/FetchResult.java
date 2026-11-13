package io.druidlabs.dotfiles.sync;

/**
 * The remote save file as returned by {@link RemoteStore#fetch()}.
 *
 * @param tag     an opaque version tag set by the remote. Only ever compared for equality, never parsed.
 * @param content the full contents of the save file.
 */
public record FetchResult(String tag, String content) {
}

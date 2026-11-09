package io.druidlabs.dotfiles.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Matcher;

import io.github.andruid929.leutils.wora.PathFinder;

/**
 * Utility class for path manipulation.
 * Includes methods for expanding/shortening home directory references (~) and handling file separators.
 */
public final class PathUtil {

    /**
     * The home folder, which {@code ~} stands for and which holds any file given without a folder.
     */
    private static final String DEFAULT_DIR = PathFinder.USER_HOME;

    private PathUtil() {
    }

    /**
     * Splits a path into its folder and filename. A path with no folder gets the home folder.
     *
     * @param path the path to split.
     * @return a two-element array: the folder, then the filename.
     * @throws IllegalArgumentException if {@code path} has no filename, such as a root.
     */
    @Contract("_ -> new")
    public static String @NotNull [] getPathAndFile(@NotNull Path path) {
        Path filepath = path.getParent();
        Path filename = path.getFileName();

        if (filename == null) {
            throw new IllegalArgumentException("File is required");
        }

        if (filepath == null) {
            return new String[]{DEFAULT_DIR, filename.toString()};
        }

        return new String[]{filepath.toString(), filename.toString()};
    }

    /**
     * Replaces a leading home folder with {@code ~}, using {@code /} for the rest of the path.
     *
     * @param pathString the path to shorten.
     * @return the shortened path, {@code ~} for a blank path, or {@code pathString} unchanged if it is not in the
     * home folder.
     */
    public static @NotNull String shortenPath(@NotNull String pathString) {
        if (pathString.isBlank()) {
            return "~";
        }

        if (!pathString.startsWith(DEFAULT_DIR)) {
            return pathString;
        }

        Path path = Path.of(pathString).normalize();

        Path relative = Path.of(DEFAULT_DIR).relativize(path);

        return "~/" + relative.toString().replace("\\", "/");

    }

    /**
     * Replaces a leading {@code ~/} with the home folder.
     *
     * @param pathString the path to expand.
     * @return the expanded path, the home folder for a blank path or {@code ~} alone, or {@code pathString}
     * unchanged if it does not start with {@code ~/}.
     */
    public static @NotNull String expandPath(@NotNull String pathString) {
        if (pathString.isBlank() || pathString.equals("~")) {
            return PathFinder.USER_HOME;
        }

        if (pathString.startsWith("~/")) {
            String fromUserHome = pathString.substring(2); //Remove the tilde and first slash

            Path expandedPath = PathFinder.createPathFromHomeRoot(fromUserHome).normalize();

            return handleSeparators(expandedPath.toString());
        }

        return pathString;
    }

    /**
     * @param pathString the path to fix.
     * @return the path with every run of {@code /} or {@code \} replaced by one OS separator.
     */
    @NotNull
    public static String handleSeparators(@NotNull String pathString) {
        return pathString.replaceAll("[/\\\\]+", Matcher.quoteReplacement(File.separator));
    }
}

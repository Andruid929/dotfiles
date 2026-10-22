package io.druidlabs.dotfiles.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Matcher;

import io.druidlabs.dotfiles.annotations.Blankable;
import io.github.andruid929.leutils.wora.PathFinder;

/**
 * Utility class for path manipulation.
 * Includes methods for expanding/shortening home directory references (~) and handling file separators.
 */
public final class PathUtil {

    private static final String DEFAULT_DIR = PathFinder.USER_HOME;

    private PathUtil() {
    }

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

    @Blankable
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

    @Blankable
    public static @NotNull String expandPath(@NotNull String pathString) {
        if (pathString.isBlank() || pathString.equals("~")) {
            return PathFinder.USER_HOME;
        }

        if (pathString.startsWith("~".concat(File.separator))) {
            String fromUserHome = pathString.substring(2); //Remove the tilde and first slash

            Path expandedPath = PathFinder.createPathFromHomeRoot(fromUserHome).normalize();

            return handleSeparators(expandedPath.toString());
        }

        return pathString;
    }

    @NotNull
    public static String handleSeparators(@NotNull String pathString) {
        return pathString.replaceAll("[/\\\\]+", Matcher.quoteReplacement(File.separator));
    }
}

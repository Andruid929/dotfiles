package io.druidlabs.dotfiles.util;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;

/**
 * Utility class for turning filesystem-related exceptions into user-facing messages.
 */
public final class ExceptionUtil {

    private ExceptionUtil() {
        //Utility classes cannot be instantiated
    }

    @Contract(pure = true)
    public static @NotNull String getFileSystemExceptionReason(@NotNull FileSystemException e) {
        return switch (e) {
            case AccessDeniedException _ -> "Access has been denied";
            case DirectoryNotEmptyException _ -> "Folder is not empty";
            case FileAlreadyExistsException _ -> "File already exists";
            case NoSuchFileException _ -> "File does not exist";
            case NotDirectoryException _ -> "The program expected a folder";
            default -> "Filesystem error";
        };
    }

    /**
     * Builds a user-facing message for an {@link IOException}, describing the reason when it is a
     * {@link FileSystemException} and falling back otherwise.
     *
     * @param e        the exception to describe.
     * @param template message template with a single {@code {}} placeholder for the lowercased reason,
     *                  used when {@code e} is a {@link FileSystemException}.
     * @param fallback message used when {@code e} is not a {@link FileSystemException}.
     * @return the resulting message.
     */
    @Contract(pure = true)
    public static @NotNull String describeIOFailure(@NotNull IOException e, @NotNull String template, @NotNull String fallback) {
        if (e instanceof FileSystemException fse) {
            return interpolate(template, getFileSystemExceptionReason(fse).toLowerCase());
        }

        return fallback;
    }
}

package io.druidlabs.dotfiles.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;

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
}

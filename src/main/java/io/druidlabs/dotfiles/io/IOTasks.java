package io.druidlabs.dotfiles.io;

import static io.druidlabs.dotfiles.constants.FilePaths.*;

import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.druidlabs.dotfiles.util.LoggerUtil;

/**
 * Utility class for general input/output operations.
 * Handles creation and deletion of application folders and files.
 */
public final class IOTasks {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(IOTasks.class);

    private IOTasks() {
        //Utility classes cannot be instantiated
    }

    public static void deleteFile(@NotNull Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            throw new NoSuchFileException(filePath.toString());
        }

        if (filePath.equals(LOG_FILE)) {
            LoggerUtil.detachFileAppender();
        }

        Files.delete(filePath);
    }

    public static void deleteFolder(@NotNull Path folderPath) throws IOException {
        if (Files.notExists(folderPath)) {
            throw new NoSuchFileException(folderPath.toString());
        }

        if (folderPath.equals(APP_FOLDER_DIR)) {
            LoggerUtil.detachFileAppender();
        }

        try (Stream<Path> dirStream = Files.walk(folderPath)) {
            List<Path> dirContents = dirStream.sorted(Comparator.reverseOrder()).toList();

            for (Path path : dirContents) {
                Files.delete(path);
            }
        }
    }

    private static void createFileIfNotExists(Path path) throws IOException {
        if (Files.notExists(path)) {

            Files.createFile(path);
        }
    }

    public static void validateAppPaths() throws IOException {
        if (Files.notExists(APP_FOLDER_DIR)) {
            Files.createDirectories(APP_FOLDER_DIR);
        }

        createFileIfNotExists(SAVE_FILE);
        createFileIfNotExists(LOG_FILE);
    }

    public static void createSyncAuthFolder(@NotNull Path path) throws IOException {
        if (Files.exists(path)) {
            restrictFolderToOwner(path);

            return;
        }

        Files.createDirectories(path);

        restrictFolderToOwner(path);
    }

    private static void restrictFolderToOwner(@NotNull Path path) throws IOException {
        if (Files.notExists(path)) {
            throw new NoSuchFileException("Cannot set permissions for a non-existent folder");
        }

        FileStore fileStore = Files.getFileStore(path);

        if (fileStore.supportsFileAttributeView(PosixFileAttributeView.class)) { // Linux/Mac
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");

            Files.setPosixFilePermissions(path, permissions);

        } else { //Windows
            AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);

            if (view == null) {
                var failureCause = new UnsupportedOperationException("File system has no ACL support");

                throw new IOException(failureCause);
            }

            FileSystem winFileSystem = FileSystems.getDefault();

            UserPrincipalLookupService lookupService = winFileSystem.getUserPrincipalLookupService();

            String username = System.getProperty("user.name");

            UserPrincipal principal = lookupService.lookupPrincipalByName(username);

            AclEntry aclEntry = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(principal)
                    .setPermissions(AclEntryPermission.values())
                    .setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT)
                    .build();

            view.setAcl(List.of(aclEntry));
        }
    }


}

package io.druidlabs.dotfiles.io;

import static io.druidlabs.dotfiles.constants.FilePaths.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.druidlabs.dotfiles.util.LoggerUtil;

/**
 * Utility class for general input/output operations.
 * Handles creation and deletion of application folders and files.
 */
public final class IOTasks {

    private IOTasks() {
        //Utility classes cannot be instantiated
    }

    /**
     * Deletes a file. For the log file, logback's file appender is detached first so the file is not held open.
     *
     * @param filePath the file to delete.
     * @throws NoSuchFileException if the file does not exist.
     * @throws IOException         if the file could not be deleted.
     */
    public static void deleteFile(@NotNull Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            throw new NoSuchFileException(filePath.toString());
        }

        if (filePath.equals(LOG_FILE)) {
            LoggerUtil.detachFileAppender();
        }

        Files.delete(filePath);
    }

    /**
     * Deletes a folder and everything in it, deepest first. For the app folder, logback's file appender is detached
     * first so the log file is not held open. If a deletion fails, whatever was already deleted stays deleted.
     *
     * @param folderPath the folder to delete.
     * @throws NoSuchFileException if the folder does not exist.
     * @throws IOException         if anything in it could not be deleted.
     */
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

    /**
     * Creates an empty file unless it already exists. The folder must already exist.
     */
    private static void createFileIfNotExists(Path path) throws IOException {
        if (Files.notExists(path)) {

            Files.createFile(path);
        }
    }

    /**
     * Creates the app folder and its save, log and config files if any are missing. The sync folder is left out on
     * purpose, so a sync problem can never stop ordinary commands.
     *
     * @throws IOException if anything could not be created.
     */
    public static void validateAppPaths() throws IOException {
        if (Files.notExists(APP_FOLDER_DIR)) {
            Files.createDirectories(APP_FOLDER_DIR);
        }

        createFileIfNotExists(SAVE_FILE);
        createFileIfNotExists(LOG_FILE);
        createFileIfNotExists(CONFIG_FILE);
    }

    /**
     * Creates a folder for sync credentials if needed, then restricts it to the current user. Only the folder
     * itself is restricted, any parent folders created along the way get the default permissions.
     *
     * @param path the folder to create.
     * @throws IOException if the folder could not be created or restricted.
     */
    public static void createSyncAuthFolder(@NotNull Path path) throws IOException {
        if (Files.exists(path)) {
            restrictFolderToOwner(path);

            return;
        }

        Files.createDirectories(path);

        restrictFolderToOwner(path);
    }

    /**
     * Restricts a folder to the current user. On file systems with POSIX permissions it is set to
     * {@code rwx------}. Otherwise, as on Windows, its ACL is replaced with a single full-control entry for the
     * current user that everything inside inherits.
     *
     * @param path the folder to restrict.
     * @throws NoSuchFileException if the folder does not exist.
     * @throws IOException         if the permissions could not be set, or the file system supports neither POSIX
     *                             permissions nor ACLs.
     */
    public static void restrictFolderToOwner(@NotNull Path path) throws IOException {
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

    /**
     * @param path the file being saved.
     * @return the temporary file {@link #atomicSave(Path, String)} writes before moving it over {@code path}: the
     * same name plus {@code .tmp}, beside it. A crash between the write and the move leaves it behind.
     */
    public static @NotNull Path tempFileFor(@NotNull Path path) {
        return path.resolveSibling(path.getFileName() + ".tmp");
    }

    public static void atomicSave(@NotNull Path path, @NotNull String content) throws IOException {
        //The move replaces whatever is at the path, so a symlink would become a regular file. Save to the file it
        //points to instead, with the temp file beside that. A link tp nothing throws NoSuchFileException
        if (Files.isSymbolicLink(path)) {
            path = path.toRealPath();
        }

        Path tempFile = tempFileFor(path);

        StandardOpenOption[] openOptions = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        Set<PosixFilePermission> targetPermissions = posixPermissionsOf(path);

        try {

            try (FileChannel writeChannel = FileChannel.open(tempFile, openOptions)) {
                //The temp file is created with the default permissions, so without this every save would undo a
                //chmod of the target. Set before writing, so the contents are never open to more than before. Only
                //when they differ, since file systems such as FAT on Linux refuse any change
                if (targetPermissions != null && !targetPermissions.equals(Files.getPosixFilePermissions(tempFile))) {
                    Files.setPosixFilePermissions(tempFile, targetPermissions);
                }

                ByteBuffer buffer = ByteBuffer.wrap(contentBytes);

                while (buffer.hasRemaining()) {
                    int _ = writeChannel.write(buffer);
                }

                writeChannel.force(true);
            }

            Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(tempFile);

            } catch (IOException deleteFailure) {
                e.addSuppressed(deleteFailure);
            }

            throw e;
        }
    }

    /**
     * @param path the file to check.
     * @return the file's POSIX permissions, or {@code null} if it doesn't exist or its file system has none, as on
     * Windows.
     * @throws IOException if the permissions could not be read.
     */
    private static @Nullable Set<PosixFilePermission> posixPermissionsOf(@NotNull Path path) throws IOException {
        if (Files.getFileAttributeView(path, PosixFileAttributeView.class) == null) {
            return null;
        }

        try {
            return Files.getPosixFilePermissions(path);

        } catch (NoSuchFileException e) {
            return null;
        }
    }
}

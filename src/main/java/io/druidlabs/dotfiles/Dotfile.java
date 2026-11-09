package io.druidlabs.dotfiles;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import io.druidlabs.dotfiles.exceptions.BlankValueException;
import io.druidlabs.dotfiles.util.PathUtil;

/**
 * Represents a managed configuration file (dotfile).
 * Stores the file's content, its original path, and a unique key for identification.
 */
public final class Dotfile {

    /**
     * Always lower-case, and the only field used for equality.
     */
    @NotNull
    private String key;

    /**
     * The saved text of the file.
     */
    @NotNull
    private String fileContents;

    /**
     * The file's folder and name. The constructor shortens the home folder to {@code ~}, and
     * {@link #getFilePath()} expands it again.
     */
    @NotNull
    private Path pathToFile;

    /**
     * Creates a new Dotfile entry.
     *
     * @param key          a unique identifier for the dotfile.
     * @param fileContents the text content of the file.
     * @param filePath     the directory path where the file is located.
     * @param filename     the name of the file.
     * @throws BlankValueException if key or fileContents are blank.
     */
    @JsonCreator
    public Dotfile(
            @NotNull @JsonProperty("key") String key,
            @NotNull @JsonProperty("fileContents") String fileContents,
            @NotNull @JsonProperty("filePath") String filePath,
            @NotNull @JsonProperty("filename") String filename) {
        checkForBlanks(key, fileContents);

        this.key = key.toLowerCase(Locale.ROOT);
        this.fileContents = fileContents;

        String shortenedPath = PathUtil.shortenPath(filePath); //Shorten the path if necessary

        this.pathToFile = Path.of(shortenedPath, filename);
    }

    /**
     * @throws BlankValueException if {@code key} or {@code fileContents} is blank.
     */
    private void checkForBlanks(@NotNull String key, String fileContents) {
        if (key.isBlank()) {
            throw new BlankValueException("Key is required");
        }

        if (fileContents.isBlank()) {
            throw new BlankValueException("File contents are required");
        }
    }

    /**
     * @return the full {@link Path} to the file.
     */
    @JsonIgnore
    public @NotNull Path getPathToFile() {
        return Path.of(getFilePath(), getFilename());
    }

    /**
     * @return the key, always lower-case.
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Sets the key, lower-casing it. Unlike the constructor, a blank key is not rejected.
     *
     * @param key the new key.
     */
    public void setKey(@NotNull String key) {
        this.key = key.toLowerCase(Locale.ROOT);
    }

    /**
     * @return the file's name, without its folder.
     */
    @NotNull
    public String getFilename() {
        Path filename = Objects.requireNonNull(pathToFile.getFileName());

        return filename.toString();
    }

    /**
     * Renames the file, keeping its folder.
     *
     * @param filename the new file name, without a folder.
     * @throws IllegalArgumentException if {@code filename} is blank.
     */
    public void setFilename(@NotNull String filename) {
        if (filename.isBlank()) {
            throw new IllegalArgumentException("DotFILEs needs at least a FILEname");
        }

        Path path = pathToFile.getParent();

        if (path == null) {
            pathToFile = Path.of(filename);

        } else {
            pathToFile = path.resolve(filename);
        }
    }

    /**
     * @return the saved text of the file.
     */
    @NotNull
    public String getFileContents() {
        return fileContents;
    }

    /**
     * Replaces the saved text of the file. Unlike the constructor, blank contents are not rejected.
     *
     * @param fileContents the new contents.
     */
    public void setFileContents(@NotNull String fileContents) {
        this.fileContents = fileContents;
    }

    /**
     * @return the folder holding the file, with a leading {@code ~} expanded to the home folder.
     */
    @NotNull
    public String getFilePath() {
        Path filepath = Objects.requireNonNull(pathToFile.getParent());

        return PathUtil.expandPath(filepath.toString());
    }

    /**
     * Moves the file to another folder, keeping its name. The path is stored as given, without shortening the
     * home folder to {@code ~}.
     *
     * @param filePath the new folder, or a blank string for the home folder.
     */
    public void setFilePath(@NotNull String filePath) {
        Path filename = pathToFile.getFileName();

        if (filePath.isBlank()) {
            pathToFile = Path.of("~", filename.toString());

        } else {
            pathToFile = Path.of(filePath).resolve(filename);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Dotfile dotFile = (Dotfile) o;
        return Objects.equals(key, dotFile.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return interpolate("Dotfile{key=\"{}\", filepath=\"{}\", content=\"{}\"}", key, pathToFile, fileContents);
    }
}

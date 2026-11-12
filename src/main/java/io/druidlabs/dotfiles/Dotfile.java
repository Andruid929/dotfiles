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
     * The file's folder in short form: the home folder is written as {@code ~}, and separators are always {@code /}.
     * {@link #getFilePath()} expands it for use on disk.
     */
    @NotNull
    private String shortFilePath;

    /**
     * The file's name, without its folder.
     */
    @NotNull
    private String filename;

    /**
     * Creates a new Dotfile entry.
     *
     * @param key          a unique identifier for the dotfile.
     * @param fileContents the text content of the file.
     * @param filePath     the directory path where the file is located.
     * @param filename     the name of the file.
     * @throws BlankValueException if key, fileContents or filename are blank.
     */
    @JsonCreator
    public Dotfile(
            @NotNull @JsonProperty("key") String key,
            @NotNull @JsonProperty("fileContents") String fileContents,
            @NotNull @JsonProperty("filePath") String filePath,
            @NotNull @JsonProperty("filename") String filename) {
        checkForBlanks(key, fileContents);

        if (filename.isBlank()) {
            throw new BlankValueException("Filename cannot be blank");
        }

        this.key = key.toLowerCase(Locale.ROOT);
        this.fileContents = fileContents;
        this.shortFilePath = PathUtil.shortenPath(filePath); //Shorten the path if necessary
        this.filename = filename;
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
     * Sets the key, lower-casing it. A blank key is rejected, as in the constructor, since a saved dotfile with
     * one could not be loaded again.
     *
     * @param key the new key.
     * @throws BlankValueException if {@code key} is blank. The key is left unchanged.
     */
    public void setKey(@NotNull String key) {
        if (key.isBlank()) {
            throw new BlankValueException("Dotfile key cannot be blank");
        }

        this.key = key.toLowerCase(Locale.ROOT);
    }

    /**
     * @return the file's name, without its folder.
     */
    @NotNull
    public String getFilename() {
        return filename;
    }

    /**
     * Renames the file, keeping its folder.
     *
     * @param filename the new file name, without a folder.
     * @throws BlankValueException if {@code filename} is blank.
     */
    public void setFilename(@NotNull String filename) {
        if (filename.isBlank()) {
            throw new BlankValueException("DotFILEs needs at least a FILEname");
        }

        this.filename = filename;
    }

    /**
     * @return the saved text of the file.
     */
    @NotNull
    public String getFileContents() {
        return fileContents;
    }

    /**
     * Replaces the saved text of the file. Blank contents are rejected, as in the constructor, since a saved
     * dotfile with them could not be loaded again.
     *
     * @param fileContents the new contents.
     * @throws BlankValueException if {@code fileContents} is blank. The contents are left unchanged.
     */
    public void setFileContents(@NotNull String fileContents) {
        if (fileContents.isBlank()) {
            throw new BlankValueException("Dotfile's contents cannot be blank");
        }

        this.fileContents = fileContents;
    }

    /**
     * @return the folder holding the file, with a leading {@code ~} expanded to the home folder.
     */
    @NotNull
    @JsonIgnore
    public String getFilePath() {
        return PathUtil.expandPath(shortFilePath);
    }

    /**
     * @return the folder holding the file in short form, with the home folder as {@code ~} and {@code /} as the
     * separator. This is the form saved to the JSON.
     */
    @JsonProperty("filePath")
    public @NotNull String getShortFilePath() {
        return shortFilePath;
    }

    /**
     * Moves the file to another folder, keeping its name. The folder is stored shortened, with the home folder as
     * {@code ~}.
     *
     * @param filePath the new folder, or a blank string for the home folder.
     */
    public void setFilePath(@NotNull String filePath) {
        this.shortFilePath = PathUtil.shortenPath(filePath);
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
        return interpolate("Dotfile{key=\"{}\", filepath=\"{}/{}\", content=\"{}\"}", key, shortFilePath, filename,
                fileContents);
    }
}

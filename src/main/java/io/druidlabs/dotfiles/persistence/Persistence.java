package io.druidlabs.dotfiles.persistence;

import java.io.IOException;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

/**
 * Loads and saves the full set of dotfiles to and from local storage.
 */
public interface Persistence {

    /**
     * Loads all previously saved dotfiles.
     *
     * @return the saved dotfiles, or an empty set if there is nothing saved yet.
     * @throws IOException if the save file could not be read or parsed.
     */
    Set<Dotfile> load() throws IOException;

    /**
     * Saves the given dotfiles, replacing whatever was previously saved.
     *
     * @param setOfDotfiles the dotfiles to save.
     * @throws IOException if the save file could not be written.
     */
    void save(Set<Dotfile> setOfDotfiles) throws IOException;

}

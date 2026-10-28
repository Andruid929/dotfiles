package io.druidlabs.dotfiles.persistence;

import java.io.IOException;
import java.util.Set;

import io.druidlabs.dotfiles.Dotfile;

public interface Persistence {

    Set<Dotfile> load() throws IOException;

    void save(Set<Dotfile> setOfDotfiles) throws IOException;

}

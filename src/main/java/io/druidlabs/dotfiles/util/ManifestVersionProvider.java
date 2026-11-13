package io.druidlabs.dotfiles.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import io.druidlabs.dotfiles.Main;
import picocli.CommandLine.IVersionProvider;

/**
 * Supplies the version picocli prints for {@code --version}.
 * <p>
 * The value is the {@code Implementation-Version} attribute that the shade plugin writes into the jar's manifest,
 * so the pom is the only place the version needs to be bumped.
 */
public final class ManifestVersionProvider implements IVersionProvider {

    /**
     * The version reported when there is no manifest to read.
     */
    static final String FALLBACK_VERSION = "dev";

    /**
     * Reads the version from the manifest of the jar {@link Main} was loaded from.
     *
     * @return the manifest version, or {@value #FALLBACK_VERSION} when not running from a jar (IDE, tests), where
     * there is no manifest to read.
     */
    static String version() {
        String version = Main.class.getPackage().getImplementationVersion();

        return version == null ? FALLBACK_VERSION : version;
    }

    @Contract(" -> new")
    @Override
    public String @NotNull [] getVersion() {
        return new String[]{version()};
    }
}

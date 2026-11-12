package io.druidlabs.dotfiles.util;

/**
 * Tells which operating system the app is running on, for behaviour that differs between them.
 */
public final class OSUtil {

    private OSUtil() {
        //Utility classes cannot be instantiated
    }

    /**
     * Works out the operating system from the {@code os.name} system property. Anything that isn't Windows or macOS
     * is treated as Linux.
     *
     * @return the operating system the app is running on.
     */
    public static OperatingSystem getCurrentOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;

        } else if (osName.contains("mac")) {
            return OperatingSystem.MAC_OS;

        } else return OperatingSystem.LINUX;

    }

    /**
     * The operating systems the app tells apart. {@link #LINUX} also stands for any other Unix-like system.
     */
    public enum OperatingSystem {
        WINDOWS, LINUX, MAC_OS;
    }

}

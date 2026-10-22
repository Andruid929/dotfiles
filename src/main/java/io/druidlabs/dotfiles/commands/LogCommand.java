package io.druidlabs.dotfiles.commands;

import static io.github.andruid929.leutils.stringutil.StringFormatter.interpolate;

import io.druidlabs.dotfiles.defaults.FilePaths;
import io.druidlabs.dotfiles.io.PathReader;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Command to locate and optionally read the application log file.
 */
@Command(name = "logs", description = "Locate the log file")
public class LogCommand extends CommandAction {

    @Option(names = {"-r", "--read"}, description = "Display the location and then read it")
    boolean readLogs = false;

    @Override
    public void run() {
        System.out.println(interpolate("You can find the logs at '{}'\n", FilePaths.LOG_FILE));

        if (readLogs) {
            String logContents = PathReader.readLogs();

            if (logContents.isBlank()) {
                System.out.println("Wow, such empty");

                return;
            }

            System.out.println(logContents);

            LOGGER.debug("Displayed logs to user");
        }
    }
}

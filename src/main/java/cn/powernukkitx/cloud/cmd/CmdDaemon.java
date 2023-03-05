package cn.powernukkitx.cloud.cmd;

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Singleton
public class CmdDaemon extends Thread {
    private static final Logger log = LoggerFactory.getLogger("");
    private final Scanner scanner = new Scanner(System.in);
    protected static final Map<String, Command> commands = new HashMap<>();

    public static void initCommands(@NotNull List<Command> commands) {
        for (var command : commands) {
            CmdDaemon.commands.put(command.getName(), command);
            for (var alias : command.getAliases()) {
                CmdDaemon.commands.put(alias, command);
            }
        }
    }


    public CmdDaemon(ExitCommand exitCommand,
                     HelpCommand helpCommand,
                     SeeDownloadIDsCommand seeDownloadIDsCommand,
                     FileInfoCacheCommand fileInfoCacheCommand,
                     PluginRepoCommand pluginRepoCommand,
                     GCCommand gcCommand) {
        super("Command Daemon");
        this.setDaemon(true);
        if (commands.isEmpty()) {
            initCommands(List.of(
                    exitCommand,
                    helpCommand,
                    seeDownloadIDsCommand,
                    fileInfoCacheCommand,
                    pluginRepoCommand,
                    gcCommand
            ));
        }
    }

    @Override
    public void run() {
        while (!interrupted()) {
            try {
                var line = scanner.nextLine();
                if (line.isBlank()) {
                    continue;
                }
                var args = line.split("\\s+");
                var command = commands.get(args[0]);
                var commandArgs = new String[args.length - 1];
                if (commandArgs.length > 0) {
                    System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);
                }
                if (command != null) {
                    var result = command.execute(LoggerFactory.getLogger(command.getName()), commandArgs);
                    if (!result) {
                        log.error("Failed to execute command {} with args {}", command.getName(), Arrays.toString(commandArgs));
                    }
                } else {
                    log.warn("Unknown command {}", args[0]);
                }
            } catch (Exception e) {
                log.error("Failed to execute command", e);
            }
        }
    }
}

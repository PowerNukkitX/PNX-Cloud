package cn.powernukkitx.cloud.cmd;

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

@Singleton
public class HelpCommand implements Command {
    @Override
    public @NotNull String getName() {
        return "help";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        var map = new TreeMap<Command, List<String>>(Comparator.comparing(Command::getName));
        for (var command : CmdDaemon.commands.values()) {
            var list = new ArrayList<String>(command.getAliases().length + 1);
            list.add(command.getName());
            list.addAll(Arrays.asList(command.getAliases()));
            map.put(command, list);
        }
        for (var entry : map.entrySet()) {
            var aliases = entry.getValue();
            log.info("{}", String.join(", ", aliases));
        }
        return true;
    }
}

package cn.powernukkitx.cloud.cmd;

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
public class ExitCommand implements Command {
    @Override
    public @NotNull String getName() {
        return "exit";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        log.info("Shutting down...");
        System.exit(0);
        return true;
    }

    @Override
    public @NotNull String @NotNull [] getAliases() {
        return new String[]{"quit", "stop"};
    }
}

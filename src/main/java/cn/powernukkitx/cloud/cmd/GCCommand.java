package cn.powernukkitx.cloud.cmd;

import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
public class GCCommand implements Command {
    @Override
    public @NotNull String getName() {
        return "gc";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        System.gc();
        log.info("GC finished");
        var totalMemory = Runtime.getRuntime().totalMemory();
        var freeMemory = Runtime.getRuntime().freeMemory();
        log.info("Memory usage: {}% ({} MB/ {} MB)", String.format("%.2f", (totalMemory - freeMemory) * 100.0 / totalMemory), (totalMemory - freeMemory) / 1024 / 1024, totalMemory / 1024 / 1024);
        return true;
    }
}

package cn.powernukkitx.cloud.cmd;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public interface Command {
    @NotNull String getName();

    boolean execute(@NotNull Logger log, @NotNull String... args);

    default @NotNull String @NotNull [] getAliases() {
        return new String[0];
    }
}

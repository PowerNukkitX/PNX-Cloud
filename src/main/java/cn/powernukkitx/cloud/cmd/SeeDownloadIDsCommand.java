package cn.powernukkitx.cloud.cmd;

import cn.powernukkitx.cloud.helper.DBHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
public class SeeDownloadIDsCommand implements Command {
    @Inject
    private DBHelper dbHelper;

    @Override
    public @NotNull String getName() {
        return "seeDownloadIDs";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        var cursor = dbHelper.getDownloadIDBeanObjectRepository().find();
        for (var each : cursor) {
            log.info("{}: {}", each.getId(), each.getUrl());
        }
        return true;
    }
}

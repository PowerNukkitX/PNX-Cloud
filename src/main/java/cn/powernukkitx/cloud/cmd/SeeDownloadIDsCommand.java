package cn.powernukkitx.cloud.cmd;

import cn.powernukkitx.cloud.helper.DBHelper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.dizitart.no2.objects.filters.ObjectFilters;
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
        if (args.length == 2 && args[0].equals("clearCache")) {
            long id;
            try {
                id = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                log.error("Invalid ID: {}", args[1]);
                return false;
            }
            var bean = dbHelper.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault();
            if (bean == null) {
                log.error("ID not found: {}", id);
                return false;
            }
            bean.setSavePath(null);
            dbHelper.getDownloadIDBeanObjectRepository().update(bean);
            log.info("Cleared cache for ID: {}", id);
            return true;
        }
        var cursor = dbHelper.getDownloadIDBeanObjectRepository().find();
        for (var each : cursor) {
            log.info("{}: {}", each.getId(), each.getUrl());
        }
        return true;
    }

    @Override
    public @NotNull String @NotNull [] getAliases() {
        return new String[]{"downloadID"};
    }
}

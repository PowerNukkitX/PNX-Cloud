package cn.powernukkitx.cloud.cmd;

import cn.powernukkitx.cloud.helper.DBHelper;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
@Slf4j
public class FileInfoCacheCommand implements Command {
    private final DBHelper dbHelper;

    public FileInfoCacheCommand(@NotNull DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public @NotNull String getName() {
        return "fileInfo";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        var db = dbHelper.getFileInfoBeanObjectRepository();
        if (args.length == 0) {
            for (var each : db.find()) {
                log.info(each.toString());
            }
        } else {
            switch (args[0]) {
                case "clear" -> {
                    db.remove(ObjectFilters.ALL);
                    log.info("Successfully cleared all.");
                }
                default -> {
                    log.error("Unknown sub-command: {}", args[0]);
                    return false;
                }
            }
        }
        return true;
    }
}

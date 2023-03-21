package cn.powernukkitx.cloud.task;

import cn.powernukkitx.cloud.helper.AsyncHelper;
import cn.powernukkitx.cloud.helper.MeiliSearchHelper;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class MeiliSearchTimedTask {
    private final MeiliSearchHelper searchHelper;

    public MeiliSearchTimedTask(@NotNull MeiliSearchHelper searchHelper) {
        this.searchHelper = searchHelper;
    }

    @Scheduled(fixedDelay = "10m", initialDelay = "11m")
    public void syncData() {
        log.info("Syncing data to MeiliSearch...");
        AsyncHelper.runIOTask(() -> {
            try {
                searchHelper.syncPluginData();
            } catch (MeilisearchException e) {
                log.warn("Failed to sync data to MeiliSearch", e);
            }
            log.info("Synced data to MeiliSearch");
        });
    }
}

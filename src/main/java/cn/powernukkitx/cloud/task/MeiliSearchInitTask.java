package cn.powernukkitx.cloud.task;

import cn.powernukkitx.cloud.helper.MeiliSearchHelper;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
public class MeiliSearchInitTask implements ApplicationEventListener<ServerStartupEvent> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MeiliSearchInitTask.class);

    private final MeiliSearchHelper searchHelper;

    public MeiliSearchInitTask(@NotNull MeiliSearchHelper searchHelper) {
        this.searchHelper = searchHelper;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        try {
            searchHelper.tryInit();
        } catch (MeilisearchException e) {
            log.warn("Failed to init MeiliSearch", e);
        }
    }
}

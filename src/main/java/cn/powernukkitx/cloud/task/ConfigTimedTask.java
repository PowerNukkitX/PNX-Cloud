package cn.powernukkitx.cloud.task;

import cn.powernukkitx.cloud.config.GHConfig;
import cn.powernukkitx.cloud.config.SecretConfig;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Singleton
public class ConfigTimedTask implements ApplicationEventListener<ServerStartupEvent> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConfigTimedTask.class);

    private final GHConfig ghConfig;
    private final SecretConfig secretConfig;

    public ConfigTimedTask(@NotNull GHConfig ghConfig, @NotNull SecretConfig secretConfig) {
        this.ghConfig = ghConfig;
        this.secretConfig = secretConfig;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        log.info("All registered config: ");
        log.info("GitHub config: {}", ghConfig);
        log.info("Secret config: {}", secretConfig);
        log.info("");
    }
}

package cn.powernukkitx.cloud.task;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Singleton
@Slf4j
public class StatusTimedTask {
    @Scheduled(fixedRate = "10m")
    void status() {
        var totalMemory = Runtime.getRuntime().totalMemory();
        var freeMemory = Runtime.getRuntime().freeMemory();
        log.info("Memory usage: {}% ({} MB/ {} MB)", String.format("%.2f", (totalMemory - freeMemory) * 100.0 / totalMemory), (totalMemory - freeMemory) / 1024 / 1024, totalMemory / 1024 / 1024);
    }
}

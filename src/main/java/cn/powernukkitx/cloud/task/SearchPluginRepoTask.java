package cn.powernukkitx.cloud.task;

import cn.powernukkitx.cloud.helper.AsyncHelper;
import cn.powernukkitx.cloud.helper.GitHubHelper;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class SearchPluginRepoTask {
    private final GitHubHelper ghHelper;

    public SearchPluginRepoTask(@NotNull GitHubHelper ghHelper) {
        this.ghHelper = ghHelper;
    }

    @Scheduled(fixedRate = "30m", initialDelay = "30m")
    public void search() {
        log.info("Searching all possible plugin repos...");
        AsyncHelper.runIOTask(ghHelper::searchPluginRepos).thenRun(() -> log.info("Search finished"));
    }
}

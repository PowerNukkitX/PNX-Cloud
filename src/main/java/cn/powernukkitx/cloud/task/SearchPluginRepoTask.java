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

    @Scheduled(fixedRate = "30m", initialDelay = "10m")
    public void searchByKeywords() {
        log.info("Searching all possible plugin repos by keywords...");
        AsyncHelper.runIOTask(() -> ghHelper.searchPluginRepos(GitHubHelper.SearchMethod.KEYWORDS))
                .thenRun(() -> log.info("Keywords search finished"));
    }

    @Scheduled(fixedRate = "30m", initialDelay = "20m")
    public void searchByMaven() {
        log.info("Searching all possible plugin repos by maven...");
        AsyncHelper.runIOTask(() -> ghHelper.searchPluginRepos(GitHubHelper.SearchMethod.MAVEN))
                .thenRun(() -> log.info("Maven search finished"));
    }

    @Scheduled(fixedRate = "30m", initialDelay = "30m")
    public void searchByGradle() {
        log.info("Searching all possible plugin repos by gradle...");
        AsyncHelper.runIOTask(() -> ghHelper.searchPluginRepos(GitHubHelper.SearchMethod.GRADLE))
                .thenRun(() -> log.info("Gradle search finished"));
    }
}

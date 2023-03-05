package cn.powernukkitx.cloud.cmd;

import cn.powernukkitx.cloud.helper.AsyncHelper;
import cn.powernukkitx.cloud.helper.DBHelper;
import cn.powernukkitx.cloud.helper.GitHubHelper;
import jakarta.inject.Singleton;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;

@Singleton
public class PluginRepoCommand implements Command {
    private final DBHelper dbHelper;
    private final GitHubHelper ghHelper;

    public PluginRepoCommand(@NotNull DBHelper dbHelper, @NotNull GitHubHelper ghHelper) {
        this.dbHelper = dbHelper;
        this.ghHelper = ghHelper;
    }

    @Override
    public @NotNull String getName() {
        return "pluginRepo";
    }

    @Override
    public boolean execute(@NotNull Logger log, @NotNull String... args) {
        if (args == null || args.length == 0) {
            log.info("Usage: pluginRepo <command> [args]");
            log.info("Commands:");
            log.info("  search - Search plugins on GitHub");
            log.info("  info <id> - Get plugin info");
            log.info("  add <id> - Add a new plugin repo");
            log.info("  ban <id> - Ban a plugin repo");
            log.info("  editorScore <id> <score> - Set the editor score of a plugin repo");
            log.info("  list - List all plugins");
            log.info("  clear - Clear all plugins");
            return true;
        }
        switch (args[0]) {
            case "info" -> {
                return false;
            }
            case "search" -> {
                log.info("Searching all possible plugin repos...");
                AsyncHelper.runIOTask(ghHelper::searchPluginRepos).thenRun(() -> log.info("Search finished"));
                return true;
            }
            case "add" -> {
                if (args.length < 2) {
                    log.info("Usage: pluginRepo add <id>");
                    return false;
                }
                add(log, args[1]);
                return true;
            }
            case "ban" -> {
                if (args.length < 2) {
                    log.info("Usage: pluginRepo ban <id>");
                    return false;
                }
                ban(log, args[1]);
                return true;
            }
            case "editorScore" -> {
                if (args.length < 3) {
                    log.info("Usage: pluginRepo editorScore <id> <score>");
                    return false;
                }
                try {
                    var score = Integer.parseInt(args[2]);
                    editorScore(log, args[1], score);
                } catch (NumberFormatException e) {
                    log.info("Invalid score: {}", args[2]);
                    return false;
                }
                return true;
            }
            case "list" -> {
                list(log);
                return true;
            }
            case "clear" -> {
                clear(log);
                return true;
            }
            default -> {
                log.info("Unknown command: {}", args[0]);
                return false;
            }
        }
    }

    private void list(@NotNull Logger log) {
        log.info("Listing all plugins...");
        for (var each : dbHelper.getRepoDataBeanObjectRepository().find()) {
            log.info("{}", each);
        }
    }

    private void clear(@NotNull Logger log) {
        log.info("Clearing all plugins...");
        dbHelper.getRepoDataBeanObjectRepository().remove(ObjectFilters.ALL);
        log.info("Clear finished");
    }

    private void add(@NotNull Logger log, @NotNull String id) {
        var bean = dbHelper.getRepoDataBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault();
        if (bean != null) {
            log.info("Repo already exists: {}", id);
            return;
        }
        var repo = GitHubHelper.getRepo(ghHelper.getConfig(), id);
        if (repo == null) {
            log.info("Failed to get repo: {}", id);
            return;
        }
        log.info("Adding plugin repo: {}", id);
        try {
            ghHelper.checkAndAddRepo(repo);
        } catch (IOException e) {
            log.info("Failed to add repo: {}", id, e);
            return;
        }
        log.info("Successfully added plugin repo: {}", id);
    }

    private void ban(@NotNull Logger log, @NotNull String id) {
        var bean = dbHelper.getRepoDataBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault();
        if (bean == null) {
            log.info("Repo not exists: {}", id);
            return;
        }
        log.info("Banning plugin repo: {}", id);
        bean.setBanned(true);
        log.info("Successfully banned plugin repo: {}", id);
    }

    private void editorScore(@NotNull Logger log, @NotNull String id, int score) {
        var bean = dbHelper.getRepoDataBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault();
        if (bean == null) {
            log.info("Repo not exists: {}", id);
            return;
        }
        log.info("Setting editor score of plugin repo: {}", id);
        bean.setEditorRecommendScore(score);
        log.info("Successfully set editor score of plugin repo: {}", id);
    }

    @Override
    public @NotNull String @NotNull [] getAliases() {
        return new String[]{"repo"};
    }
}

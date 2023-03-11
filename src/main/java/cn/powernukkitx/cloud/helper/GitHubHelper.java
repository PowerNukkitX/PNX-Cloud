package cn.powernukkitx.cloud.helper;

import cn.powernukkitx.cloud.bean.DownloadIDBean;
import cn.powernukkitx.cloud.bean.RepoDataBean;
import cn.powernukkitx.cloud.config.GHConfig;
import cn.powernukkitx.cloud.exception.GHRepoFailedException;
import cn.powernukkitx.cloud.exception.GHRepoNotFoundException;
import cn.powernukkitx.cloud.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Singleton
@CacheConfig(value = "github", keyGenerator = PrefixedKeyGenerator.class)
@Slf4j
public class GitHubHelper {
    private static final AtomicReference<GitHub> GITHUB = new AtomicReference<>(null);
    private static final AtomicLong LAST_UPDATE = new AtomicLong(0);

    @Getter
    private final GHConfig config;
    private final DBHelper dbHelper;
    private final Yaml yaml;

    public GitHubHelper(@NotNull GHConfig config, DBHelper dbHelper) {
        this.config = config;
        this.dbHelper = dbHelper;
        this.yaml = new Yaml();
    }

    @Cacheable
    public @NotNull List<GHIssue> getIssueListCached(GHIssueState state) throws IOException {
        return getRepoNotNull(config).getIssues(state);
    }

    @Cacheable
    public @NotNull List<GHIssue> getIssueListCached(GHIssueState state, String repo) throws IOException {
        return getRepoNotNull(config, repo).getIssues(GHIssueState.ALL);
    }

    @Cacheable
    @CacheKeyPrefix
    public int getStarCached() {
        return getRepoNotNull(config).getStargazersCount();
    }

    @Cacheable
    @CacheKeyPrefix
    public int getStarCached(String repo) {
        return getRepoNotNull(config, repo).getStargazersCount();
    }

    @Cacheable
    @CacheKeyPrefix
    public int getOpenIssueCountCached() {
        return getRepoNotNull(config).getOpenIssueCount();
    }

    @Cacheable
    @CacheKeyPrefix
    public int getOpenIssueCountCached(String repo) {
        return getRepoNotNull(config, repo).getOpenIssueCount();
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull List<GHArtifact> getLatestBuildArtifactCached() {
        var it = getRepoNotNull(config).listArtifacts()._iterator(4);
        return List.of(it.next(), it.next(), it.next(), it.next());
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull List<GHArtifact> getLatestBuildArtifactCached(String repo) {
        var it = getRepoNotNull(config, repo).listArtifacts()._iterator(4);
        return List.of(it.next(), it.next(), it.next(), it.next());
    }

    @Cacheable
    @CacheKeyPrefix
    public @Nullable GHRelease getLatestReleaseCached() throws IOException {
        return getRepoNotNull(config).getLatestRelease();
    }

    @Cacheable
    @CacheKeyPrefix
    public @Nullable GHRelease getLatestReleaseCached(String repo) throws IOException {
        return getRepoNotNull(config, repo).getLatestRelease();
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull List<GHRelease> getReleasesCached() throws IOException {
        return getRepoNotNull(config).listReleases().toList();
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull List<GHRelease> getReleasesCached(String repo) throws IOException {
        return getRepoNotNull(config, repo).listReleases().toList();
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull List<GHAsset> getReleaseAssetsCached(@NotNull GHRelease release) throws IOException {
        return release.listAssets().toList();
    }

    public record ReadMe(String fileName, String content) {
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull ReadMe getReadmeCached(String repo) throws IOException {
        try {
            var readme = getRepoNotNull(config, repo).getReadme();
            try (var ins = readme.read()) {
                return new ReadMe(readme.getName(), new String(ins.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (FileNotFoundException e) {
            return new ReadMe(null, null);
        }
    }

    @Cacheable
    @CacheKeyPrefix
    public long getRepoIconDownloadIDCached(String repo) throws IOException {
        return DownloadIDBean.getDownloadId(getRepoNotNull(config, repo).getOwner().getAvatarUrl(),
                dbHelper.getDownloadIDBeanObjectRepository());
    }

    public void searchPluginRepos() {
        var gh = getGitHub(config, true);
        var repoSearchReq = gh.searchRepositories()
                .q("((pnx OR powernukkitx OR \"PowerNukkit X\") AND (plugin OR plug-in))")
                .language("Java")
                .language("Kotlin")
                .language("JavaScript")
                .language("Groovy")
                .language("Scala")
                .language("TypeScript")
                .in("name")
                .in("description")
                .in("readme")
                .in("topics")
                .sort(GHRepositorySearchBuilder.Sort.UPDATED)
                .order(GHDirection.DESC);
        log.debug("Searching for plugin repos by keywords...");
        var allKnownRepos = dbHelper.getRepoDataBeanObjectRepository().find().toList()
                .stream().filter(e -> !e.isBanned()).map(RepoDataBean::getId).collect(Collectors.toSet());
        var scannedRepos = new ArrayList<String>();
        for (var repo : repoSearchReq.list().withPageSize(100)) {
            allKnownRepos.remove(repo.getFullName());
            try {
                log.debug("Scanning repo {}", repo.getFullName());
                checkAndAddRepo(repo);
                scannedRepos.add(repo.getFullName());
            } catch (IOException e) {
                log.error("Failed to check repo " + repo.getFullName(), e);
            }
        }
        log.debug("Searching for plugin repos by pom.xml...");
        var buildCodeSearchReq = gh.searchContent()
                .q("dependency groupId cn.powernukkitx artifactId powernukkitx")
                .sort(GHContentSearchBuilder.Sort.BEST_MATCH);
        for (var snippet : buildCodeSearchReq.list().withPageSize(100)) {
            if (scannedRepos.contains(snippet.getOwner().getFullName())) {
                log.debug("Skipping repo {} because it was already scanned", snippet.getOwner().getFullName());
                continue;
            }
            var repo = getRepo(config, snippet.getOwner().getFullName());
            if (repo == null || scannedRepos.contains(repo.getFullName())) {
                log.debug("Skipping repo {} because it was already scanned", NullUtil.tryDo(repo, GHRepository::getFullName, "null"));
                continue;
            }
            allKnownRepos.remove(repo.getFullName());
            try {
                log.debug("Scanning repo {}", repo.getFullName());
                checkAndAddRepo(repo);
                scannedRepos.add(repo.getFullName());
            } catch (IOException e) {
                log.error("Failed to check repo " + repo.getFullName(), e);
            }
        }
        // build.gradle
        log.debug("Searching for plugin repos by build.gradle...");
        buildCodeSearchReq = gh.searchContent()
                .q("\"cn.powernukkitx:powernukkitx\"")
                .sort(GHContentSearchBuilder.Sort.BEST_MATCH);
        for (var snippet : buildCodeSearchReq.list().withPageSize(100)) {
            if (scannedRepos.contains(snippet.getOwner().getFullName())) {
                log.debug("Skipping repo {} because it was already scanned", snippet.getOwner().getFullName());
                continue;
            }
            var repo = getRepo(config, snippet.getOwner().getFullName());
            if (repo == null || scannedRepos.contains(repo.getFullName())) {
                log.debug("Skipping repo {} because it was already scanned", NullUtil.tryDo(repo, GHRepository::getFullName, "null"));
                continue;
            }
            allKnownRepos.remove(repo.getFullName());
            try {
                log.debug("Scanning repo {}", repo.getFullName());
                checkAndAddRepo(repo);
                scannedRepos.add(repo.getFullName());
            } catch (IOException e) {
                log.error("Failed to check repo " + repo.getFullName(), e);
            }
        }
        log.debug("Searching for custom plugin repos...");
        for (var id : allKnownRepos) {
            if (scannedRepos.contains(id)) {
                log.debug("Skipping repo {} because it was already scanned", id);
                continue;
            }
            GHRepository repo;
            try {
                repo = getRepo(config, id);
            } catch (GHRepoNotFoundException e) {
                log.debug("Remove repo {} because it is not accessible", id);
                dbHelper.getRepoDataBeanObjectRepository().remove(ObjectFilters.eq("id", id));
                continue;
            }
            if (repo == null || scannedRepos.contains(repo.getFullName())) {
                log.debug("Skipping repo {} because it was already scanned", id);
                continue;
            }
            try {
                log.debug("Scanning repo {}", repo.getFullName());
                checkAndAddRepo(repo);
                scannedRepos.add(id);
            } catch (IOException e) {
                log.error("Failed to check repo " + repo.getFullName(), e);
            }
        }
    }

    public void checkAndAddRepo(@NotNull GHRepository repo) throws IOException {
        var bean = new RepoDataBean();
        if (repo.isArchived() || repo.isDisabled() || repo.isPrivate() ||
                repo.getName().equals(config.getRepoName()) || config.getOtherRepos().contains(repo.getName())) {
            return;
        }
        bean.setId(repo.getFullName());
        bean.setName(repo.getName());
        bean.setOwner(repo.getOwnerName());
        bean.setDescription(repo.getDescription());
        var findResult = dbHelper.getRepoDataBeanObjectRepository().find(ObjectFilters.eq("id", bean.getId())).firstOrDefault();
        // 此前没有收录的插件或超过一天内没有做过全量索引的插件才做过滤
        if (findResult == null || findResult.getLastFullIndexTime().getTime() < System.currentTimeMillis() - 86400000) {
            if (findResult != null) {
                bean = findResult;
            }
            var hasPNXKeyWord = false;
            var hasPluginKeyWord = false;
            hasPNXKeyWord = bean.getId().toLowerCase().contains("pnx") || bean.getId().toLowerCase().contains("powernukkitx");
            hasPluginKeyWord = bean.getId().toLowerCase().contains("plugin") || bean.getId().toLowerCase().contains("plug-in");
            if (!hasPNXKeyWord || !hasPluginKeyWord) {
                try {
                    var readme = getReadmeCached(bean.getId());
                    if (readme.content != null) {
                        var content = readme.content.toLowerCase();
                        hasPNXKeyWord = hasPNXKeyWord || content.toLowerCase().contains("pnx") || content.toLowerCase().contains("powernukkitx");
                        hasPluginKeyWord = hasPluginKeyWord || content.toLowerCase().contains("plugin") || content.toLowerCase().contains("plug-in");
                    }
                } catch (IOException ignored) {

                }
            }
            if (!hasPNXKeyWord || !hasPluginKeyWord) {
                try {
                    bean.setTopics(String.join(" ", repo.listTopics()));
                    var lowerTopics = bean.getTopics().toLowerCase();
                    hasPNXKeyWord = hasPNXKeyWord || lowerTopics.contains("pnx") || lowerTopics.contains("powernukkitx");
                    hasPluginKeyWord = hasPluginKeyWord || lowerTopics.contains("plugin") || lowerTopics.contains("plug-in");
                } catch (IOException ignored) {

                }
            }

            // 尝试检查是否存在插件配置文件
            var hasPluginYml = false;
            String pluginYmlContent = null;
            try (var ins = repo.getFileContent("/src/main/resources/plugin.yml").read()) {
                hasPluginYml = true;
                pluginYmlContent = new String(ins.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {

            }
            if (!hasPluginYml) {
                try (var ins = repo.getFileContent("/plugin.yml").read()) {
                    hasPluginYml = true;
                    pluginYmlContent = new String(ins.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException ignored) {

                }
            }
            if (((hasPluginKeyWord && hasPNXKeyWord) || repo.getStargazersCount() >= 10 || bean.getEditorRecommendScore() > 0) && !hasPluginYml) {
                log.debug("Deep searching plugin.yml for repo " + repo.getFullName());
                var tree = repo.getTreeRecursive(repo.getDefaultBranch(), 1);
                for (var each : tree.getTree()) {
                    if (each.getPath().endsWith("plugin.yml")) {
                        try (var ins = each.readAsBlob()) {
                            var tmpString = new String(ins.readAllBytes(), StandardCharsets.UTF_8);
                            if (tmpString.contains("api")) {
                                log.debug("Found plugin.yml for repo {} at {}.", repo.getFullName(), each.getPath());
                                hasPluginYml = true;
                                pluginYmlContent = tmpString;
                                break;
                            }
                        } catch (IOException ignored) {

                        }
                    }
                }
            }
            if (!(hasPluginYml || (hasPNXKeyWord && hasPluginKeyWord))) {
                return;
            }
            if (hasPluginYml) {
                var pluginYmlObject = yaml.loadAs(pluginYmlContent, Map.class);
                if (pluginYmlObject.containsKey("name")) {
                    bean.setPluginName(pluginYmlObject.get("name").toString());
                    if (bean.getPluginName().equals("${project.name}")) {
                        bean.setPluginName(bean.getName());
                    }
                }
                if (pluginYmlObject.containsKey("main")) {
                    bean.setMainClass(pluginYmlObject.get("main").toString());
                }
                if (pluginYmlObject.containsKey("depend")) {
                    bean.setDependencies(StringUtil.toStringArray(pluginYmlObject.get("depend")));
                } else {
                    bean.setDependencies(StringUtil.EMPTY_STRING_ARRAY);
                }
                if (pluginYmlObject.containsKey("softdepend")) {
                    bean.setSoftDependencies(StringUtil.toStringArray(pluginYmlObject.get("softdepend")));
                } else {
                    bean.setSoftDependencies(StringUtil.EMPTY_STRING_ARRAY);
                }
            }
            bean.setLastFullIndexTime(new Date());
        } else {
            bean = findResult;
        }
        if ((bean.getTopics() == null || bean.getTopics().isEmpty()) && (bean.getLastUpdateAt() == null || bean.getLastUpdateAt().before(repo.getUpdatedAt()))) {
            bean.setTopics(String.join(" ", repo.listTopics()));
        }
        bean.setStar(repo.getStargazersCount());
        bean.setMainLanguage(repo.getLanguage());
        bean.setLastUpdateAt(repo.getUpdatedAt());
        if (bean.getLastUpdateAt() == null) {
            return;
        }
        if (bean.getIconDownloadID() <= 0) {
            bean.setIconDownloadID(getRepoIconDownloadIDCached(bean.getId()));
        }
        bean.calcQualityScore(config);
        dbHelper.getRepoDataBeanObjectRepository().update(bean, true);
    }

    private final Cache<String, String> markdownCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(32).build();

    public String renderMarkdown(@NotNull String markdown, @Nullable String repo) throws IOException {
        var key = (repo == null ? "" : repo) + "::" + markdown.hashCode();
        var md = markdownCache.getIfPresent(key);
        if (repo == null) {
            if (md == null) {
                var writer = new StringWriter();
                try (var reader = getGitHub(config).renderMarkdown(markdown)) {
                    reader.transferTo(writer);
                }
                markdownCache.put(key, md = writer.toString());
            }
        } else {
            if (md == null) {
                var writer = new StringWriter();
                try (var reader = getRepoNotNull(config, repo).renderMarkdown(markdown, MarkdownMode.GFM)) {
                    reader.transferTo(writer);
                }
                markdownCache.put(key, md = writer.toString());
            }
        }
        return md;
    }

    static Field clientField;
    static Method getEncodedAuthorizationMethod;

    static {
        try {
            clientField = GitHub.class.getDeclaredField("client");
            clientField.setAccessible(true);
            var GitHubClientClass = Class.forName("org.kohsuke.github.GitHubClient");
            getEncodedAuthorizationMethod = GitHubClientClass.getDeclaredMethod("getEncodedAuthorization");
            getEncodedAuthorizationMethod.setAccessible(true);
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public @NotNull String getEncodedAuthorization() {
        try {
            var ghClient = clientField.get(getGitHub(config));
            return (String) getEncodedAuthorizationMethod.invoke(ghClient);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static GitHub getGitHub(GHConfig config) {
        return getGitHub(config, false);
    }

    public static GitHub getGitHub(GHConfig config, boolean forceRefreshToken) {
        return GITHUB.updateAndGet(gitHub -> {
            var current = System.currentTimeMillis();
            if (forceRefreshToken || gitHub == null || current - LAST_UPDATE.get() > 60 * 60 * 1000) {
                try {
                    LAST_UPDATE.set(current);
                    return new GitHubBuilder().withAppInstallationToken(getToken(config))
                            .withConnector(new OkHttpGitHubConnector(HttpUtil.client)).build();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return gitHub;
        });
    }

    public static @Nullable GHRepository getRepo(GHConfig config) {
        try {
            return getGitHub(config).getRepository(config.getOrganization() + "/" + config.getRepoName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static @Nullable GHRepository getRepo(GHConfig config, String repoName) {
        try {
            if (repoName.contains("/") || config.getRepoName().equals(repoName) || config.getOtherRepos().contains(repoName)) {
                if (!repoName.contains("/")) {
                    repoName = config.getOrganization() + "/" + repoName;
                }
                return getGitHub(config).getRepository(repoName);
            } else
                throw new GHRepoNotFoundException(repoName);
        } catch (Exception e) {
            if (e instanceof GHFileNotFoundException) {
                throw new GHRepoNotFoundException(repoName);
            }
            e.printStackTrace();
        }
        return null;
    }

    public static @NotNull GHRepository getRepoNotNull(GHConfig config) {
        var repo = getRepo(config);
        if (repo == null) {
            throw new GHRepoFailedException(config.getRepoName());
        }
        return repo;
    }

    public static @NotNull GHRepository getRepoNotNull(GHConfig config, String repoName) {
        if (repoName == null) {
            return getRepoNotNull(config);
        }
        if (!repoName.contains("/") && !config.getRepoName().equals(repoName) && !config.getOtherRepos().contains(repoName))
            throw new GHRepoNotFoundException(repoName);
        var repo = getRepo(config, repoName);
        if (repo == null) {
            throw new GHRepoFailedException(repoName);
        }
        return repo;
    }

    static String getToken(GHConfig config) throws Exception {
        var binDirPath = Path.of("data/bin");
        var cliPath = binDirPath.resolve("gh-cli" + (EnumOS.getOs() == EnumOS.WINDOWS ? ".exe" : ""));
        var cliResource = switch (EnumOS.getOs()) {
            case WINDOWS -> "gh-cli-win.exe";
            case LINUX, SOLARIS -> "gh-cli-linux";
            case OSX -> "gh-cli-mac";
            case UNKNOWN -> throw new IllegalStateException("Unknown OS");
        };
        if (!Files.exists(cliPath)) {
            if (!Files.exists(binDirPath)) {
                Files.createDirectories(binDirPath);
            }
            Files.copy(Objects.requireNonNull(GitHubHelper.class.getClassLoader().getResourceAsStream(cliResource)), cliPath);
        }
        var process = new ProcessBuilder(StringUtil.tryWarpPath(cliPath.toString()),
                "--privateKeyLocation", StringUtil.tryWarpPath(config.getPrivateKeyPath().toString()),
                "--appId", String.valueOf(config.getAppId()),
                "--installationId", String.valueOf(config.getInstallationId()))
                .start();
        process.waitFor(10000, TimeUnit.MICROSECONDS);
        try (var er = process.errorReader()) {
            for (String line; (line = er.readLine()) != null; ) {
                log.info(line);
            }
        }
        try (var eis = process.inputReader()) {
            var tmp = eis.readLine();
            log.info("Refreshed GH token {}.", tmp);
            return tmp;
        }
    }
}

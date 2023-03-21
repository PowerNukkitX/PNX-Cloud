package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.auth.Roles;
import cn.powernukkitx.cloud.bean.RepoDataBean;
import cn.powernukkitx.cloud.bean.RequestIDBean;
import cn.powernukkitx.cloud.exception.InvalidIndexException;
import cn.powernukkitx.cloud.helper.AsyncHelper;
import cn.powernukkitx.cloud.helper.DBHelper;
import cn.powernukkitx.cloud.helper.MeiliSearchHelper;
import cn.powernukkitx.cloud.util.Ok;
import cn.powernukkitx.cloud.util.PrefixedKeyGenerator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import jakarta.annotation.security.RolesAllowed;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import static org.dizitart.no2.objects.filters.ObjectFilters.*;

@Controller("/api/plugin")
@RolesAllowed(Roles.API_Plugin)
@CacheConfig(value = "plugin", keyGenerator = PrefixedKeyGenerator.class)
public class PluginController {
    private final DBHelper dbHelper;
    private final AsyncHelper asyncHelper;
    private final MeiliSearchHelper searchHelper;

    public PluginController(DBHelper dbHelper, AsyncHelper asyncHelper, MeiliSearchHelper searchHelper) {
        this.dbHelper = dbHelper;
        this.asyncHelper = asyncHelper;
        this.searchHelper = searchHelper;
    }

    record PluginListRes(int size, int totalSize, List<?> plugins) {
    }

    @Get("/list")
    public PluginListRes list(@QueryValue(defaultValue = "0") int from, @QueryValue(defaultValue = "15") int size) {
        return list(from, size, "recommend", "desc");
    }

    @Get("/list/{sorter:recommend|lastUpdate|star}")
    public PluginListRes list(@QueryValue(defaultValue = "0") int from, @QueryValue(defaultValue = "15") int size,
                              @NotNull String sorter) {
        return list(from, size, sorter, "desc");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Get("/list/{sorter:recommend|lastUpdate|star}/{order:asc|desc}")
    public PluginListRes list(@QueryValue(defaultValue = "0") int from, @QueryValue(defaultValue = "15") int size,
                              @NotNull String sorter, @NotNull String order) {
        if (from < 0 || size <= 0) {
            throw new InvalidIndexException("Invalid index: from=" + from + ", size=" + size);
        }
        if (size > 100) {
            throw new InvalidIndexException("Too many items, should be no more than 100: from=" + from + ", size=" + size);
        }
        var repoDataBeanObjectRepository = dbHelper.getRepoDataBeanObjectRepository();
        var repoSize = repoDataBeanObjectRepository.size();
        if (from + size > repoSize) {
            size = (int) (repoSize - from);
        }
        SortOrder sortOrder;
        if (order.equalsIgnoreCase("desc")) {
            sortOrder = SortOrder.Descending;
        } else {
            sortOrder = SortOrder.Ascending;
        }
        FindOptions findOptions = switch (sorter.toLowerCase()) {
            case "recommend" -> FindOptions.sort("qualityScore", sortOrder);
            case "lastupdate" -> FindOptions.sort("lastUpdateAt", sortOrder);
            case "star" -> FindOptions.sort("star", sortOrder);
            default -> throw new IllegalArgumentException("Invalid sorter: " + sorter);
        };
        var result = repoDataBeanObjectRepository.find(eq("banned", false),
                findOptions.thenLimit(from, size));
        return new PluginListRes(result.size(), (int) repoSize, result.toList());
    }

    @Get("/search")
    public PluginListRes search(@QueryValue String keywords, @QueryValue(defaultValue = "15") int size) throws MeilisearchException {
        return search(keywords, size, "desc");
    }

    @Get("/search/{order:asc|desc}")
    public PluginListRes search(@QueryValue String keywords, @QueryValue(defaultValue = "15") int size,
                                @NotNull String order) throws MeilisearchException {
        if (size <= 0) {
            throw new InvalidIndexException("Invalid index: size=" + size);
        }
        if (size > 100) {
            throw new InvalidIndexException("Too many items, should be no more than 100: size=" + size);
        }
        var result = searchHelper.search(keywords, size, order);
        var hits = result.getHits();
        return new PluginListRes(hits.size(), (int) dbHelper.getRepoDataBeanObjectRepository().size(), hits);
    }

    @Get("/get/{id:[^/]+(/[^/]+)?}")
    public RepoDataBean get(String id) {
        var repoDataBeanObjectRepository = dbHelper.getRepoDataBeanObjectRepository();
        var result = repoDataBeanObjectRepository.find(eq("id", id)).firstOrDefault();
        if (result == null) {
            throw new InvalidIndexException("Invalid id: " + id);
        }
        return result;
    }

    record DependencyGraphRes(boolean success,
                              @JsonSerialize(nullsUsing = NullSerializer.class) @Nullable String reason,
                              @JsonSerialize(nullsUsing = NullSerializer.class) @Nullable String mermaid) {

    }

    record Link(boolean isSoftDependency, String toPluginName, String fromPluginName) {
    }

    static final Cache<String, String> dependencyGraphCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(128)
            .build();

    @Get("/dependency-graph/{name}")
    public RequestIDBean dependencyGraph(String name) {
        return asyncHelper.runIOTask(UUID.nameUUIDFromBytes(("dependency-graph-" + name + ThreadLocalRandom.current().nextInt()).getBytes()), () -> {
            var cachedMermaid = dependencyGraphCache.getIfPresent(name);
            if (cachedMermaid != null) {
                return HttpResponse.ok(new DependencyGraphRes(true, null, cachedMermaid));
            }
            var repoDataBeanObjectRepository = dbHelper.getRepoDataBeanObjectRepository();
            var root = repoDataBeanObjectRepository.find(eq("pluginName", name)).firstOrDefault();
            if (root == null) {
                return HttpResponse.badRequest(new DependencyGraphRes(false, "Plugin " + name + " not found.", null));
            }
            var searchedSet = new HashSet<String>(16);
            var toSearchSet = new HashSet<RepoDataBean>(4);
            var pluginList = new ArrayList<String>(8);
            var linkList = new LinkedHashSet<Link>(32);
            toSearchSet.add(root);
            // 查找依赖的插件
            while (!toSearchSet.isEmpty()) {
                var plugin = toSearchSet.iterator().next();
                toSearchSet.remove(plugin);
                if (searchedSet.contains(plugin.getPluginName())) {
                    continue;
                }
                searchedSet.add(plugin.getPluginName());
                pluginList.add(plugin.getPluginName());
                for (var dependency : plugin.getDependencies()) {
                    var dependencyPlugin = repoDataBeanObjectRepository.find(eq("pluginName", dependency)).firstOrDefault();
                    if (dependencyPlugin != null) {
                        toSearchSet.add(dependencyPlugin);
                    }
                    linkList.add(new Link(false, plugin.getPluginName(), dependency));
                }
                for (var softDependency : plugin.getSoftDependencies()) {
                    var softDependencyPlugin = repoDataBeanObjectRepository.find(eq("pluginName", softDependency)).firstOrDefault();
                    if (softDependencyPlugin != null) {
                        toSearchSet.add(softDependencyPlugin);
                    }
                    linkList.add(new Link(true, plugin.getPluginName(), softDependency));
                }
            }
            // 保证正确的顺序
            Collections.reverse(pluginList);
            BiConsumer<RepoDataBean, Boolean> tmp = (dependentPlugin, isSoftDependency) -> {
                if (searchedSet.contains(dependentPlugin.getPluginName())) {
                    return;
                }
                searchedSet.add(dependentPlugin.getPluginName());
                pluginList.add(dependentPlugin.getPluginName());
                for (var each : dependentPlugin.getDependencies()) {
                    if (pluginList.contains(each)) {
                        linkList.add(new Link(false, dependentPlugin.getPluginName(), each));
                    }
                }
                for (var each : dependentPlugin.getSoftDependencies()) {
                    if (pluginList.contains(each)) {
                        linkList.add(new Link(true, dependentPlugin.getPluginName(), each));
                    }
                }
                linkList.add(new Link(isSoftDependency, dependentPlugin.getPluginName(), name));
            };
            // 查找被依赖的插件
            repoDataBeanObjectRepository.find(elemMatch("dependencies", eq("pluginName", name))).forEach(v -> tmp.accept(v, false));
            repoDataBeanObjectRepository.find(elemMatch("softDependencies", eq("pluginName", name))).forEach(v -> tmp.accept(v, true));
            var mermaid = new StringBuilder("graph TD").append('\n');
            for (var plugin : pluginList) {
                mermaid.append("    ").append(plugin).append("(").append(plugin).append(")").append('\n');
            }
            for (var link : linkList) {
                mermaid.append("    ").append(link.fromPluginName()).append("-");
                if (link.isSoftDependency()) {
                    mermaid.append(".-");
                } else {
                    mermaid.append("--");
                }
                mermaid.append(">").append(link.toPluginName()).append('\n');
            }
            return HttpResponse.ok(new DependencyGraphRes(true, null, mermaid.toString()));
        });
    }

    @Error(exception = InvalidIndexException.class)
    public HttpResponse<Ok<InvalidIndexException>> invalidIndex(InvalidIndexException e) {
        return HttpResponse.badRequest(Ok.ok(e));
    }

    @Error(exception = IllegalArgumentException.class)
    public HttpResponse<Ok<IllegalArgumentException>> invalidArgument(IllegalArgumentException e) {
        return HttpResponse.badRequest(Ok.ok(e));
    }

    @Error(exception = MeilisearchException.class)
    public HttpResponse<Ok<MeilisearchException>> meilisearchError(MeilisearchException e) {
        return HttpResponse.serverError(Ok.ok(e));
    }
}

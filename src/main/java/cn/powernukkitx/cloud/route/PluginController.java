package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.auth.Roles;
import cn.powernukkitx.cloud.bean.RepoDataBean;
import cn.powernukkitx.cloud.exception.InvalidIndexException;
import cn.powernukkitx.cloud.helper.DBHelper;
import cn.powernukkitx.cloud.util.Ok;
import cn.powernukkitx.cloud.util.PrefixedKeyGenerator;
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

import java.util.List;

import static org.dizitart.no2.objects.filters.ObjectFilters.*;

@Controller("/api/plugin")
@RolesAllowed(Roles.API_Plugin)
@CacheConfig(value = "plugin", keyGenerator = PrefixedKeyGenerator.class)
public class PluginController {
    private final DBHelper dbHelper;

    public PluginController(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    record PluginListRes(int size, List<RepoDataBean> plugins) {
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
        return new PluginListRes(result.size(), result.toList());
    }

    @Get("/search")
    public PluginListRes search(@QueryValue String keywords, @QueryValue(defaultValue = "15") int size) {
        return search(keywords, size, "desc");
    }

    @Get("/search/{order:asc|desc}")
    public PluginListRes search(@QueryValue String keywords, @QueryValue(defaultValue = "15") int size,
                                @NotNull String order) {
        if (size <= 0) {
            throw new InvalidIndexException("Invalid index: size=" + size);
        }
        if (size > 30) {
            throw new InvalidIndexException("Too many items, should be no more than 100: size=" + size);
        }
        var repoDataBeanObjectRepository = dbHelper.getRepoDataBeanObjectRepository();
        SortOrder sortOrder;
        if (order.equalsIgnoreCase("desc")) {
            sortOrder = SortOrder.Descending;
        } else {
            sortOrder = SortOrder.Ascending;
        }
        var result = repoDataBeanObjectRepository.find(and(
                eq("banned", false),
                or(
                        eq("owner", keywords),
                        eq("name", keywords),
                        eq("id", keywords),
                        text("description", keywords),
                        eq("mainLanguage", keywords),
                        text("topics", keywords),
                        eq("pluginName", keywords)
                )
        ), FindOptions.sort("qualityScore", sortOrder).thenLimit(0, size));
        return new PluginListRes(result.size(), result.toList());
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

    @Error(exception = InvalidIndexException.class)
    public HttpResponse<Ok<InvalidIndexException>> invalidIndex(InvalidIndexException e) {
        return HttpResponse.badRequest(Ok.ok(e));
    }

    @Error(exception = IllegalArgumentException.class)
    public HttpResponse<Ok<IllegalArgumentException>> invalidArgument(IllegalArgumentException e) {
        return HttpResponse.badRequest(Ok.ok(e));
    }
}

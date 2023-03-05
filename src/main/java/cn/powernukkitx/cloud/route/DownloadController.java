package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.auth.Roles;
import cn.powernukkitx.cloud.bean.RequestIDBean;
import cn.powernukkitx.cloud.helper.AsyncHelper;
import cn.powernukkitx.cloud.helper.DBHelper;
import cn.powernukkitx.cloud.helper.FileCacheHelper;
import cn.powernukkitx.cloud.util.FileCachedInputStream;
import cn.powernukkitx.cloud.util.Ok;
import cn.powernukkitx.cloud.util.StringUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.types.files.StreamedFile;
import io.micronaut.http.server.types.files.SystemFile;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

@Controller("/api/download")
@Slf4j
public class DownloadController {
    private final FileCacheHelper fileCacheHelper;
    private final DBHelper dbHelper;
    private final AsyncHelper asyncHelper;

    public DownloadController(@NotNull FileCacheHelper fileCacheHelper, @NotNull DBHelper dbHelper,
                              @NotNull AsyncHelper asyncHelper) {
        this.fileCacheHelper = fileCacheHelper;
        this.dbHelper = dbHelper;
        this.asyncHelper = asyncHelper;
    }

    @Get("/{id:^[0-9]+$}")
    @RolesAllowed(Roles.Download_IdFile)
    public Mono<HttpResponse<?>> download(long id) {
        return Mono.fromCallable(() -> {
            try {
                MutableHttpResponse<?> response;
                if (fileCacheHelper.isFileCached(id)) {
                    var file = fileCacheHelper.getCachePath(id).toFile();
                    response = HttpResponse.ok(new SystemFile(file, MediaType.forFilename(getDownloadName(file)))
                                    .attach(getDownloadName(file)))
                            .header("Content-Length", String.valueOf(file.length()));
                } else {
                    @SuppressWarnings("resource")
                    var result = fileCacheHelper.tryCacheFile(id);
                    var ins = result.bodyInputStream();
                    var url = fileCacheHelper.getRawURL(id);
                    var cachePath = fileCacheHelper.getCachePath(id);
                    var mediaType = MediaType.forFilename(getDownloadName(url));
                    if (result.rawResponse() != null && null != (result.rawResponse().header("content-type"))) {
                        var oldType = mediaType;
                        mediaType = MediaType.of(Objects.requireNonNull(result.rawResponse().header("content-type")));
                        if (!mediaType.equals(oldType)) {
                            cachePath = fileCacheHelper.resetExtNameOfCachePath(id, mediaType.getExtension());
                        }
                    }
                    InputStream cachedIns;
                    var cacheFile = cachePath.toFile();
                    if (FileCachedInputStream.isCaching(cacheFile)) {
                        cachedIns = ins;
                    } else {
                        cachedIns = new FileCachedInputStream(ins, cacheFile, (int) result.size());
                    }
                    response = HttpResponse.ok(new StreamedFile(cachedIns, mediaType)
                                    .attach(getDownloadName(cacheFile)))
                            .header("Content-Length", String.valueOf(result.size()));
                }
                return response.header("Cache-Control", "max-age=2592000");
            } catch (FileCacheHelper.IdNotFoundException e) {
                return HttpResponse.notFound(Ok.ok(e));
            } catch (IOException e) {
                return HttpResponse.serverError(Ok.ok(e));
            }
        });
    }

    private @NotNull String getDownloadName(@NotNull String url) {
        var name = StringUtil.afterLast(url, "/");
        if (!name.contains(".")) {
            return StringUtil.afterLast(StringUtil.beforeLast(url, "/"), "/") + "." + name;
        } else {
            return name;
        }
    }

    private @NotNull String getDownloadName(@NotNull File file) {
        var name = file.getName();
        if (!name.contains(".")) {
            return file.getParentFile().getName() + "." + name;
        } else {
            return name;
        }
    }

    record HasIdRes(boolean hasId) {
    }

    @Get("/has/{id:^[0-9]+$}")
    @RolesAllowed(Roles.API_Download)
    public HasIdRes hasId(long id) {
        return new HasIdRes(dbHelper.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id)).size() != 0);
    }

    record IsCachedRes(boolean isCached) {
    }

    @Get("/isCached/{id:^[0-9]+$}")
    @RolesAllowed(Roles.API_Download)
    public IsCachedRes isCached(long id) {
        try {
            return new IsCachedRes(fileCacheHelper.isFileCached(id));
        } catch (FileCacheHelper.IdNotFoundException | IOException e) {
            return new IsCachedRes(false);
        }
    }

    record RawURLRes(@JsonSerialize(nullsUsing = NullSerializer.class) String url) {
    }

    @Get("/rawURL/{id:^[0-9]+$}")
    @RolesAllowed(Roles.API_Download)
    public RawURLRes rawURL(long id) throws FileCacheHelper.IdNotFoundException {
        var url = fileCacheHelper.getRawURL(id);
        return new RawURLRes(url.startsWith("file://") ? null : url);
    }

    @Error(FileCacheHelper.IdNotFoundException.class)
    public HttpResponse<?> idNotFound(FileCacheHelper.IdNotFoundException e) {
        return HttpResponse.notFound(Ok.ok(e));
    }

    @Get("/decompress/{id:^[0-9]+$}")
    @RolesAllowed(Roles.API_Download)
    public RequestIDBean decompress(long id) {
        return asyncHelper.runIOTask(UUID.nameUUIDFromBytes(("decompress-" + id).getBytes()), () -> {
            try {
                var cachePath = fileCacheHelper.getCachePath(id);
                if (!Files.exists(cachePath.getParent())) {
                    Files.createDirectories(cachePath.getParent());
                }
                if (!Files.exists(cachePath) || Files.size(cachePath) == 0) {
                    try (var outputStream = new BufferedOutputStream(Files.newOutputStream(cachePath));
                         var response = fileCacheHelper.tryCacheFile(id)) {
                        log.info("Downloading {} for decompressing", cachePath);
                        response.bodyInputStream().transferTo(outputStream);
                    }
                }
                fileCacheHelper.decompress(id);
                return HttpResponse.ok(fileCacheHelper.getDirManifest(fileCacheHelper.getDecompressedPath(id), true));
            } catch (FileCacheHelper.IdNotFoundException e) {
                return HttpResponse.notFound(Ok.ok(e));
            } catch (Exception e) {
                return HttpResponse.serverError(Ok.ok(e));
            }
        });
    }
}

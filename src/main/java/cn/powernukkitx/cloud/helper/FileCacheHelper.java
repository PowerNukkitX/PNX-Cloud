package cn.powernukkitx.cloud.helper;

import cn.powernukkitx.cloud.bean.DownloadIDBean;
import cn.powernukkitx.cloud.bean.FileInfoBean;
import cn.powernukkitx.cloud.util.*;
import io.micronaut.cache.annotation.CacheConfig;
import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Singleton;
import okhttp3.Response;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Singleton
@CacheConfig(value = "file-cache", keyGenerator = PrefixedKeyGenerator.class)
public class FileCacheHelper {
    public static final Path CACHE_DIR = Path.of("./data/cache");

    private final DBHelper db;
    private final GitHubHelper gh;

    protected FileCacheHelper(@NotNull DBHelper db, @NotNull GitHubHelper gh) {
        // mkdir
        if (!Files.exists(CACHE_DIR)) {
            try {
                Files.createDirectories(CACHE_DIR);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.db = db;
        this.gh = gh;
    }

    public @NotNull Path getCachePath(long id) throws IdNotFoundException {
        var result = db.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id));
        if (result.size() == 0) throw new IdNotFoundException(id);
        return getCachePath(result.firstOrDefault());
    }

    @SuppressWarnings("HttpUrlsUsage")
    public @NotNull Path getCachePath(@NotNull DownloadIDBean bean) {
        if (bean.getSavePath() != null) {
            var savePath = bean.getSavePath();
            if (!savePath.isBlank()) return Path.of(savePath);
        }
        if (bean.getUrl().startsWith("file://")) {
            var pathStr = bean.getUrl().substring(7);
            if (EnumOS.getOs() == EnumOS.WINDOWS) {
                if (pathStr.startsWith("/")) {
                    pathStr = pathStr.substring(1);
                }
                pathStr = pathStr.replace("/", "\\");
            }
            return Path.of(pathStr);
        }
        var savePath = CACHE_DIR.resolve(bean.getUrl()
                .replace("https://", "")
                .replace("http://", "")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "/")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "/")
        );
        bean.setSavePath(savePath.toString());
        db.getDownloadIDBeanObjectRepository().update(bean);
        return savePath;
    }

    public @NotNull Path resetExtNameOfCachePath(long id, @NotNull String extName) throws IdNotFoundException, IOException {
        var result = db.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id));
        if (result.size() == 0) throw new IdNotFoundException(id);
        Path oldPath;
        var bean = result.firstOrDefault();
        if (bean.getSavePath() == null) {
            oldPath = getCachePath(bean);
        } else {
            oldPath = Path.of(bean.getSavePath());
        }
        var fileName = oldPath.getFileName().toString();
        if (fileName.contains(".")) {
            fileName = StringUtil.beforeFirst(fileName, ".");
        }
        fileName += "." + extName;
        var newPath = oldPath.getParent().resolve(fileName);
        if (newPath.equals(oldPath)) return newPath;
        bean.setSavePath(newPath.toString());
        db.getDownloadIDBeanObjectRepository().update(bean);
        if (Files.exists(oldPath)) {
            if (Files.exists(newPath)) {
                Files.delete(newPath);
            }
            Files.move(oldPath, newPath);
        }
        return newPath;
    }

    public boolean isFileCached(long id) throws IdNotFoundException, IOException {
        var path = getCachePath(id);
        return Files.exists(path) && !FileCachedInputStream.isCaching(path.toFile()) && Files.size(path) != 0;
    }

    public record TryCacheFileResult(InputStream bodyInputStream, long size,
                                     @Nullable Response rawResponse) implements Closeable {
        @Override
        public void close() {
            if (rawResponse != null) {
                rawResponse.close();
            }
        }
    }

    public @NotNull TryCacheFileResult tryCacheFile(long id) throws IdNotFoundException, IOException {
        var path = getCachePath(id);
        if (Files.exists(path) && Files.size(path) != 0)
            return new TryCacheFileResult(Files.newInputStream(path), Files.size(path), null);
        var dirPath = path.getParent();
        if (!Files.exists(dirPath)) Files.createDirectories(dirPath);
        var url = db.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault().getUrl();
        var client = HttpUtil.client;
        var reqBuilder = new okhttp3.Request.Builder()
                .url(url);
        if (url.contains("github")) {
            reqBuilder.addHeader("Authorization", gh.getEncodedAuthorization());
        }
        var request = reqBuilder.build();
        var response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response + ", body: " +
                NullUtil.tryDo(response.body(), body -> {
                    try {
                        return body.string();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, "null"));
        if (response.body() == null) throw new IOException("Response body is null");
        var size = response.body().contentLength();
        return new TryCacheFileResult(response.body().byteStream(), size, response);
    }

    @Cacheable
    @CacheKeyPrefix
    public @NotNull String getRawURL(long id) throws IdNotFoundException {
        var tmp = db.getDownloadIDBeanObjectRepository().find(ObjectFilters.eq("id", id)).firstOrDefault();
        if (tmp == null) throw new IdNotFoundException(id);
        return tmp.getUrl();
    }

    public boolean isCompressedFile(Path path) throws IOException {
        if (!Files.exists(path)) return false;
        if (Files.isDirectory(path)) return false;
        if (Files.size(path) == 0) return false;
        var pathString = path.toString();
        if (pathString.endsWith("zip")) return true;
        if (pathString.endsWith("jar")) return true;
        return pathString.endsWith("tar.gz");
    }

    public @NotNull Path getDecompressedPath(long id) throws IdNotFoundException {
        var path = getCachePath(id);
        var pathStr = path.getFileName().toString();
        if (pathStr.contains(".")) {
            pathStr = StringUtil.beforeLast(pathStr, ".") + "_decompress-cache";
        } else {
            pathStr += "_decompress-cache";
        }
        return path.resolveSibling(pathStr);
    }

    public boolean isDecompressed(long id) throws IdNotFoundException {
        var path = getCachePath(id);
        return Files.exists(path) && Files.exists(getDecompressedPath(id));
    }

    public void decompress(long id) throws IdNotFoundException, IOException {
        if (isDecompressed(id)) {
            return;
        }
        var path = getCachePath(id);
        if (!Files.exists(path)) throw new FileNotFoundException(path.toString());
        if (isCompressedFile(path)) {
            ZipUtil.decompress(path, getDecompressedPath(id));
        }
    }

    @Cacheable(parameters = "path")
    @CacheKeyPrefix
    public Map<String, FileInfoBean.NoSecretFileInfoBean> getDirManifest(Path path, boolean generateDownloadID) throws IOException {
        if (!Files.exists(path)) throw new FileNotFoundException(path.toString());
        if (!Files.isDirectory(path)) throw new IOException(path + " is not a directory");
        var objectRepo = db.getFileInfoBeanObjectRepository();
        var result = new HashMap<String, FileInfoBean.NoSecretFileInfoBean>();
        try (var s = Files.walk(path)) {
            s.forEach(p -> {
                if (!Files.isRegularFile(p)) return;
                var relativePath = path.relativize(p);
                FileInfoBean bean;
                var old = objectRepo.find(ObjectFilters.eq("path",
                        p.toAbsolutePath().toString().replace('\\', '/'))).firstOrDefault();
                if (old != null) {
                    bean = old;
                } else {
                    try {
                        bean = new FileInfoBean(path, p);
                        if (generateDownloadID) {
                            var fileURL = "file://" + (EnumOS.getOs() == EnumOS.WINDOWS ? "/" : "")
                                    + p.toAbsolutePath().toString().replace('\\', '/');
                            var downloadID = DownloadIDBean.getDownloadId(fileURL, db.getDownloadIDBeanObjectRepository());
                            bean.setDownloadID(downloadID);
                        } else {
                            bean.setDownloadID(-1);
                        }
                        objectRepo.insert(bean);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                result.put(relativePath.toString().replace('\\', '/'), bean.toNoSecretFileInfoBean());
            });
        }
        return result;
    }

    public static final class IdNotFoundException extends Exception {
        public final long id;

        public IdNotFoundException(long id) {
            super("ID " + id + " not found.");
            this.id = id;
        }
    }
}

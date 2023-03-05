package cn.powernukkitx.cloud.helper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public class DelayedResponseHelper {
    public final Cache<String, HttpResponse<?>> responseCache;

    public DelayedResponseHelper() {
        this.responseCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    public HttpResponse<?> get(@NotNull UUID uuid) {
        return responseCache.getIfPresent(uuid.toString());
    }

    public void put(@NotNull UUID uuid, HttpResponse<?> response) {
        responseCache.put(uuid.toString(), response);
    }

    public void remove(@NotNull UUID uuid) {
        responseCache.invalidate(uuid.toString());
    }
}

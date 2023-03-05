package cn.powernukkitx.cloud.helper;

import cn.powernukkitx.cloud.bean.RequestIDBean;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Singleton
public final class AsyncHelper {
    private static final int IO_THREADS = Runtime.getRuntime().availableProcessors() * 4;

    public static final ExecutorService IO_EXECUTOR = Executors.newWorkStealingPool(IO_THREADS);

    private final DelayedResponseHelper delayedResponseHelper;

    AsyncHelper(@NotNull DelayedResponseHelper delayedResponseHelper) {
        this.delayedResponseHelper = delayedResponseHelper;
    }

    public static @NotNull CompletableFuture<?> runIOTask(Runnable runnable) {
        var future = IO_EXECUTOR.submit(runnable);
        CompletableFuture<?> completableFuture;
        if (future instanceof CompletableFuture) {
            completableFuture = (CompletableFuture<?>) future;
        } else {
            completableFuture = CompletableFuture.runAsync(() -> {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, IO_EXECUTOR);
        }
        return completableFuture;
    }

    @Contract("_, _ -> new")
    public @NotNull RequestIDBean runIOTask(UUID uuid, Supplier<HttpResponse<?>> requestHandler) {
        IO_EXECUTOR.execute(() -> {
            var result = requestHandler.get();
            delayedResponseHelper.put(uuid, result);
        });
        return new RequestIDBean(uuid);
    }
}

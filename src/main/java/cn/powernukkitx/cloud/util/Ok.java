package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Ok<T extends Throwable>(@Nullable T throwable, @NotNull String message) {
    public static final Ok<Throwable> OK = new Ok<>(null, "OK");

    private Ok(T throwable) {
        this(throwable, throwable.getClass().getName() + ": " + throwable.getMessage());
    }

    @Contract("_ -> new")
    public static <T extends Throwable> @NotNull Ok<T> ok(T throwable) {
        return new Ok<>(throwable);
    }

    public boolean isOk() {
        return throwable == null;
    }

    public boolean isFailed() {
        return throwable != null;
    }

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @NotNull String getMessage() {
        return message;
    }
}

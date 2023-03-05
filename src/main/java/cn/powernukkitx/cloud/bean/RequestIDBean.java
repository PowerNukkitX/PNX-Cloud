package cn.powernukkitx.cloud.bean;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RequestIDBean(@NotNull String uuid, long acceptTime) {
    public RequestIDBean(@NotNull UUID uuid) {
        this(uuid.toString(), System.currentTimeMillis());
    }
}

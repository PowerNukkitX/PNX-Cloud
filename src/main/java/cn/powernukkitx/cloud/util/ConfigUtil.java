package cn.powernukkitx.cloud.util;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigUtil {
    private ConfigUtil() {
    }

    public static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static void init(String @NotNull ... paths) {
        var sb = new StringBuilder();
        for (var path : paths) {
            if (Files.exists(Path.of(path))) {
                sb.append(path).append(",");
            }
        }
        if (System.getenv().containsKey("micronaut.config.files")) {
            System.setProperty("micronaut.config.files", sb + "classpath:application.yml," + System.getenv("micronaut.config.files"));
        } else {
            System.setProperty("micronaut.config.files", sb + "classpath:application.yml");
        }
    }
}

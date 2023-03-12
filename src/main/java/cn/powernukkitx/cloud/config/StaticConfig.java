package cn.powernukkitx.cloud.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import lombok.Data;

import java.util.Map;

@ConfigurationProperties("static")
@Data
public class StaticConfig {
    String defaultReferer;

    @EachProperty("redirect")
    @Data
    public static class RedirectConfig {
        String to;
        int status;
    }

    Map<String, RedirectConfig> redirect;
}

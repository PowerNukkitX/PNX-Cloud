package cn.powernukkitx.cloud.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import jakarta.inject.Named;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@ConfigurationProperties("secret")
@Data
public class SecretConfig {
    @ConfigurationProperties("cdn_auth_header")
    @Data
    public static class CDNAuthHeader {
        String key;
        String value;
    }

    CDNAuthHeader cdnAuthHeader;

    @EachProperty("tokens")
    @Data
    public static class TokenConfig {
        String token;
        List<String> roles;
    }

    LinkedHashMap<String, TokenConfig> tokens;
}

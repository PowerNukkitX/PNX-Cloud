package cn.powernukkitx.cloud.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.Map;

@ConfigurationProperties("search")
@Data
public class SearchConfig {
    Map<String, String[]> synonyms;
}

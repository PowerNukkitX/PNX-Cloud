package cn.powernukkitx.cloud.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Property;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("github")
@Data
public class GHConfig {
    @Property(name = "github.auth.app-id")
    int appId;
    @Property(name = "github.auth.installation-id")
    int installationId;
    @Property(name = "github.auth.private-key-path")
    Path privateKeyPath;
    @Property(name = "github.config.organization")
    String organization;
    @Property(name = "github.config.repo")
    String repoName;
    @Property(name = "github.config.other-repos")
    List<String> otherRepos;

    @NotNull
    public List<String> getOtherRepos() {
        if (otherRepos == null) {
            otherRepos = List.of();
        }
        return otherRepos;
    }
}

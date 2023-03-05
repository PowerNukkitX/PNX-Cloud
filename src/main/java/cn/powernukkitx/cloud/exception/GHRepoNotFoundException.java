package cn.powernukkitx.cloud.exception;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public final class GHRepoNotFoundException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = -7732381145243187733L;

    @Getter
    private final String repoName;

    public GHRepoNotFoundException(@NotNull String repoName) {
        super("The GitHub repository " + repoName + " was not found");
        this.repoName = repoName;
    }
}

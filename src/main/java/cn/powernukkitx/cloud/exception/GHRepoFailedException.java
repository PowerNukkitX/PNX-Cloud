package cn.powernukkitx.cloud.exception;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public final class GHRepoFailedException extends IllegalArgumentException {
    @Serial
    private static final long serialVersionUID = 4888435798986719229L;

    @Getter
    private final String repoName;

    public GHRepoFailedException(@NotNull String repoName) {
        super("Failed to get GitHub repository \"" + repoName + "\".");
        this.repoName = repoName;
    }
}

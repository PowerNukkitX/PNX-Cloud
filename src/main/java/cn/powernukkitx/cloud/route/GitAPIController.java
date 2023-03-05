package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.bean.DownloadIDBean;
import cn.powernukkitx.cloud.exception.GHRepoFailedException;
import cn.powernukkitx.cloud.exception.GHRepoNotFoundException;
import cn.powernukkitx.cloud.helper.DBHelper;
import cn.powernukkitx.cloud.helper.GitHubHelper;
import cn.powernukkitx.cloud.task.ConfigTimedTask;
import cn.powernukkitx.cloud.util.Ok;
import cn.powernukkitx.cloud.util.StringUtil;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHArtifact;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHIssueState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static cn.powernukkitx.cloud.util.Ok.ok;

@Controller("/api/git")
@RolesAllowed("API/GIT")
public class GitAPIController {
    private static final Date FUTURE = new Date(Long.MAX_VALUE);
    private final GitHubHelper gh;
    private final DBHelper db;
    private final ConfigTimedTask configTimedTask;

    public GitAPIController(@NotNull GitHubHelper gh,
                            @NotNull DBHelper db,
                            @NotNull ConfigTimedTask ct) {
        this.gh = gh;
        this.db = db;
        this.configTimedTask = ct;
    }

    record StarRes(int stargazersCount) {
    }

    @Get("/star")
    public StarRes star() {
        return new StarRes(gh.getStarCached());
    }

    @Get("/star/{repo:[^/]+(/[^/]+)?}")
    public StarRes star(@NotNull String repo) {
        configTimedTask.toString();
        return new StarRes(gh.getStarCached(repo));
    }

    record IssueRes(int issueCount) {
    }

    @Get("/issue")
    public IssueRes issue() throws IOException {
        return issue(null, null);
    }

    @Get(uris = {"/issue/{repo:[^/]+(/[^/]+)?(?=/(all|open|closed))}/{status:all|open|closed}",
            "/issue/{repo:[^/]+(/[^/]+)?}"})
    public IssueRes issue(@Nullable String status, @Nullable String repo) throws IOException, GHRepoNotFoundException {
        var count = 0;
        if (repo != null) {
            switch (repo) {
                case "all", "open", "close" -> {
                    status = repo;
                    repo = null;
                }
            }
        }
        if (status == null || status.equals("all")) {
            count = gh.getIssueListCached(GHIssueState.ALL, repo).size();
        } else if (status.equals("open")) {
            count = gh.getOpenIssueCountCached(repo);
        } else if (status.equals("closed")) {
            count = gh.getIssueListCached(GHIssueState.CLOSED, repo).size();
        }
        return new IssueRes(count);
    }

    record ArtifactRes(String name, Date createAt, Date expiresAt, long sizeInBytes, long downloadId) {
        public ArtifactRes(@NotNull GHArtifact artifact, @NotNull DBHelper db) throws IOException {
            this(artifact.getName(), artifact.getCreatedAt(), artifact.getExpiresAt(), artifact.getSizeInBytes(),
                    DownloadIDBean.getDownloadId(artifact.getArchiveDownloadUrl(), db));
        }

        public ArtifactRes(@NotNull GHAsset asset, @NotNull DBHelper db) throws IOException {
            this(asset.getName(), asset.getCreatedAt(), FUTURE, asset.getSize(),
                    DownloadIDBean.getDownloadId(asset.getBrowserDownloadUrl(), db));
        }
    }

    record BuildArtifactsRes(ArtifactRes libs, ArtifactRes full, ArtifactRes core, ArtifactRes hashes) {
    }

    @Get(uris = {"/latest-build", "/latest-build/{repo:[^/]+(/[^/]+)?}"})
    public BuildArtifactsRes latestBuild(@Nullable String repo) throws IOException {
        var tmp = gh.getLatestBuildArtifactCached(repo).iterator();
        return new BuildArtifactsRes(
                new ArtifactRes(tmp.next(), db),
                new ArtifactRes(tmp.next(), db),
                new ArtifactRes(tmp.next(), db),
                new ArtifactRes(tmp.next(), db)
        );
    }

    record ReleaseRes(@JsonSerialize(nullsUsing = NullSerializer.class) String name,
                      @JsonSerialize(nullsUsing = NullSerializer.class) String tagName,
                      @JsonSerialize(nullsUsing = NullSerializer.class) String body,
                      @JsonSerialize(nullsUsing = NullSerializer.class) Date publishedAt,
                      @JsonSerialize(nullsUsing = NullSerializer.class) List<ArtifactRes> artifacts) {
    }

    @Get(uris = {"/latest-release", "/latest-release/{repo:[^/]+(/[^/]+)?}"})
    public ReleaseRes latestRelease(@Nullable String repo) throws IOException {
        var release = gh.getLatestReleaseCached(repo);
        if (release == null) {
            return new ReleaseRes(null, null, null, null, null);
        }
        var artifacts = new ArrayList<ArtifactRes>();
        for (var each : gh.getReleaseAssetsCached(release)) {
            artifacts.add(new ArtifactRes(each, db));
        }
        return new ReleaseRes(release.getName(), release.getTagName(), release.getBody(), release.getPublished_at(), artifacts);
    }

    @Get(uris = {"/all-releases", "/all-releases/{repo:[^/]+(/[^/]+)?}"})
    public List<ReleaseRes> allReleases(@Nullable String repo) throws IOException {
        var releases = gh.getReleasesCached(repo);
        var res = new ArrayList<ReleaseRes>();
        for (var release : releases) {
            var artifacts = new ArrayList<ArtifactRes>();
            for (var each : gh.getReleaseAssetsCached(release)) {
                artifacts.add(new ArtifactRes(each, db));
            }
            res.add(new ReleaseRes(release.getName(), release.getTagName(), release.getBody(), release.getPublished_at(), artifacts));
        }
        return res;
    }

    record ReadMeRes(@JsonSerialize(nullsUsing = NullSerializer.class) String format,
                     @JsonSerialize(nullsUsing = NullSerializer.class) String content) {
    }

    @Get("/readme/{repo:[^/]+(/[^/]+)?}")
    public ReadMeRes readme(@NotNull String repo) throws IOException {
        var readme = gh.getReadmeCached(repo);
        if (readme.fileName() == null) {
            return new ReadMeRes(null, null);
        }
        var format = switch (StringUtil.afterLast(readme.fileName(), ".")) {
            case "md" -> "Markdown";
            case "html" -> "HTML";
            case "rst" -> "ReStructuredText";
            default -> "Text";
        };
        return new ReadMeRes(format, readme.content());
    }

    record IconRes(long downloadID) {
    }

    @Get("/icon/{repo:[^/]+(/[^/]+)?}")
    public IconRes icon(@NotNull String repo) throws IOException {
        return new IconRes(gh.getRepoIconDownloadIDCached(repo));
    }

    @Error(GHRepoNotFoundException.class)
    public HttpResponse<Ok<GHRepoNotFoundException>> handle(@NotNull GHRepoNotFoundException exception) {
        return HttpResponse.badRequest(ok(exception));
    }

    @Error(GHRepoFailedException.class)
    public HttpResponse<Ok<GHRepoFailedException>> handle(@NotNull GHRepoFailedException exception) {
        return HttpResponse.serverError(ok(exception));
    }
}

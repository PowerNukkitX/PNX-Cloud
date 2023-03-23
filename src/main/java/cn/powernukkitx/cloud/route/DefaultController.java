package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.config.StaticConfig;
import cn.powernukkitx.cloud.util.StringUtil;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.security.annotation.Secured;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;

@Controller
@Secured("DOWNLOAD/STATIC")
public class DefaultController {
    private final StaticConfig staticConfig;

    public DefaultController(@NotNull StaticConfig staticConfig) {
        this.staticConfig = staticConfig;
    }

    @Get
    public SystemFile index() {
        return new SystemFile(new File("data/static/index.html"));
    }

    @Get(uri = "/{path:.+}")
    public HttpResponse<?> file(@Header("referer") @Nullable String referer,
                                @Header("if-none-match") @Nullable String ifNoneMatch,
                                String path) throws IOException {
        if (staticConfig.getRedirect() != null) {
            var redirect = staticConfig.getRedirect().get(path);
            if (redirect != null) {
                if (referer == null || referer.isBlank()) {
                    referer = staticConfig.getDefaultReferer();
                }
                if (!referer.endsWith("/")) {
                    referer += "/";
                }
                return HttpResponse.redirect(URI.create(referer + redirect.getTo())).status(redirect.getStatus());
            }
        }
        var file = new File("data/static/" + path);
        if (file.exists()) {
            if (file.isDirectory()) {
                file = new File(file, "index.html");
            }
            if (file.exists()) {
                return responseFile(new SystemFile(file), ifNoneMatch);
            } else {
                throw new IOException(path + " not found");
            }
        } else {
            var indexPath = StringUtil.beforeFirst(path, "/");
            var tmpFile = new File("data/static/" + indexPath + "/index.html");
            if (tmpFile.exists()) {
                return responseFile(new SystemFile(tmpFile), ifNoneMatch);
            }
            throw new IOException(path + " not found");
        }
    }

    private MutableHttpResponse<?> responseFile(@NotNull SystemFile systemFile, @Nullable String ifNoneMatch) {
        var file = systemFile.getFile();
        var etag = file.lastModified() + ":" + file.length();
        if (etag.equals(ifNoneMatch)) {
            return HttpResponse.notModified();
        }
        return HttpResponse.ok(file)
                .header("ETag", etag);
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<?> notFound() {
        return HttpResponse.notFound("<h1>404 Not Found</h1></hr>")
                .contentType(MediaType.TEXT_HTML_TYPE);
    }

}

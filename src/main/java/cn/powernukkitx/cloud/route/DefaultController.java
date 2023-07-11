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
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@Secured("DOWNLOAD/STATIC")
public class DefaultController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultController.class);
    private final StaticConfig staticConfig;
    private String gtagContent = null;

    public DefaultController(@NotNull StaticConfig staticConfig) {
        this.staticConfig = staticConfig;
    }

    @Get
    public HttpResponse<?> index(@Header("if-none-match") @Nullable String ifNoneMatch) {
        return responseFile(new SystemFile(new File("data/static/index.html")), ifNoneMatch);
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
            // 如果是省略了/index.html的，尝试返回该目录下的index.html
            var indexPath = StringUtil.beforeLast(path, "/");
            var tmpFile = new File("data/static/" + indexPath + "/index.html");
            if (tmpFile.exists()) {
                return responseFile(new SystemFile(tmpFile), ifNoneMatch);
            }
            // 如果是省略了.html的，尝试补充了.html的文件
            var tmpFile2 = new File("data/static/" + path + ".html");
            if (tmpFile2.exists()) {
                return responseFile(new SystemFile(tmpFile2), ifNoneMatch);
            }
            // 否则失败
            throw new IOException(path + " not found");
        }
    }

    private MutableHttpResponse<?> responseFile(@NotNull SystemFile systemFile, @Nullable String ifNoneMatch) {
        var file = systemFile.getFile();
        var etag = "v1:" + file.lastModified() + ":" + file.length();
        if (etag.equals(ifNoneMatch)) {
            return HttpResponse.notModified();
        }
        if (systemFile.getFile().getName().endsWith(".html")) {
            try {
                if (gtagContent == null) {
                    gtagContent = "</head>\n" + Files.readString(Path.of("config", "gtag.txt"));
                }
                return HttpResponse.ok(Files.readString(systemFile.getFile().toPath())
                                .replaceFirst("</head>", gtagContent))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .header("Cache-Control", "public")
                        .contentType(MediaType.TEXT_HTML_TYPE).header("ETag", etag);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return HttpResponse.ok(file).header("Cache-Control", "public").header("ETag", etag);
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<?> notFound() {
        return HttpResponse.notFound("<h1>404 Not Found</h1></hr>")
                .contentType(MediaType.TEXT_HTML_TYPE);
    }

}

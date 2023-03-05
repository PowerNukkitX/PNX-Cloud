package cn.powernukkitx.cloud.route;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.server.types.files.SystemFile;
import io.micronaut.security.annotation.Secured;

import java.io.File;
import java.io.IOException;

@Controller
@Secured("DOWNLOAD/STATIC")
public class DefaultController {
    @Get
    public SystemFile index() {
        return new SystemFile(new File("data/static/index.html"));
    }

    @Get(uri = "/{path:.+}")
    public SystemFile file(String path) throws IOException {
        var file = new File("data/static/" + path);
        if (file.exists()) {
            return new SystemFile(file);
        } else {
            throw new IOException(path + " not found");
        }
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse<?> notFound() {
        return HttpResponse.notFound("<h1>404 Not Found</h1></hr>")
                .contentType(MediaType.TEXT_HTML_TYPE);
    }

}

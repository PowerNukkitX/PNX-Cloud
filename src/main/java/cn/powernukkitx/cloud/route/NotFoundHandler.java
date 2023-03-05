package cn.powernukkitx.cloud.route;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.runtime.context.scope.ThreadLocal;

import java.io.IOException;

@ThreadLocal
@Produces("text/html")
@Requires(classes = {IOException.class, ExceptionHandler.class})
public class NotFoundHandler implements ExceptionHandler<IOException, HttpResponse<String>> {
    @Override
    public HttpResponse<String> handle(HttpRequest request, IOException exception) {
        return HttpResponse.notFound("<h1>404 Not Found</h1> <hr> /" + exception.getMessage())
                .contentType(MediaType.TEXT_HTML_TYPE);
    }
}

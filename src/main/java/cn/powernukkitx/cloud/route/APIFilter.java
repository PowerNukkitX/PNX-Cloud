package cn.powernukkitx.cloud.route;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;

@Filter("/api/**")
public class APIFilter implements HttpServerFilter {
    /**
     * check a response header, if there is no "Cache-Control" header, add it
     */
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers.map(chain.proceed(request), response -> {
            if (!response.getHeaders().contains("Cache-Control")) {
                response.header("Cache-Control", "no-cache");
            }
            return response;
        });
    }

    @Override
    public int getOrder() {
        return ServerFilterPhase.LAST.before();
    }
}

package cn.powernukkitx.cloud.auth;

import cn.powernukkitx.cloud.util.StringUtil;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.runtime.context.scope.ThreadLocal;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.filters.AuthenticationFetcher;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

@ThreadLocal
public class PNXAuthenticationFetcher implements AuthenticationFetcher {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(PNXAuthenticationFetcher.class);
    private final Authenticator authenticator;

    public PNXAuthenticationFetcher(@NotNull Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    @Override
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        var authenticationResponse = Flux.from(authenticator.authenticate(request, null));
        return authenticationResponse.switchMap(response -> {
            if (response.isAuthenticated() && response.getAuthentication().isPresent()) {
                log.info("Authenticated: {} @ {} for {}",
                        response.getAuthentication().get().getName(),
                        getRealIP(request),
                        request.getPath());
                return Flux.just(response.getAuthentication().get());
            } else {
                log.info("Error authenticating: {} @ {} for {}",
                        response.getMessage(),
                        getRealIP(request),
                        request.getPath());
                return Publishers.empty();
            }
        });
    }

    public static @NotNull String getRealIP(@NotNull HttpRequest<?> request) {
        String ip = request.getHeaders().get("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }
        if (ip.contains(",")) {
            ip = StringUtil.beforeFirst(ip, ",");
        }
        return ip;
    }
}

package cn.powernukkitx.cloud.auth;

import cn.powernukkitx.cloud.config.SecretConfig;
import io.micronaut.http.HttpRequest;
import io.micronaut.runtime.context.scope.ThreadLocal;
import io.micronaut.security.authentication.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

@ThreadLocal
@Slf4j
public class TokenAuthenticationProvider implements AuthenticationProvider, Roles {
    private final SecretConfig secretConfig;

    public TokenAuthenticationProvider(@NotNull SecretConfig secretConfig) {
        this.secretConfig = secretConfig;
    }

    /**
     * If the request is from cdn, which means there is
     * secretConfig.getCdnAuthHeader().getKey() + secretConfig.getCdnAuthHeader().getValue()
     * in header, then the user is authenticated, with "cdn" username.
     *
     * @param httpRequest           The http request
     * @param authenticationRequest The credentials to authenticate
     * @return The authentication response
     */
    @Override
    public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        return Mono.create(emitter -> {
            var token = httpRequest.getHeaders().findFirst("access-token");
            if (token.isEmpty()) {
                emitter.error(new AuthenticationException(new AuthenticationFailed()));
                return;
            }
            var tokenEntry = secretConfig.getTokens().entrySet().stream().filter(entry -> entry.getValue().getToken().equals(token.get())).findFirst();
            if (tokenEntry.isEmpty()) {
                emitter.error(new AuthenticationException(new AuthenticationFailed()));
                return;
            }
            var tokenConfig = tokenEntry.get().getValue();
            emitter.success(AuthenticationResponse.success(tokenEntry.get().getKey(), tokenConfig.getRoles()));
        });
    }
}

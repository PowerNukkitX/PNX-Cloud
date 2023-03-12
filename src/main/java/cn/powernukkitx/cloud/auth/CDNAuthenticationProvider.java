package cn.powernukkitx.cloud.auth;

import cn.powernukkitx.cloud.config.SecretConfig;
import io.micronaut.http.HttpRequest;
import io.micronaut.runtime.context.scope.ThreadLocal;
import io.micronaut.security.authentication.*;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

@ThreadLocal
public class CDNAuthenticationProvider implements AuthenticationProvider, Roles {
    private final SecretConfig secretConfig;

    public CDNAuthenticationProvider(@NotNull SecretConfig secretConfig) {
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
            // Deploy API
            var token = httpRequest.getHeaders().findFirst("access-token");
            if (token.isPresent()) {
                var tokenEntry = secretConfig.getTokens().entrySet().stream().filter(entry -> entry.getValue().getToken().equals(token.get())).findFirst();
                if (tokenEntry.isPresent()) {
                    var tokenConfig = tokenEntry.get().getValue();
                    emitter.success(AuthenticationResponse.success(tokenEntry.get().getKey(), tokenConfig.getRoles()));
                    return;
                }
            }
            // Standard CDN
            if (secretConfig.getCdnAuthHeader().getValue().equals(httpRequest.getHeaders().get(secretConfig.getCdnAuthHeader().getKey()))) {
                emitter.success(AuthenticationResponse.success("cdn", List.of(
                        Download_Static, Download_IdFile,
                        API_GIT, API_Download, API_Delayed_Response, API_Plugin)));
            } else {
                emitter.error(new AuthenticationException(new AuthenticationFailed()));
            }
        });
    }
}

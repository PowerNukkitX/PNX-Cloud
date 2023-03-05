package cn.powernukkitx.cloud.auth;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationException;
import io.micronaut.security.authentication.AuthenticationProvider;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

@Singleton
public class LocalHostAuthenticationProvider implements AuthenticationProvider, Roles {
    /**
     * If the request is from localhost, then the user is authenticated, with "admin" username.
     *
     * @param httpRequest           The http request
     * @param authenticationRequest The credentials to authenticate
     * @return The authentication response
     */
    @Override
    public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest, AuthenticationRequest<?, ?> authenticationRequest) {
        return Mono.create(emitter -> {
            if (httpRequest.getRemoteAddress().getAddress().isLoopbackAddress()) {
                emitter.success(AuthenticationResponse.success("admin", List.of(
                        Admin,
                        Deploy_Static,
                        Download_Static, Download_IdFile,
                        API_GIT, API_Download, API_Delayed_Response, API_Plugin
                )));
            } else {
                emitter.error(new AuthenticationException("Not authenticated"));
            }
        });
    }
}

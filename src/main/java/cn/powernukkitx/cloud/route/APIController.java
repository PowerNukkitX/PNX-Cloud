package cn.powernukkitx.cloud.route;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Produces;
import jakarta.annotation.security.PermitAll;

@Controller("/api")
@PermitAll
@Header(name = "Cache-Control", value = "no-cache")
public class APIController {
    @Get(uris = {"/", "/ping"})
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "pong";
    }

    @Get("/time")
    @Produces(MediaType.TEXT_PLAIN)
    public long time() {
        return System.currentTimeMillis();
    }
}

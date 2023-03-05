package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.auth.Roles;
import cn.powernukkitx.cloud.helper.DelayedResponseHelper;
import cn.powernukkitx.cloud.util.Ok;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.annotation.security.RolesAllowed;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Pattern;

@Controller("/api/delayed")
@RolesAllowed(Roles.API_Delayed_Response)
public class DelayedController {
    private final DelayedResponseHelper helper;

    public DelayedController(DelayedResponseHelper helper) {
        this.helper = helper;
    }

    public static final Pattern UUID__X_PATTERN_X = Pattern.compile("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    @Get("/query/{uuid}")
    public HttpResponse<?> get(@NotNull String uuid) {
        if (!UUID__X_PATTERN_X.matcher(uuid).matches()) {
            return HttpResponse.badRequest(Ok.ok(new IllegalArgumentException("Invalid UUID: " + uuid)));
        }
        var result = helper.get(UUID.fromString(uuid));
        if (result == null) {
            return HttpResponse.noContent();
        } else {
            helper.remove(UUID.fromString(uuid));
            return result;
        }
    }
}

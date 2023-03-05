package cn.powernukkitx.cloud.route;

import cn.powernukkitx.cloud.auth.Roles;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Put;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller("/deploy")
@Header(name = "Cache-Control", value = "no-cache")
@Slf4j
public class DeployController {
    record DeployRes(boolean success, @Nullable String failedReason) {
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Put("/static")
    @RolesAllowed(Roles.Deploy_Static)
    public HttpResponse<DeployRes> deployStatic(@Body byte[] body) {
        log.info("Deploying static files ({}KB)", String.format("%.2f", body.length / 1024.0));
        // check whether bodyData is a valid zip file
        // if not, return HttpResponse.badRequest(new DeployRes(false, "Invalid zip file"));
        // if yes, extract it to the static folder
        try (var zipInputStream = new ZipInputStream(new ByteArrayInputStream(body))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                var path = Path.of("data/static/", entry.getName());
                var dir = path.getParent();
                if (dir != null) {
                    if (!dir.toFile().exists()) {
                        dir.toFile().mkdirs();
                    }
                }
                if (entry.isDirectory()) {
                    path.toFile().mkdirs();
                } else {
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    try (var outputStream = Files.newOutputStream(path)) {
                        log.info("deploying {} ({}KB)", path, String.format("%.2f", entry.getSize() / 1024.0));
                        zipInputStream.transferTo(outputStream);
                    }
                }
                zipInputStream.closeEntry();
            }
            return HttpResponse.ok(new DeployRes(true, null));
        } catch (IOException e) {
            return HttpResponse.badRequest(new DeployRes(false, "Invalid zip file"));
        }
    }
}

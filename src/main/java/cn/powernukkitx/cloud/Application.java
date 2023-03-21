package cn.powernukkitx.cloud;

import cn.powernukkitx.cloud.util.ConfigUtil;
import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
        // Use UTF-8
        System.setProperty("file.encoding", "UTF-8");
        // Pass config files we need.
        ConfigUtil.init("config/github.yml", "config/server.yml", "config/secret.yml", "config/acme.yml",
                "config/static.yml", "config/search.yml");
        Micronaut.run(Application.class, args);
    }
}
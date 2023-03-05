package cn.powernukkitx.cloud.task;

import cn.powernukkitx.cloud.cmd.CmdDaemon;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;

@Singleton
public class StartDaemonTask implements ApplicationEventListener<ServerStartupEvent> {
    private final CmdDaemon cmdDaemon;

    public StartDaemonTask(CmdDaemon cmdDaemon) {
        this.cmdDaemon = cmdDaemon;
    }

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        cmdDaemon.start();
    }
}

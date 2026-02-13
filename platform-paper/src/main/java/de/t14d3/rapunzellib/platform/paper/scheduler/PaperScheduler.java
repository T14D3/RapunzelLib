package de.t14d3.rapunzellib.platform.paper.scheduler;

import de.t14d3.rapunzellib.platform.shared.scheduler.SharedSchedulerCore;
import de.t14d3.rapunzellib.platform.paper.PaperHandleBridge;
import net.minecraft.server.MinecraftServer;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class PaperScheduler extends SharedSchedulerCore {
    public PaperScheduler(Plugin plugin) {
        this(PaperHandleBridge.server(Objects.requireNonNull(plugin, "plugin")));
    }

    public PaperScheduler(MinecraftServer server) {
        super(server, "RapunzelLib-PaperScheduler");
    }
}

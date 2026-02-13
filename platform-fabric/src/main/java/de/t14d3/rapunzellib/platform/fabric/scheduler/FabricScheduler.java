package de.t14d3.rapunzellib.platform.fabric.scheduler;

import de.t14d3.rapunzellib.platform.shared.scheduler.SharedSchedulerCore;
import net.minecraft.server.MinecraftServer;

public final class FabricScheduler extends SharedSchedulerCore {
    public FabricScheduler(MinecraftServer server) {
        super(server, "RapunzelLib-FabricScheduler");
    }
}

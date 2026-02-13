package de.t14d3.rapunzellib.platform.neoforge.scheduler;

import de.t14d3.rapunzellib.platform.shared.scheduler.SharedSchedulerCore;
import net.minecraft.server.MinecraftServer;

public final class NeoForgeScheduler extends SharedSchedulerCore {
    public NeoForgeScheduler(MinecraftServer server) {
        super(server, "RapunzelLib-NeoForgeScheduler");
    }
}

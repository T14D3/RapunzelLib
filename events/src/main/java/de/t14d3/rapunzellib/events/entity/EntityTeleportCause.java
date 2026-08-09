package de.t14d3.rapunzellib.events.entity;

/**
 * Reasons an entity or player teleport may occur.
 *
 * <p>Faithful to the Bukkit {@code PlayerTeleportEvent.TeleportCause} set
 * (verified against Paper 26.2): {@link #COMMAND}, {@link #PLUGIN},
 * {@link #NETHER_PORTAL}, {@link #END_PORTAL}, {@link #END_GATEWAY},
 * {@link #SPECTATE}, {@link #CHORUS_FRUIT}, {@link #ENDER_PEARL},
 * {@link #CONSUMABLE_EFFECT}, {@link #DISMOUNT} and {@link #EXIT_BED} map
 * one-to-one to their Bukkit counterparts. {@link #UNKNOWN} is the fallback
 * for platforms without a cause concept (Fabric, Sponge) and for generic
 * entity teleports whose Bukkit event carries no cause
 * ({@code EntityTeleportEvent} has none - only the player variant does).</p>
 */
public enum EntityTeleportCause {
    COMMAND,
    PLUGIN,
    NETHER_PORTAL,
    END_PORTAL,
    END_GATEWAY,
    SPECTATE,
    CHORUS_FRUIT,
    ENDER_PEARL,
    CONSUMABLE_EFFECT,
    DISMOUNT,
    EXIT_BED,
    UNKNOWN,
}

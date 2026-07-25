package de.t14d3.rapunzellib.events.world;

/**
 * Identifies what kind of object caused an explosion, so consumers can
 * dereference {@link ExplosionPre#sourceTypeKey()} against the correct
 * registry.
 */
public enum ExplosionSourceKind {
    /** The explosion was caused by an entity (e.g. a creeper, ghast, or TNT ignited by an entity). */
    ENTITY,
    /** The explosion was caused by a block (e.g. a bed or respawn anchor detonating in an unsupported dimension). */
    BLOCK,
    /** The explosion was caused by something with no clear type identity (e.g. a plugin calling the explosion API directly). */
    OTHER
}

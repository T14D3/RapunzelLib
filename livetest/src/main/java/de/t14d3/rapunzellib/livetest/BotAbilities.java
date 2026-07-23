package de.t14d3.rapunzellib.livetest;

import org.jetbrains.annotations.NotNull;

/**
 * Snapshot of the bot's current player abilities (creative, flying, etc.).
 *
 * <p>Maps directly to the {@code ClientboundPlayerAbilitiesPacket} fields the
 * bot receives whenever its abilities change.</p>
 */
public record BotAbilities(boolean invincible,
                            boolean canFly,
                            boolean flying,
                            boolean creative,
                            float flySpeed,
                            float walkSpeed) {

    /** Sentinel used when abilities have not yet been announced to the bot. */
    public static final BotAbilities UNKNOWN = new BotAbilities(false, false, false, false, 0f, 0f);

    public boolean isUnknown() {
        return flySpeed == 0f && walkSpeed == 0f && !invincible && !canFly && !flying && !creative;
    }
}

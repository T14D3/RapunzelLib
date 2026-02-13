package de.t14d3.rapunzellib.commands;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class TestPaperCommandFeatureInstaller implements CommandFeatureInstaller {
    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public void install(@NotNull RapunzelContext context) {
        TestCommandFeatureInstallers.recordPaperInstall();
        CommandFeatureInstallerSupport.registerCommandSourceAdapter(
            context,
            PlatformId.PAPER,
            Object.class,
            source -> new TestCommandSource(source)
        );
    }

    private record TestCommandSource(@NotNull Object handle) implements RCommandSource {
        @Override
        public @NotNull Optional<de.t14d3.rapunzellib.objects.RPlayer> player() {
            return Optional.empty();
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull PlatformId platformId() {
            return PlatformId.PAPER;
        }
    }
}

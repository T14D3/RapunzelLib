package de.t14d3.rapunzellib.visuals;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.registry.RBlockType;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class VisualBuilder {
    private final VisualManager manager;

    VisualBuilder(@NotNull VisualManager manager) {
        this.manager = manager;
    }

    public @NotNull ParticleBuilder particles() {
        return new ParticleBuilder(manager);
    }

    public @NotNull BlockDisplayBuilder blockDisplay() {
        return new BlockDisplayBuilder(manager);
    }

    public @NotNull GlowOutlineBuilder glowOutline() {
        return new GlowOutlineBuilder(manager);
    }

    public @NotNull BeaconBeamBuilder beaconBeam() {
        return new BeaconBeamBuilder(manager);
    }

    public @NotNull BlockStructureBuilder blockStructure() {
        return new BlockStructureBuilder(manager);
    }

    public static final class ParticleBuilder {
        private final VisualManager manager;
        private ParticleShape shape = null;
        private TextColor color = NamedTextColor.WHITE;
        private double density = 1.0;
        private VisualAudience audience = VisualAudience.empty();
        private double viewDistance = -1;

        ParticleBuilder(@NotNull VisualManager manager) {
            this.manager = manager;
        }

        public @NotNull ParticleBuilder shape(@NotNull ParticleShape shape) {
            this.shape = shape;
            return this;
        }

        public @NotNull ParticleBuilder color(@NotNull TextColor color) {
            this.color = color;
            return this;
        }

        public @NotNull ParticleBuilder density(double density) {
            if (density <= 0 || density > 100) throw new IllegalArgumentException("density must be in (0, 100]");
            this.density = density;
            return this;
        }

        public @NotNull ParticleBuilder audience(@NotNull VisualAudience audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull ParticleBuilder viewDistance(double viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public @NotNull ParticleVisual build() {
            if (shape == null) throw new IllegalStateException("shape must be set");
            return manager.createParticle(new ParticleConfig(shape, color, density, viewDistance), audience);
        }
    }

    public static final class BlockDisplayBuilder {
        private final VisualManager manager;
        private RBlockType block;
        private DisplayTransform transform = DisplayTransform.identity();
        private RLocation location = null;
        private TextColor color = NamedTextColor.WHITE;
        private boolean glow = false;
        private VisualAudience audience = VisualAudience.empty();
        private double viewDistance = -1;

        BlockDisplayBuilder(@NotNull VisualManager manager) {
            this.manager = manager;
        }

        public @NotNull BlockDisplayBuilder block(@NotNull RBlockType block) {
            this.block = block;
            return this;
        }

        public @NotNull BlockDisplayBuilder transform(@NotNull DisplayTransform transform) {
            this.transform = transform;
            return this;
        }

        public @NotNull BlockDisplayBuilder location(@NotNull RLocation location) {
            this.location = location;
            return this;
        }

        public @NotNull BlockDisplayBuilder color(@NotNull TextColor color) {
            this.color = color;
            return this;
        }

        public @NotNull BlockDisplayBuilder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public @NotNull BlockDisplayBuilder audience(@NotNull VisualAudience audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull BlockDisplayBuilder viewDistance(double viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public @NotNull BlockDisplayVisual build() {
            if (block == null) throw new IllegalStateException("block must be set");
            if (location == null) throw new IllegalStateException("location must be set");
            return manager.createBlockDisplay(
                new BlockDisplayConfig(block, transform, color, glow, viewDistance),
                location,
                audience
            );
        }
    }

    public static final class GlowOutlineBuilder {
        private final VisualManager manager;
        private Set<RBlockPos> blocks = Set.of();
        private RBlockType outlineBlock;
        private TextColor color = NamedTextColor.GREEN;
        private VisualAudience audience = VisualAudience.empty();
        private double viewDistance = -1;

        GlowOutlineBuilder(@NotNull VisualManager manager) {
            this.manager = manager;
            this.outlineBlock = RBlockType.require("minecraft:tinted_glass");
        }

        public @NotNull GlowOutlineBuilder blocks(@NotNull Set<RBlockPos> blocks) {
            this.blocks = blocks;
            return this;
        }

        public @NotNull GlowOutlineBuilder outlineBlock(@NotNull RBlockType outlineBlock) {
            this.outlineBlock = outlineBlock;
            return this;
        }

        public @NotNull GlowOutlineBuilder color(@NotNull TextColor color) {
            this.color = color;
            return this;
        }

        public @NotNull GlowOutlineBuilder audience(@NotNull VisualAudience audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull GlowOutlineBuilder viewDistance(double viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public @NotNull GlowOutlineVisual build() {
            if (blocks.isEmpty()) throw new IllegalStateException("blocks must not be empty");
            return manager.createGlowOutline(
                new GlowOutlineConfig(blocks, outlineBlock, color, viewDistance),
                audience
            );
        }
    }

    public static final class BeaconBeamBuilder {
        private final VisualManager manager;
        private RLocation location = null;
        private TextColor color = DyeColor.WHITE.adventureColor();
        private int pyramidLevels = 0;
        private boolean extendToSky = false;
        private BeaconColorRenderer colorRenderer = null;
        private VisualAudience audience = VisualAudience.empty();
        private double viewDistance = -1;

        BeaconBeamBuilder(@NotNull VisualManager manager) {
            this.manager = manager;
        }

        public @NotNull BeaconBeamBuilder location(@NotNull RLocation location) {
            this.location = location;
            return this;
        }

        public @NotNull BeaconBeamBuilder color(@NotNull TextColor color) {
            this.color = color;
            return this;
        }

        public @NotNull BeaconBeamBuilder pyramidLevels(int pyramidLevels) {
            this.pyramidLevels = Math.max(0, Math.min(4, pyramidLevels));
            return this;
        }

        public @NotNull BeaconBeamBuilder extendToSky(boolean extendToSky) {
            this.extendToSky = extendToSky;
            return this;
        }

        public @NotNull BeaconBeamBuilder colorRenderer(BeaconColorRenderer colorRenderer) {
            this.colorRenderer = colorRenderer;
            return this;
        }

        public @NotNull BeaconBeamBuilder audience(@NotNull VisualAudience audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull BeaconBeamBuilder viewDistance(double viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public @NotNull BeaconBeamVisual build() {
            if (location == null) throw new IllegalStateException("location must be set");
            return manager.createBeaconBeam(
                new BeaconBeamConfig(location, color, pyramidLevels, extendToSky, colorRenderer, viewDistance),
                audience
            );
        }
    }

    public static final class BlockStructureBuilder {
        private final VisualManager manager;
        private BlockStructureShape shape = null;
        private TextColor color = NamedTextColor.RED;
        private boolean glow = true;
        private VisualAudience audience = VisualAudience.empty();
        private double viewDistance = -1;

        BlockStructureBuilder(@NotNull VisualManager manager) {
            this.manager = manager;
        }

        public @NotNull BlockStructureBuilder shape(@NotNull BlockStructureShape shape) {
            this.shape = shape;
            return this;
        }

        public @NotNull BlockStructureBuilder color(@NotNull TextColor color) {
            this.color = color;
            return this;
        }

        public @NotNull BlockStructureBuilder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public @NotNull BlockStructureBuilder audience(@NotNull VisualAudience audience) {
            this.audience = audience;
            return this;
        }

        public @NotNull BlockStructureBuilder viewDistance(double viewDistance) {
            this.viewDistance = viewDistance;
            return this;
        }

        public @NotNull BlockStructureVisual build() {
            if (shape == null) throw new IllegalStateException("shape must be set");
            return manager.createBlockStructure(
                new BlockStructureConfig(shape, color, glow, viewDistance),
                audience
            );
        }
    }
}

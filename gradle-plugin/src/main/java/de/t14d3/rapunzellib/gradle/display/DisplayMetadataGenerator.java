package de.t14d3.rapunzellib.gradle.display;

import de.t14d3.rapunzellib.gradle.extractor.EntityDataExtractor;
import org.gradle.api.GradleException;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DisplayMetadataGenerator extends EntityDataExtractor {
    private static final ConcurrentHashMap<List<String>, DisplayMetadataSpec> EXTRACTION_CACHE = new ConcurrentHashMap<>();

    private static final String ENTITY_CLASS = "net.minecraft.world.entity.Entity";
    private static final String DISPLAY_CLASS = "net.minecraft.world.entity.Display";
    private static final String BLOCK_DISPLAY_CLASS = "net.minecraft.world.entity.Display$BlockDisplay";

    private static final Map<String, Integer> KNOWN_BYTE_FLAGS = new LinkedHashMap<>();

    static {
        KNOWN_BYTE_FLAGS.put("FLAG_ONFIRE", 0);
        KNOWN_BYTE_FLAGS.put("FLAG_SHIFT_KEY_DOWN", 1);
        KNOWN_BYTE_FLAGS.put("FLAG_SPRINTING", 3);
        KNOWN_BYTE_FLAGS.put("FLAG_SWIMMING", 4);
        KNOWN_BYTE_FLAGS.put("FLAG_INVISIBLE", 5);
        KNOWN_BYTE_FLAGS.put("FLAG_GLOWING", 6);
        KNOWN_BYTE_FLAGS.put("FLAG_FALL_FLYING", 7);
    }

    private DisplayMetadataGenerator() {
    }

    public static DisplayMetadataSpec extractFromSource(List<File> classpath) {
        if (classpath.isEmpty()) {
            throw new GradleException("Display metadata extraction requires a non-empty Minecraft classpath.");
        }
        List<String> classpathKeys = classpath.stream().map(File::getAbsolutePath).sorted().toList();
        return EXTRACTION_CACHE.computeIfAbsent(classpathKeys, ignored -> doExtract(classpath));
    }

    private static DisplayMetadataSpec doExtract(List<File> classpath) {
        return withNativeRuntimeIsolation(() -> {
            try (URLClassLoader classLoader = isolatedClassLoader(classpath)) {
                bootstrapMinecraft(classLoader);

                Class<?> dataAccessorType = loadClass(DATA_ACCESSOR_CLASS, classLoader);
                Method accessorGetId = findAccessorIndexMethod(dataAccessorType);

                DisplayMetadataGenerator extractor = new DisplayMetadataGenerator();
                List<FieldSpec> fields = new ArrayList<>();

                Class<?> entityClass = loadClass(ENTITY_CLASS, classLoader);
                fields.addAll(extractor.extractFields(entityClass, dataAccessorType, accessorGetId));

                Class<?> displayClass = loadClass(DISPLAY_CLASS, classLoader);
                fields.addAll(extractor.extractFields(displayClass, dataAccessorType, accessorGetId));

                Class<?> blockDisplayClass = loadClass(BLOCK_DISPLAY_CLASS, classLoader);
                fields.addAll(extractor.extractFields(blockDisplayClass, dataAccessorType, accessorGetId));

                fields.sort(Comparator.comparingInt(FieldSpec::index));

                List<DisplayMetadataSpec.ByteFlagSpec> flags = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : KNOWN_BYTE_FLAGS.entrySet()) {
                    flags.add(new DisplayMetadataSpec.ByteFlagSpec(entry.getKey(), entry.getValue()));
                }

                return new DisplayMetadataSpec("block_display", entities(fields), flags);
            } catch (Exception ex) {
                if (ex instanceof GradleException ge) throw ge;
                throw new GradleException("Failed to extract display metadata from Minecraft classes.", ex);
            }
        });
    }

    private static List<DisplayFieldSpec> entities(List<FieldSpec> fields) {
        return fields.stream()
            .map(f -> new DisplayFieldSpec(f.name(), f.type(), f.index()))
            .toList();
    }

    public static String renderJavaSource(String packageName, String className, DisplayMetadataSpec spec) {
        StringBuilder source = new StringBuilder();

        source.append("package ").append(packageName).append(";\n\n");
        source.append("import net.minecraft.network.syncher.EntityDataSerializers;\n");
        source.append("import net.minecraft.network.syncher.SynchedEntityData;\n");
        source.append("import net.minecraft.world.level.block.state.BlockState;\n");
        source.append("import org.joml.Quaternionf;\n");
        source.append("import org.joml.Vector3f;\n");
        source.append("import java.util.List;\n");
        source.append("\n/**\n");
        source.append(" * Extracted from Minecraft classes at build time.\n");
        source.append(" * Field indices are read directly from {@code EntityDataAccessor} static fields.\n");
        source.append(" */\n");
        source.append("public final class ").append(className).append(" {\n\n");
        source.append("    // ── Field indices ────────────────────────────────────────\n\n");

        for (DisplayFieldSpec field : spec.fields()) {
            source.append("    public static final int ").append(field.name())
                .append(" = ").append(field.index()).append(";\n");
        }

        source.append("\n    // ── Byte flags ─────────────────────────────────────────\n\n");

        for (DisplayMetadataSpec.ByteFlagSpec flag : spec.byteFlags()) {
            source.append("    public static final byte ")
                .append(flag.name())
                .append(" = (byte) (1 << ")
                .append(flag.bit())
                .append(");\n");
        }

        source.append("\n    // ── Convenience wrappers ───────────────────────────────────\n\n");

        source.append("    public static byte sharedFlags(boolean glowing) {\n");
        source.append("        return glowing ? FLAG_GLOWING : 0;\n");
        source.append("    }\n\n");

        source.append("    public static ").append("SynchedEntityData.DataValue<?> sharedFlagsData(boolean glowing) {\n");
        source.append("        return new SynchedEntityData.DataValue<>(\n");
        source.append("            DATA_SHARED_FLAGS_ID, EntityDataSerializers.BYTE, sharedFlags(glowing)\n");
        source.append("        );\n");
        source.append("    }\n\n");

        boolean hasBlockState = spec.fields().stream().anyMatch(f -> f.name().equals("DATA_BLOCK_STATE_ID"));
        if (hasBlockState) {
            source.append("    public static ").append("SynchedEntityData.DataValue<?> blockStateData(BlockState state) {\n");
            source.append("        return new SynchedEntityData.DataValue<>(\n");
            source.append("            DATA_BLOCK_STATE_ID, EntityDataSerializers.BLOCK_STATE, state\n");
            source.append("        );\n");
            source.append("    }\n\n");
        }

        boolean hasGlowColor = spec.fields().stream().anyMatch(f ->
            f.name().equals("DATA_GLOW_COLOR_ID") || f.name().equals("DATA_GLOW_COLOR_OVERRIDE_ID")
        );
        if (hasGlowColor) {
            String glowFieldName = spec.fields().stream()
                .filter(f -> f.name().equals("DATA_GLOW_COLOR_ID") || f.name().equals("DATA_GLOW_COLOR_OVERRIDE_ID"))
                .findFirst().orElseThrow().name();

            source.append("    public static ").append("SynchedEntityData.DataValue<?> glowColorData(int color) {\n");
            source.append("        return new SynchedEntityData.DataValue<>(\n");
            source.append("            ").append(glowFieldName).append(", EntityDataSerializers.INT, color\n");
            source.append("        );\n");
            source.append("    }\n\n");
        }

        source.append("    public static ").append("List<SynchedEntityData.DataValue<?>> transformData(\n");
        source.append("            Vector3f translation, Vector3f scale,\n");
        source.append("            Quaternionf leftRotation, Quaternionf rightRotation) {\n");
        source.append("        return java.util.List.of(\n");
        source.append("            new SynchedEntityData.DataValue<>(DATA_TRANSLATION_ID, EntityDataSerializers.VECTOR3, translation),\n");
        source.append("            new SynchedEntityData.DataValue<>(DATA_SCALE_ID, EntityDataSerializers.VECTOR3, scale),\n");
        source.append("            new SynchedEntityData.DataValue<>(DATA_LEFT_ROTATION_ID, EntityDataSerializers.QUATERNION, leftRotation),\n");
        source.append("            new SynchedEntityData.DataValue<>(DATA_RIGHT_ROTATION_ID, EntityDataSerializers.QUATERNION, rightRotation)\n");
        source.append("        );\n");
        source.append("    }\n\n");

        source.append("    private ").append(className).append("() {\n");
        source.append("    }\n");
        source.append("}\n");

        return source.toString();
    }
}

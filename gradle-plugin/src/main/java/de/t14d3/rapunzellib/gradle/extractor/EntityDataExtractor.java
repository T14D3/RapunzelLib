package de.t14d3.rapunzellib.gradle.extractor;

import org.gradle.api.GradleException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base for extracting entity data metadata from Minecraft classes.
 * Subclasses define which entity classes to inspect and how to render output.
 */
public abstract class EntityDataExtractor {

    protected static final String DATA_ACCESSOR_CLASS = "net.minecraft.network.syncher.EntityDataAccessor";

    private static final Object LOG4J_CONFIGURATION_LOCK = new Object();

    private static final Map<String, String> TYPE_TO_SERIALIZER = new LinkedHashMap<>();

    static {
        TYPE_TO_SERIALIZER.put("org.joml.Vector3f", "VECTOR3");
        TYPE_TO_SERIALIZER.put("org.joml.Quaternionf", "QUATERNION");
        TYPE_TO_SERIALIZER.put("net.minecraft.world.level.block.state.BlockState", "BLOCK_STATE");
        TYPE_TO_SERIALIZER.put("int", "INT");
        TYPE_TO_SERIALIZER.put("java.lang.Integer", "INT");
        TYPE_TO_SERIALIZER.put("float", "FLOAT");
        TYPE_TO_SERIALIZER.put("java.lang.Float", "FLOAT");
        TYPE_TO_SERIALIZER.put("byte", "BYTE");
        TYPE_TO_SERIALIZER.put("java.lang.Byte", "BYTE");
        TYPE_TO_SERIALIZER.put("boolean", "BOOLEAN");
        TYPE_TO_SERIALIZER.put("java.lang.Boolean", "BOOLEAN");
        TYPE_TO_SERIALIZER.put("java.lang.String", "STRING");
        TYPE_TO_SERIALIZER.put("net.minecraft.network.chat.Component", "COMPONENT");
        TYPE_TO_SERIALIZER.put("net.minecraft.world.entity.Pose", "POSE");
        TYPE_TO_SERIALIZER.put("net.minecraft.world.item.ItemStack", "ITEM_STACK");
        TYPE_TO_SERIALIZER.put("java.util.Optional", "OPTIONAL");
    }

    protected EntityDataExtractor() {
    }

    /**
     * Extract all EntityDataAccessor fields from a Minecraft entity class.
     */
    protected List<FieldSpec> extractFields(Class<?> entityClass, Class<?> dataAccessorType, Method accessorGetId)
            throws Exception {
        List<FieldSpec> result = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (isEntityDataAccessorField(field, dataAccessorType)) {
                field.setAccessible(true);
                Object accessor = field.get(null);
                int index = (int) accessorGetId.invoke(accessor);
                String type = resolveSerializerType(field);
                result.add(new FieldSpec(field.getName(), type, index));
            }
        }
        return result;
    }

    private boolean isEntityDataAccessorField(Field field, Class<?> dataAccessorType) {
        int mod = field.getModifiers();
        return Modifier.isStatic(mod) && Modifier.isFinal(mod)
            && dataAccessorType.isAssignableFrom(field.getType());
    }

    private static String resolveSerializerType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0) {
                Type arg = typeArgs[0];
                String className = arg instanceof Class<?> c ? c.getName() : arg.getTypeName();
                String mapped = TYPE_TO_SERIALIZER.get(className);
                if (mapped != null) return mapped;
                // Try superclass for Optional<Something>
                if (className.startsWith("java.util.Optional<")) return "OPTIONAL";
                // Default to INT for unknown types
                return "INT";
            }
        }
        return "INT";
    }

    /**
     * Find the method to get the index from an EntityDataAccessor.
     * Tries multiple names across different mapping conventions.
     */
    protected static Method findAccessorIndexMethod(Class<?> dataAccessorType) {
        for (String name : List.of("getId", "getIndex", "id", "index")) {
            try {
                Method m = dataAccessorType.getMethod(name);
                if (m.getReturnType() == int.class || m.getReturnType() == Integer.class) {
                    return m;
                }
            } catch (NoSuchMethodException ignored) {
            }
        }
        // Fallback: try to read an int field named "id" or "index"
        for (Field f : dataAccessorType.getDeclaredFields()) {
            if (f.getType() == int.class || f.getType() == Integer.class) {
                f.setAccessible(true);
                try {
                    Object staticValue = f.get(null);
                    if (staticValue != null) {
                        return dataAccessorType.getDeclaredMethod("getId");
                    }
                } catch (Exception ignored) {
                }
            }
        }
        throw new GradleException("Cannot find index accessor method on " + dataAccessorType.getName()
            + ". Tried: getId, getIndex, id, index");
    }

    protected static void bootstrapMinecraft(ClassLoader classLoader) {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", true, classLoader);
            for (Method m : sharedConstantsClass.getMethods()) {
                if (m.getParameterCount() == 0
                    && ("tryDetectVersion".equals(m.getName()) || "detectVersion".equals(m.getName()))) {
                    m.invoke(null);
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Class<?> bootstrapClass = Class.forName("net.minecraft.server.Bootstrap", true, classLoader);
            for (Method m : bootstrapClass.getMethods()) {
                if (m.getParameterCount() == 0 && "bootstrap".equalsIgnoreCase(m.getName())) {
                    m.invoke(null);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    protected static Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new GradleException("Could not load Minecraft class '" + name + "'.", ex);
        }
    }

    protected static URLClassLoader isolatedClassLoader(List<File> classpath) {
        URL[] urls = classpath.stream().map(f -> {
            try {
                return f.toURI().toURL();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).toArray(URL[]::new);
        ClassLoader parent = ClassLoader.getPlatformClassLoader();
        return new URLClassLoader(urls, parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded != null) return loaded;
                    if (!name.startsWith("java.") && !name.startsWith("javax.")
                        && !name.startsWith("kotlin.") && !name.startsWith("org.gradle.")) {
                        try {
                            Class<?> found = findClass(name);
                            if (resolve) resolveClass(found);
                            return found;
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    return super.loadClass(name, resolve);
                }
            }
        };
    }

    protected static <T> T withNativeRuntimeIsolation(java.util.concurrent.Callable<T> action) {
        synchronized (LOG4J_CONFIGURATION_LOCK) {
            String originalLog4j1 = System.getProperty("log4j.configurationFile");
            String originalLog4j2 = System.getProperty("log4j2.configurationFile");
            try {
                var configFile = Files.createTempFile("rapunzellib-extractor-log4j", ".xml");
                Files.writeString(configFile, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Configuration status="OFF">
                      <Appenders><Console name="Console" target="SYSTEM_ERR">
                        <PatternLayout pattern="%m%n"/>
                      </Console></Appenders>
                      <Loggers><Root level="error">
                        <AppenderRef ref="Console"/>
                      </Root></Loggers>
                    </Configuration>
                    """);
                String configPath = configFile.toUri().toString();
                System.setProperty("log4j.configurationFile", configPath);
                System.setProperty("log4j2.configurationFile", configPath);
                try {
                    return action.call();
                } finally {
                    if (originalLog4j1 == null) System.clearProperty("log4j.configurationFile");
                    else System.setProperty("log4j.configurationFile", originalLog4j1);
                    if (originalLog4j2 == null) System.clearProperty("log4j2.configurationFile");
                    else System.setProperty("log4j2.configurationFile", originalLog4j2);
                    Files.deleteIfExists(configFile);
                    System.gc();
                }
            } catch (Exception ex) {
                throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
            }
        }
    }

    public record FieldSpec(String name, String type, int index) {
    }
}

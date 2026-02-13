package de.t14d3.rapunzellib.gradle.catalog;

import de.t14d3.rapunzellib.gradle.RegistryCatalogSourceType;
import org.gradle.api.GradleException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class RegistryCatalogSourceExtractor {
    private static final Pattern NATIVE_SYMBOL_PATTERN = Pattern.compile("[A-Z0-9_]+");
    private static final Object LOG4J_CONFIGURATION_LOCK = new Object();
    private static final ConcurrentHashMap<ExtractionRequest, ExtractedRegistryCatalog> EXTRACTION_CACHE = new ConcurrentHashMap<>();

    private RegistryCatalogSourceExtractor() {
    }

    public static ExtractedRegistryCatalog extract(
        String sourceType,
        List<File> nativeSourceClasspath,
        String normalizationProfile,
        String nativeEnumClassName,
        String nativeStaticFieldOwnerClassName,
        String nativeStaticFieldValueTypeName,
        List<String> nativeIncludePredicateMethods,
        List<String> nativeExcludePredicateMethods,
        String nativeKeyAccessorMethodName,
        Set<String> nativeExcludedEnumConstants
    ) {
        ExtractionRequest request = new ExtractionRequest(
            sourceType,
            nativeSourceClasspath.stream().map(File::getAbsolutePath).toList(),
            normalizationProfile,
            nativeEnumClassName,
            nativeStaticFieldOwnerClassName,
            nativeStaticFieldValueTypeName,
            List.copyOf(nativeIncludePredicateMethods),
            List.copyOf(nativeExcludePredicateMethods),
            nativeKeyAccessorMethodName,
            new java.util.TreeSet<>(nativeExcludedEnumConstants)
        );
        return EXTRACTION_CACHE.computeIfAbsent(request, ignored -> {
            ExtractedRegistryCatalog extracted = switch (sourceType) {
                case RegistryCatalogSourceType.NATIVE_ENUM -> extractNativeEnum(
                    nativeSourceClasspath,
                    nativeEnumClassName,
                    nativeIncludePredicateMethods,
                    nativeExcludePredicateMethods,
                    nativeKeyAccessorMethodName,
                    nativeExcludedEnumConstants
                );
                case RegistryCatalogSourceType.NATIVE_STATIC_FIELDS -> extractNativeStaticFields(
                    nativeSourceClasspath,
                    nativeStaticFieldOwnerClassName,
                    nativeStaticFieldValueTypeName,
                    nativeKeyAccessorMethodName
                );
                default -> throw new GradleException("Unsupported registry catalog source type '" + sourceType + "'.");
            };

            String normalizationDescription = RegistryCatalogKeyNormalizer.describe(normalizationProfile);
            if (normalizationDescription == null) {
                return extracted;
            }
            return new ExtractedRegistryCatalog(
                extracted.description() + "; " + normalizationDescription,
                RegistryCatalogKeyNormalizer.normalize(normalizationProfile, extracted.keys())
            );
        });
    }

    private static ExtractedRegistryCatalog extractNativeEnum(
        List<File> classpath,
        String enumClassName,
        List<String> includePredicateMethods,
        List<String> excludePredicateMethods,
        String keyAccessorMethodName,
        Set<String> excludedEnumConstants
    ) {
        if (classpath.isEmpty()) {
            throw new GradleException("Native registry catalog extraction requires a non-empty classpath.");
        }
        String trimmedEnumClassName = enumClassName.trim();
        if (trimmedEnumClassName.isEmpty()) {
            throw new GradleException("Native registry catalog extraction requires an enum class name.");
        }
        return withNativeRuntimeIsolation(() -> {
            try (URLClassLoader classLoader = isolatedClassLoader(classpath)) {
                bootstrapSharedRuntimeIfNeeded(classLoader, trimmedEnumClassName);
                Class<?> enumClass = loadClass(trimmedEnumClassName, classLoader, "Could not load native enum class '%s'.");
                if (!enumClass.isEnum()) {
                    throw new GradleException("Native registry catalog source '" + trimmedEnumClassName + "' is not an enum.");
                }

                LinkedHashSet<NamespacedKeyEntry> entries = new LinkedHashSet<>();
                for (Field field : enumClass.getDeclaredFields()) {
                    if (field.isEnumConstant() && !excludedEnumConstants.contains(field.getName())) {
                        entries.add(keyEntryFromNativeField(field.getName(), field, keyAccessorMethodName));
                    }
                }
                List<NamespacedKeyEntry> sorted = entries.stream()
                    .sorted(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path))
                    .toList();
                return new ExtractedRegistryCatalog(
                    buildDescription(trimmedEnumClassName, includePredicateMethods, excludePredicateMethods, excludedEnumConstants),
                    sorted
                );
            } catch (Exception ex) {
                if (ex instanceof GradleException gradleException) {
                    throw gradleException;
                }
                throw new GradleException("Failed native enum extraction for '" + trimmedEnumClassName + "'.", ex);
            }
        });
    }

    private static ExtractedRegistryCatalog extractNativeStaticFields(
        List<File> classpath,
        String ownerClassName,
        String valueTypeName,
        String keyAccessorMethodName
    ) {
        if (classpath.isEmpty()) {
            throw new GradleException("Native registry catalog extraction requires a non-empty classpath.");
        }
        if (ownerClassName.isBlank()) {
            throw new GradleException("Native static-field registry catalog extraction requires an owner class name.");
        }
        if (valueTypeName.isBlank()) {
            throw new GradleException("Native static-field registry catalog extraction requires a value type name.");
        }
        return withNativeRuntimeIsolation(() -> {
            try (URLClassLoader classLoader = isolatedClassLoader(classpath)) {
                bootstrapSharedRuntimeIfNeeded(classLoader, ownerClassName, valueTypeName);
                Class<?> ownerClass = loadClass(ownerClassName, classLoader, "Could not load native static-field owner '%s'.");
                Class<?> valueType = loadClass(valueTypeName, classLoader, "Could not load native static-field value type '%s'.");

                LinkedHashSet<NamespacedKeyEntry> entries = new LinkedHashSet<>();
                for (Field field : ownerClass.getFields()) {
                    if (Modifier.isPublic(field.getModifiers())
                        && Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())
                        && valueType.isAssignableFrom(field.getType())
                        && NATIVE_SYMBOL_PATTERN.matcher(field.getName()).matches()) {
                        entries.add(keyEntryFromNativeField(field.getName(), field, keyAccessorMethodName));
                    }
                }
                List<NamespacedKeyEntry> sorted = entries.stream()
                    .sorted(Comparator.comparing(NamespacedKeyEntry::namespace).thenComparing(NamespacedKeyEntry::path))
                    .toList();
                return new ExtractedRegistryCatalog(
                    ownerClassName + " native static fields assignable to " + valueTypeName,
                    sorted
                );
            } catch (Exception ex) {
                if (ex instanceof GradleException gradleException) {
                    throw gradleException;
                }
                throw new GradleException("Failed native static-field extraction for '" + ownerClassName + "'.", ex);
            }
        });
    }

    private static String buildDescription(
        String enumClassName,
        List<String> includePredicateMethods,
        List<String> excludePredicateMethods,
        Set<String> excludedEnumConstants
    ) {
        List<String> filters = new ArrayList<>();
        filters.addAll(includePredicateMethods);
        for (String method : excludePredicateMethods) {
            filters.add("!" + method);
        }
        if (!excludedEnumConstants.isEmpty()) {
            filters.add("exclude " + String.join(", ", new java.util.TreeSet<>(excludedEnumConstants)));
        }
        return filters.isEmpty()
            ? enumClassName + " native enum keys"
            : enumClassName + " native enum keys filtered by " + String.join(", ", filters);
    }

    private static NamespacedKeyEntry keyEntryFromNativeField(String symbolName, Field field, String keyAccessorMethodName) {
        if (keyAccessorMethodName.isBlank()) {
            return keyEntryFromNativeSymbol(symbolName);
        }
        try {
            Object nativeValue = field.get(null);
            if (nativeValue == null) {
                throw new IllegalArgumentException("Native value for '" + symbolName + "' must not be null.");
            }
            Object resolvedKey = invokeFirstMatchingNoArgMethodChain(nativeValue, keyAccessorMethodName);
            return parseNamespacedKeyEntry(resolvedKey.toString(), "native symbol '" + symbolName + "'");
        } catch (Exception ex) {
            throw new GradleException(
                "Could not resolve namespaced key for native symbol '" + symbolName + "' via '" + keyAccessorMethodName + "'.",
                ex
            );
        }
    }

    private static Object invokeFirstMatchingNoArgMethodChain(Object target, String accessorCandidates) throws Exception {
        List<String> candidates = java.util.Arrays.stream(accessorCandidates.split("\\|"))
            .map(String::trim)
            .filter(candidate -> !candidate.isBlank())
            .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one native key accessor candidate is required.");
        }

        Exception lastFailure = null;
        for (String candidate : candidates) {
            try {
                return invokeNoArgMethodChain(target, candidate);
            } catch (Exception ex) {
                if (lastFailure != null && lastFailure != ex) {
                    ex.addSuppressed(lastFailure);
                }
                lastFailure = ex;
            }
        }
        throw lastFailure != null ? lastFailure : new IllegalStateException("Native key accessor resolution failed without an exception.");
    }

    private static Object invokeNoArgMethodChain(Object target, String methodChain) throws Exception {
        Object current = target;
        for (String methodName : methodChain.split("\\.")) {
            if (methodName.isBlank()) {
                continue;
            }
            Method method;
            try {
                method = current.getClass().getMethod(methodName);
            } catch (NoSuchMethodException ex) {
                throw new GradleException("Native type '" + current.getClass().getName() + "' has no no-arg method '" + methodName + "'.", ex);
            }
            Object previous = current;
            current = method.invoke(current);
            if (current == null) {
                throw new IllegalArgumentException(
                    "Native key accessor '" + methodName + "' returned null for '" + previous.getClass().getName() + "'."
                );
            }
        }
        return current;
    }

    private static NamespacedKeyEntry keyEntryFromNativeSymbol(String symbolName) {
        return new NamespacedKeyEntry("minecraft", symbolName.toLowerCase(Locale.ROOT));
    }

    private static NamespacedKeyEntry parseNamespacedKeyEntry(String value, String label) {
        String trimmed = value.trim();
        int separatorIndex = trimmed.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == trimmed.length() - 1) {
            throw new GradleException("Invalid namespaced key '" + trimmed + "' from " + label + ".");
        }
        return new NamespacedKeyEntry(
            trimmed.substring(0, separatorIndex).toLowerCase(Locale.ROOT),
            trimmed.substring(separatorIndex + 1).toLowerCase(Locale.ROOT)
        );
    }

    private static URLClassLoader isolatedClassLoader(List<File> classpath) {
        URL[] urls = classpath.stream().map(file -> {
            try {
                return file.toURI().toURL();
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
                    if (loaded != null) {
                        return loaded;
                    }
                    if (!name.startsWith("java.")
                        && !name.startsWith("javax.")
                        && !name.startsWith("kotlin.")
                        && !name.startsWith("org.gradle.")) {
                        try {
                            Class<?> found = findClass(name);
                            if (resolve) {
                                resolveClass(found);
                            }
                            return found;
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                    return super.loadClass(name, resolve);
                }
            }
        };
    }

    private static <T> T withNativeRuntimeIsolation(ThrowingSupplier<T> action) {
        synchronized (LOG4J_CONFIGURATION_LOCK) {
            String originalLog4j1 = System.getProperty("log4j.configurationFile");
            String originalLog4j2 = System.getProperty("log4j2.configurationFile");
            try {
                var configFile = Files.createTempFile("rapunzellib-native-registry-log4j", ".xml");
                Files.writeString(
                    configFile,
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Configuration status="OFF">
                      <Appenders>
                        <Console name="Console" target="SYSTEM_ERR">
                          <PatternLayout pattern="%m%n"/>
                        </Console>
                      </Appenders>
                      <Loggers>
                        <Root level="error">
                          <AppenderRef ref="Console"/>
                        </Root>
                      </Loggers>
                    </Configuration>
                    """
                );
                String configPath = configFile.toUri().toString();
                System.setProperty("log4j.configurationFile", configPath);
                System.setProperty("log4j2.configurationFile", configPath);
                try {
                    return action.get();
                } finally {
                    restoreSystemProperty("log4j.configurationFile", originalLog4j1);
                    restoreSystemProperty("log4j2.configurationFile", originalLog4j2);
                    Files.deleteIfExists(configFile);
                    System.gc();
                }
            } catch (Exception ex) {
                throw ex instanceof RuntimeException runtimeException ? runtimeException : new RuntimeException(ex);
            }
        }
    }

    private static void restoreSystemProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private static void bootstrapSharedRuntimeIfNeeded(ClassLoader classLoader, String... classNames) {
        if (classNames.length == 0) {
            return;
        }

        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", true, classLoader);
            Method method = java.util.Arrays.stream(sharedConstantsClass.getMethods())
                .filter(candidate -> candidate.getParameterCount() == 0
                    && ("tryDetectVersion".equals(candidate.getName()) || "detectVersion".equals(candidate.getName())))
                .findFirst()
                .orElse(null);
            if (method != null) {
                method.invoke(null);
            }
        } catch (Throwable ignored) {
        }

        Class<?> bootstrapClass;
        try {
            bootstrapClass = Class.forName("net.minecraft.server.Bootstrap", true, classLoader);
        } catch (ClassNotFoundException ignored) {
            return;
        }

        try {
            Method bootstrap = java.util.Arrays.stream(bootstrapClass.getMethods())
                .filter(candidate -> candidate.getParameterCount() == 0
                    && ("bootStrap".equals(candidate.getName()) || "bootstrap".equals(candidate.getName())))
                .findFirst()
                .orElse(null);
            if (bootstrap != null) {
                bootstrap.invoke(null);
            }
            Method validate = java.util.Arrays.stream(bootstrapClass.getMethods())
                .filter(candidate -> candidate.getParameterCount() == 0 && "validate".equals(candidate.getName()))
                .findFirst()
                .orElse(null);
            if (validate != null) {
                validate.invoke(null);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader, String messageTemplate) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new GradleException(messageTemplate.formatted(className), ex);
        }
    }

    private record ExtractionRequest(
        String sourceType,
        List<String> nativeSourceClasspath,
        String normalizationProfile,
        String nativeEnumClassName,
        String nativeStaticFieldOwnerClassName,
        String nativeStaticFieldValueTypeName,
        List<String> nativeIncludePredicateMethods,
        List<String> nativeExcludePredicateMethods,
        String nativeKeyAccessorMethodName,
        Set<String> nativeExcludedEnumConstants
    ) {
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

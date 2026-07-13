package de.t14d3.rapunzellib.livetest;

public final class Assertions {
    private Assertions() {}

    public static void assertTrue(boolean condition) {
        if (!condition) throw new LiveTestAssertionError("Expected true but was false");
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) throw new LiveTestAssertionError(message);
    }

    public static void assertFalse(boolean condition) {
        if (condition) throw new LiveTestAssertionError("Expected false but was true");
    }

    public static void assertFalse(boolean condition, String message) {
        if (condition) throw new LiveTestAssertionError(message);
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new LiveTestAssertionError("Expected " + expected + " but was " + actual);
        }
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new LiveTestAssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        if (java.util.Objects.equals(unexpected, actual)) {
            throw new LiveTestAssertionError("Did not expect " + unexpected);
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual, String message) {
        if (java.util.Objects.equals(unexpected, actual)) {
            throw new LiveTestAssertionError(message + " (unexpected=" + unexpected + ")");
        }
    }

    public static void assertNotNull(Object obj) {
        if (obj == null) throw new LiveTestAssertionError("Expected non-null");
    }

    public static void assertNotNull(Object obj, String message) {
        if (obj == null) throw new LiveTestAssertionError(message);
    }

    public static void assertNull(Object obj) {
        if (obj != null) throw new LiveTestAssertionError("Expected null but was " + obj);
    }

    public static void assertNull(Object obj, String message) {
        if (obj != null) throw new LiveTestAssertionError(message);
    }

    public static void fail(String message) {
        throw new LiveTestAssertionError(message);
    }
}

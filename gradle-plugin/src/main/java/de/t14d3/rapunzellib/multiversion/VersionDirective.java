package de.t14d3.rapunzellib.multiversion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionDirective {
    public enum Type {
        IF,
        ELSEIF,
        ELSE,
        ENDIF
    }

    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile(
        "^\\s*//\\s*#(?<type>if|elseif|else|endif)\\s*(?<condition>.*)$"
    );
    
    private static final Pattern VERSION_COMPARE_PATTERN = Pattern.compile(
        "(?:(?<leftVersion>[0-9]+(?:\\.[0-9]+)*)\\s*)?(?<op>>=|<=|>|<|==|!=)?\\s*(?<rightVersion>[0-9]+(?:\\.[0-9]+)*)?"
    );
    
    private final Type type;
    private final String condition;
    
    private VersionDirective(Type type, String condition) {
        this.type = type;
        this.condition = condition;
    }
    
    public static VersionDirective parse(String line) {
        Matcher matcher = DIRECTIVE_PATTERN.matcher(line);
        if (matcher.matches()) {
            String condition = matcher.group("condition");
            return new VersionDirective(parseType(matcher.group("type")), condition != null ? condition.trim() : "");
        }
        return null;
    }
    
    public Type getType() {
        return type;
    }

    private static Type parseType(String type) {
        return switch (type) {
            case "if" -> Type.IF;
            case "elseif" -> Type.ELSEIF;
            case "else" -> Type.ELSE;
            case "endif" -> Type.ENDIF;
            default -> throw new IllegalArgumentException("Unknown directive type: " + type);
        };
    }

    public boolean evaluate(String currentVersion) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        
        // Handle VERSION prefix (e.g., "VERSION >= 1.21")
        String evalCondition = condition;
        if (condition.startsWith("VERSION")) {
            evalCondition = condition.substring("VERSION".length()).trim();
        } else if (condition.startsWith("MC")) {
            evalCondition = condition.substring("MC".length()).trim();
        }
        
        Matcher matcher = VERSION_COMPARE_PATTERN.matcher(evalCondition);
        if (matcher.matches()) {
            String leftVersion = matcher.group("leftVersion");
            String op = matcher.group("op");
            String rightVersion = matcher.group("rightVersion");
            
            // For expressions like "1.21 >= 1.20", leftVersion is the first version, rightVersion is the second
            // For expressions like ">= 1.21", leftVersion is null, rightVersion is the version
            // We want to compare currentVersion with the version specified in the condition
            
            String conditionVersion = rightVersion != null ? rightVersion : leftVersion;
            
            // Default to comparing with currentVersion if no version specified
            if (conditionVersion == null || conditionVersion.isEmpty()) {
                conditionVersion = currentVersion;
            }
            
            if (op == null) {
                // Just a version number (e.g., "1.21"), check equality with currentVersion
                return versionEquals(conditionVersion, currentVersion);
            }
            
            int cmp = compareVersions(currentVersion, conditionVersion);
            return switch (op) {
                case ">=" -> cmp >= 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case "<" -> cmp < 0;
                case "==" -> cmp == 0;
                case "!=" -> cmp != 0;
                default -> false;
            };
        }
        
        // Fallback: if it's just a version number, check equality
        return versionEquals(evalCondition, currentVersion);
    }
    
    private boolean versionEquals(String v1, String v2) {
        return normalizeVersion(v1).equals(normalizeVersion(v2));
    }
    
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLen = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLen; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (n1 != n2) {
                return Integer.compare(n1, n2);
            }
        }
        
        return 0;
    }
    
    private String normalizeVersion(String version) {
        version = version.trim();
        if (version.contains(".")) {
            return version;
        }
        
        if (version.length() > 3) {
            return version.substring(0, 1) + "." + version.substring(1, 2) + "." + version.substring(2);
        }
        
        return version;
    }
}

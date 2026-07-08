package net.mosspad.oldcombatmechanics.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads a world override first, then its inherited global default. Invalid world
 * overrides never erase a valid global setting: they fall back to that setting.
 */
final class ConfigReader {
    private final ConfigurationSection defaults;
    private final ConfigurationSection override;
    private final ConfigLoader.WarningSink warnings;
    private final String scope;
    private final String defaultsScope;

    ConfigReader(
            ConfigurationSection defaults,
            ConfigurationSection override,
            ConfigLoader.WarningSink warnings,
            String scope
    ) {
        this(defaults, override, warnings, scope, "defaults");
    }

    ConfigReader(
            ConfigurationSection defaults,
            ConfigurationSection override,
            ConfigLoader.WarningSink warnings,
            String scope,
            String defaultsScope
    ) {
        this.defaults = defaults;
        this.override = override;
        this.warnings = warnings;
        this.scope = scope;
        this.defaultsScope = defaultsScope;
    }

    boolean bool(String path, boolean builtInFallback) {
        if (hasOverride(path)) {
            Boolean parsed = parseBoolean(override.get(path));
            if (parsed != null) {
                return parsed;
            }
            warnings.warn(scope + "." + path + " must be true or false; using the inherited value.");
            return inheritedBoolean(path, builtInFallback);
        }
        return inheritedBoolean(path, builtInFallback);
    }

    int boundedInt(String path, int builtInFallback, int minimum, int maximum) {
        if (hasOverride(path)) {
            Integer parsed = parseInt(override.get(path), minimum, maximum);
            if (parsed != null) {
                return parsed;
            }
            warnings.warn(scope + "." + path + " must be a whole number between " + minimum + " and " + maximum
                    + "; using the inherited value.");
            return inheritedInt(path, builtInFallback, minimum, maximum);
        }
        return inheritedInt(path, builtInFallback, minimum, maximum);
    }

    double boundedDouble(String path, double builtInFallback, double minimum, double maximum) {
        if (hasOverride(path)) {
            Double parsed = parseDouble(override.get(path), minimum, maximum);
            if (parsed != null) {
                return parsed;
            }
            warnings.warn(scope + "." + path + " must be between " + minimum + " and " + maximum
                    + "; using the inherited value.");
            return inheritedDouble(path, builtInFallback, minimum, maximum);
        }
        return inheritedDouble(path, builtInFallback, minimum, maximum);
    }

    List<String> stringList(String path) {
        if (hasOverride(path)) {
            List<String> parsed = parseStringList(override.get(path), path);
            if (parsed != null) {
                return parsed;
            }
            warnings.warn(scope + "." + path + " must be a list; using the inherited value.");
            return inheritedStringList(path);
        }
        return inheritedStringList(path);
    }

    private boolean inheritedBoolean(String path, boolean builtInFallback) {
        if (!hasDefault(path)) {
            return builtInFallback;
        }
        Boolean parsed = parseBoolean(defaults.get(path));
        if (parsed != null) {
            return parsed;
        }
        warnings.warn(defaultsScope + "." + path + " must be true or false; using " + builtInFallback + ".");
        return builtInFallback;
    }

    private int inheritedInt(String path, int builtInFallback, int minimum, int maximum) {
        if (!hasDefault(path)) {
            return builtInFallback;
        }
        Integer parsed = parseInt(defaults.get(path), minimum, maximum);
        if (parsed != null) {
            return parsed;
        }
        warnings.warn(defaultsScope + "." + path + " must be a whole number between " + minimum + " and " + maximum
                + "; using " + builtInFallback + ".");
        return builtInFallback;
    }

    private double inheritedDouble(String path, double builtInFallback, double minimum, double maximum) {
        if (!hasDefault(path)) {
            return builtInFallback;
        }
        Double parsed = parseDouble(defaults.get(path), minimum, maximum);
        if (parsed != null) {
            return parsed;
        }
        warnings.warn(defaultsScope + "." + path + " must be between " + minimum + " and " + maximum
                + "; using " + builtInFallback + ".");
        return builtInFallback;
    }

    private List<String> inheritedStringList(String path) {
        if (!hasDefault(path)) {
            return List.of();
        }
        List<String> parsed = parseStringList(defaults.get(path), "defaults." + path);
        if (parsed != null) {
            return parsed;
        }
        warnings.warn(defaultsScope + "." + path + " must be a list; using an empty list.");
        return List.of();
    }

    private List<String> parseStringList(Object value, String path) {
        if (!(value instanceof List<?> values)) {
            return null;
        }

        List<String> strings = new ArrayList<>();
        for (Object entry : values) {
            if (entry instanceof String string) {
                strings.add(string);
            } else {
                warnings.warn(scopeLabel(path) + " contains a non-text entry; that entry was skipped.");
            }
        }
        return List.copyOf(strings);
    }

    private String scopeLabel(String path) {
        return path.startsWith(defaultsScope + ".") ? path : scope + "." + path;
    }

    private static Boolean parseBoolean(Object value) {
        return value instanceof Boolean booleanValue ? booleanValue : null;
    }

    private static Integer parseInt(Object value, int minimum, int maximum) {
        if (!(value instanceof Number number)) {
            return null;
        }
        double raw = number.doubleValue();
        if (!Double.isFinite(raw) || Math.rint(raw) != raw) {
            return null;
        }
        long parsed = number.longValue();
        if (parsed < minimum || parsed > maximum) {
            return null;
        }
        return (int) parsed;
    }

    private static Double parseDouble(Object value, double minimum, double maximum) {
        if (!(value instanceof Number number)) {
            return null;
        }
        double parsed = number.doubleValue();
        if (!Double.isFinite(parsed) || parsed < minimum || parsed > maximum) {
            return null;
        }
        return parsed;
    }

    private boolean hasOverride(String path) {
        return override != null && override.isSet(path);
    }

    private boolean hasDefault(String path) {
        return defaults != null && defaults.isSet(path);
    }
}

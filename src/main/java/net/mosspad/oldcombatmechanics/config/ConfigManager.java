package net.mosspad.oldcombatmechanics.config;

import net.mosspad.oldcombatmechanics.OldCombatMechanicsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.logging.Level;

/** Owns the current immutable configuration snapshot. */
public final class ConfigManager {
    private final OldCombatMechanicsPlugin plugin;
    private volatile Settings.PluginSettings settings;

    public ConfigManager(OldCombatMechanicsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Loads and validates a new configuration snapshot. On failure, an existing
     * snapshot remains active so a reload cannot leave the combat system half-updated.
     */
    public boolean reload() {
        try {
            plugin.reloadConfig();
            FileConfiguration configuration = plugin.getConfig();
            Settings.PluginSettings loaded = ConfigLoader.load(plugin.getLogger(), configuration);
            settings = loaded;
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not load OldCombatMechanics configuration; keeping the previous settings.", exception);
            return false;
        }
    }

    public Settings.PluginSettings settings() {
        Settings.PluginSettings current = settings;
        if (current == null) {
            throw new IllegalStateException("Configuration was requested before it was loaded.");
        }
        return current;
    }
}

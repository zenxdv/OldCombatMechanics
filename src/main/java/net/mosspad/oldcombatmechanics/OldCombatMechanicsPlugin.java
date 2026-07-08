package net.mosspad.oldcombatmechanics;

import net.mosspad.oldcombatmechanics.command.OldCombatCommand;
import net.mosspad.oldcombatmechanics.combat.AttackSpeedService;
import net.mosspad.oldcombatmechanics.combat.CombatListener;
import net.mosspad.oldcombatmechanics.combat.VelocityService;
import net.mosspad.oldcombatmechanics.config.ConfigManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point. Runtime combat code only reads cached immutable settings.
 */
public final class OldCombatMechanicsPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private AttackSpeedService attackSpeedService;
    private VelocityService velocityService;
    private long runtimeGeneration;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        runtimeGeneration = 1L;

        configManager = new ConfigManager(this);
        if (!configManager.reload()) {
            throw new IllegalStateException("Could not load OldCombatMechanics configuration.");
        }

        velocityService = new VelocityService(this);
        attackSpeedService = new AttackSpeedService(this, configManager);

        getServer().getPluginManager().registerEvents(attackSpeedService, this);
        getServer().getPluginManager().registerEvents(velocityService, this);
        getServer().getPluginManager().registerEvents(
                new CombatListener(this, configManager, velocityService), this
        );

        PluginCommand command = getCommand("oldcombat");
        if (command == null) {
            throw new IllegalStateException("The oldcombat command is missing from plugin.yml.");
        }
        OldCombatCommand oldCombatCommand = new OldCombatCommand(this, configManager);
        command.setExecutor(oldCombatCommand);
        command.setTabCompleter(oldCombatCommand);

        attackSpeedService.refreshAll();
        getLogger().info(() -> "Enabled OldCombatMechanics " + getDescription().getVersion()
                + " with cached, Bukkit-compatible combat settings.");
    }

    @Override
    public void onDisable() {
        runtimeGeneration++;
        if (velocityService != null) {
            velocityService.clear();
        }
        if (attackSpeedService != null) {
            attackSpeedService.removeFromAll();
        }
        getLogger().info("Disabled OldCombatMechanics and removed its attack-speed modifiers.");
    }

    /**
     * Reloads settings transactionally. A failed reload leaves the previous cached
     * configuration active instead of leaving players with partially applied state.
     *
     * @return {@code true} only when the new configuration was loaded and applied
     */
    public boolean reloadPlugin() {
        if (!configManager.reload()) {
            return false;
        }

        runtimeGeneration++;
        velocityService.clear();
        attackSpeedService.refreshAll();
        getLogger().info("Configuration reloaded and current world settings were applied to online players.");
        return true;
    }

    /** Returns the current lifecycle generation for short-lived delayed combat work. */
    public long runtimeGeneration() {
        return runtimeGeneration;
    }
}

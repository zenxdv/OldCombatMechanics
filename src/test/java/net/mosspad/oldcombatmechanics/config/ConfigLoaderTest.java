package net.mosspad.oldcombatmechanics.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    private static final Logger LOGGER = Logger.getLogger("OldCombatMechanicsTest");

    @Test
    void disabledWorldListWinsOverEnabledWorldList() {
        YamlConfiguration configuration = baseConfiguration();
        configuration.set("world-control.enabled-worlds", List.of("Practice", "Lobby"));
        configuration.set("world-control.disabled-worlds", List.of("lobby"));

        Settings.PluginSettings settings = ConfigLoader.load(LOGGER, configuration);

        assertTrue(settings.forWorld("practice").enabled());
        assertFalse(settings.forWorld("lobby").enabled());
        assertFalse(settings.forWorld("survival").enabled());
    }

    @Test
    void worldOverrideInheritsMissingDefaultValues() {
        YamlConfiguration configuration = baseConfiguration();
        configuration.set("defaults.melee.knockback.horizontal", 0.38D);
        configuration.set("defaults.melee.knockback.vertical", 0.36D);
        configuration.set("worlds.practice.melee.knockback.horizontal", 0.44D);

        Settings.WorldSettings practice = ConfigLoader.load(LOGGER, configuration).forWorld("practice");

        assertEquals(0.44D, practice.melee().knockback().horizontal());
        assertEquals(0.36D, practice.melee().knockback().vertical());
    }

    @Test
    void invalidWorldOverrideRetainsTheGlobalDefault() {
        YamlConfiguration configuration = baseConfiguration();
        configuration.set("defaults.melee.knockback.horizontal", 0.41D);
        configuration.set("worlds.practice.melee.knockback.horizontal", "not-a-number");

        Settings.WorldSettings practice = ConfigLoader.load(LOGGER, configuration).forWorld("practice");

        assertEquals(0.41D, practice.melee().knockback().horizontal());
    }

    @Test
    void invalidProjectileTypeIsSkippedWithoutDisablingProjectiles() {
        YamlConfiguration configuration = baseConfiguration();
        configuration.set("defaults.projectiles.allowed-types", List.of("ARROW", "NOT_A_PROJECTILE"));

        Settings.ProjectileSettings projectiles = ConfigLoader.load(LOGGER, configuration)
                .forWorld("practice")
                .projectiles();

        assertTrue(projectiles.enabled());
        assertTrue(projectiles.allowsType("ARROW"));
        assertFalse(projectiles.allowsType("SNOWBALL"));
    }

    private YamlConfiguration baseConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("settings.log-configuration-warnings", false);
        configuration.set("defaults.enabled", true);
        configuration.set("defaults.scope.player-only", true);
        configuration.set("defaults.scope.pve-enabled", false);
        configuration.set("defaults.fast-attack.enabled", true);
        configuration.set("defaults.fast-attack.attack-speed-bonus", 16.0D);
        configuration.set("defaults.melee.enabled", true);
        configuration.set("defaults.melee.require-positive-final-damage", true);
        configuration.set("defaults.melee.disable-sweeping-attacks", true);
        configuration.set("defaults.melee.damage.enabled", true);
        configuration.set("defaults.melee.damage.multiplier", 1.0D);
        configuration.set("defaults.melee.damage.flat-bonus", 0.0D);
        configuration.set("defaults.melee.damage.normalize-cooldown-scaling", true);
        configuration.set("defaults.melee.damage.maximum-normalization-multiplier", 5.0D);
        configuration.set("defaults.melee.knockback.enabled", true);
        configuration.set("defaults.melee.knockback.existing-velocity-multiplier", 0.0D);
        configuration.set("defaults.melee.knockback.horizontal", 0.38D);
        configuration.set("defaults.melee.knockback.vertical", 0.36D);
        configuration.set("defaults.melee.knockback.maximum-vertical", 0.42D);
        configuration.set("defaults.melee.sprint-knockback-multiplier", 1.15D);
        configuration.set("defaults.melee.sprint-reset.enabled", true);
        configuration.set("defaults.melee.sprint-reset.delay-ticks", 1);
        configuration.set("defaults.rods.enabled", true);
        configuration.set("defaults.rods.pull.existing-velocity-multiplier", 0.35D);
        configuration.set("defaults.rods.pull.horizontal", 0.12D);
        configuration.set("defaults.rods.pull.vertical", 0.08D);
        configuration.set("defaults.rods.pull.maximum-vertical", 0.30D);
        configuration.set("defaults.projectiles.enabled", true);
        configuration.set("defaults.projectiles.require-positive-final-damage", true);
        configuration.set("defaults.projectiles.damage.enabled", true);
        configuration.set("defaults.projectiles.damage.multiplier", 1.0D);
        configuration.set("defaults.projectiles.damage.flat-bonus", 0.0D);
        configuration.set("defaults.projectiles.knockback.enabled", false);
        configuration.set("defaults.projectiles.knockback.existing-velocity-multiplier", 0.35D);
        configuration.set("defaults.projectiles.knockback.horizontal", 0.22D);
        configuration.set("defaults.projectiles.knockback.vertical", 0.12D);
        configuration.set("defaults.projectiles.knockback.maximum-vertical", 0.35D);
        return configuration;
    }
}

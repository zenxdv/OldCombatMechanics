package net.mosspad.oldcombatmechanics.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/** Builds validated immutable settings once per startup or reload. */
public final class ConfigLoader {
    private static final int CURRENT_CONFIG_VERSION = 1;

    private ConfigLoader() {
    }

    public static Settings.PluginSettings load(Logger logger, FileConfiguration configuration) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(configuration, "configuration");

        boolean logWarnings = configuration.getBoolean("settings.log-configuration-warnings", true);
        WarningSink warnings = new WarningSink(logger, logWarnings);
        validateConfigVersion(configuration, warnings);

        ConfigurationSection defaultsSection = configuration.getConfigurationSection("defaults");
        if (defaultsSection == null) {
            warnings.warn("Missing defaults section; built-in fallback values are being used.");
            defaultsSection = new MemoryConfiguration();
        }

        Settings.WorldSettings defaults = readWorldSettings(defaultsSection, null, warnings, "defaults");
        Settings.WorldControl worldControl = readWorldControl(configuration, warnings);
        Map<String, Settings.WorldSettings> worlds = readWorlds(configuration, defaultsSection, warnings);
        Map<Material, Settings.WeaponSettings> weapons = readWeapons(configuration, warnings);
        Settings.Messages messages = readMessages(configuration, warnings);

        logger.info(() -> "Loaded combat settings for " + worlds.size() + " named world override(s), "
                + worldControl.enabledWorlds().size() + " enabled-list entries, "
                + worldControl.disabledWorlds().size() + " disabled-list entries, and "
                + weapons.size() + " weapon override(s).");
        return new Settings.PluginSettings(
                defaults,
                worldControl,
                Collections.unmodifiableMap(worlds),
                Collections.unmodifiableMap(weapons),
                messages
        );
    }

    private static void validateConfigVersion(FileConfiguration configuration, WarningSink warnings) {
        Object rawVersion = configuration.get("settings.config-version");
        if (rawVersion == null) {
            warnings.warn("settings.config-version is missing; treating this as configuration version "
                    + CURRENT_CONFIG_VERSION + ".");
            return;
        }
        if (!(rawVersion instanceof Number number) || !Double.isFinite(number.doubleValue())
                || Math.rint(number.doubleValue()) != number.doubleValue()) {
            warnings.warn("settings.config-version must be a whole number; known settings will be loaded defensively.");
            return;
        }
        long version = number.longValue();
        if (version > CURRENT_CONFIG_VERSION) {
            warnings.warn("settings.config-version is " + version + ", newer than this plugin supports ("
                    + CURRENT_CONFIG_VERSION + "). Unknown settings will be ignored.");
        } else if (version < CURRENT_CONFIG_VERSION) {
            warnings.warn("settings.config-version is " + version + ", older than the current format ("
                    + CURRENT_CONFIG_VERSION + "). Missing values will use safe defaults.");
        }
    }

    private static Settings.WorldControl readWorldControl(FileConfiguration configuration, WarningSink warnings) {
        ConfigurationSection section = configuration.getConfigurationSection("world-control");
        if (section == null) {
            return new Settings.WorldControl(Set.of(), Set.of());
        }

        ConfigReader reader = new ConfigReader(section, null, warnings, "world-control", "world-control");
        Set<String> enabled = normalizeWorldNames(reader.stringList("enabled-worlds"), warnings, "enabled-worlds");
        Set<String> disabled = normalizeWorldNames(reader.stringList("disabled-worlds"), warnings, "disabled-worlds");
        for (String worldName : enabled) {
            if (disabled.contains(worldName)) {
                warnings.warn("World '" + worldName + "' appears in both world-control lists; disabled-worlds wins.");
            }
        }
        return new Settings.WorldControl(enabled, disabled);
    }

    private static Set<String> normalizeWorldNames(
            java.util.List<String> rawNames,
            WarningSink warnings,
            String path
    ) {
        Set<String> names = new LinkedHashSet<>();
        for (String rawName : rawNames) {
            String normalized = rawName.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                warnings.warn("world-control." + path + " contains an empty world name; it was skipped.");
                continue;
            }
            if (!names.add(normalized)) {
                warnings.warn("world-control." + path + " contains duplicate world '" + rawName
                        + "'; duplicate skipped.");
            }
        }
        return Collections.unmodifiableSet(names);
    }

    private static Map<String, Settings.WorldSettings> readWorlds(
            FileConfiguration configuration,
            ConfigurationSection defaults,
            WarningSink warnings
    ) {
        ConfigurationSection worldsSection = configuration.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return Map.of();
        }

        Map<String, Settings.WorldSettings> worlds = new LinkedHashMap<>();
        for (String worldName : worldsSection.getKeys(false)) {
            ConfigurationSection override = worldsSection.getConfigurationSection(worldName);
            if (override == null) {
                warnings.warn("World override '" + worldName + "' is not a configuration section and was skipped.");
                continue;
            }

            String normalized = worldName.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                warnings.warn("World override contains an empty world name; entry was skipped.");
                continue;
            }
            if (worlds.containsKey(normalized)) {
                warnings.warn("World override '" + worldName + "' duplicates another world name ignoring case; it was skipped.");
                continue;
            }
            worlds.put(normalized, readWorldSettings(defaults, override, warnings, "worlds." + worldName));
        }
        return worlds;
    }

    private static Map<Material, Settings.WeaponSettings> readWeapons(
            FileConfiguration configuration,
            WarningSink warnings
    ) {
        ConfigurationSection weaponsSection = configuration.getConfigurationSection("weapons");
        if (weaponsSection == null) {
            return Map.of();
        }

        Map<Material, Settings.WeaponSettings> weapons = new LinkedHashMap<>();
        for (String materialName : weaponsSection.getKeys(false)) {
            String normalizedName = materialName.trim().toUpperCase(Locale.ROOT);
            if (normalizedName.isEmpty()) {
                warnings.warn("Weapon entry has an empty material name; it was skipped.");
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(normalizedName);
            } catch (IllegalArgumentException exception) {
                warnings.warn("Unknown weapon material '" + materialName + "'; entry was skipped.");
                continue;
            }
            if (material.isAir()) {
                warnings.warn("Weapon material '" + materialName + "' is air; entry was skipped.");
                continue;
            }
            if (weapons.containsKey(material)) {
                warnings.warn("Weapon entry '" + materialName + "' duplicates another material name ignoring case; it was skipped.");
                continue;
            }

            ConfigurationSection section = weaponsSection.getConfigurationSection(materialName);
            if (section == null) {
                warnings.warn("Weapon entry '" + materialName + "' is not a section and was skipped.");
                continue;
            }

            ConfigReader reader = new ConfigReader(
                    section, null, warnings, "weapons." + materialName, "weapons." + materialName
            );
            Settings.WeaponSettings weapon = new Settings.WeaponSettings(
                    reader.bool("enabled", true),
                    readDamage(reader, "damage", false),
                    reader.boundedDouble("knockback.horizontal-multiplier", 1.0D, 0.0D, 10.0D),
                    reader.boundedDouble("knockback.vertical-multiplier", 1.0D, 0.0D, 10.0D)
            );
            weapons.put(material, weapon);
        }
        return weapons;
    }

    private static Settings.WorldSettings readWorldSettings(
            ConfigurationSection defaults,
            ConfigurationSection override,
            WarningSink warnings,
            String scope
    ) {
        ConfigReader reader = new ConfigReader(defaults, override, warnings, scope);

        Settings.ScopeSettings targetScope = new Settings.ScopeSettings(
                reader.bool("scope.player-only", true),
                reader.bool("scope.pve-enabled", false),
                readEntityCategories(reader, warnings)
        );

        Settings.FastAttackSettings fastAttack = new Settings.FastAttackSettings(
                reader.bool("fast-attack.enabled", true),
                reader.boundedDouble("fast-attack.attack-speed-bonus", 16.0D, 0.0D, 128.0D)
        );

        Settings.MeleeSettings melee = new Settings.MeleeSettings(
                reader.bool("melee.enabled", true),
                reader.bool("melee.require-positive-final-damage", true),
                reader.bool("melee.disable-sweeping-attacks", true),
                readDamage(reader, "melee.damage", true),
                readKnockback(reader, "melee.knockback", true),
                reader.boundedDouble("melee.sprint-knockback-multiplier", 1.15D, 0.0D, 10.0D),
                new Settings.SprintResetSettings(
                        reader.bool("melee.sprint-reset.enabled", true),
                        reader.boundedInt("melee.sprint-reset.delay-ticks", 1, 0, 20)
                )
        );

        Settings.RodSettings rods = new Settings.RodSettings(
                reader.bool("rods.enabled", true),
                readKnockback(reader, "rods.pull", false)
        );

        Settings.ProjectileSettings projectiles = new Settings.ProjectileSettings(
                reader.bool("projectiles.enabled", true),
                reader.bool("projectiles.require-positive-final-damage", true),
                readProjectileTypes(reader, warnings),
                readDamage(reader, "projectiles.damage", false),
                readKnockback(reader, "projectiles.knockback", true)
        );

        return new Settings.WorldSettings(
                reader.bool("enabled", true), targetScope, fastAttack, melee, rods, projectiles
        );
    }

    private static Settings.DamageSettings readDamage(
            ConfigReader reader,
            String path,
            boolean includeCooldownNormalization
    ) {
        return new Settings.DamageSettings(
                reader.bool(path + ".enabled", true),
                reader.boundedDouble(path + ".multiplier", 1.0D, 0.0D, 100.0D),
                reader.boundedDouble(path + ".flat-bonus", 0.0D, -100.0D, 100.0D),
                includeCooldownNormalization && reader.bool(path + ".normalize-cooldown-scaling", true),
                includeCooldownNormalization
                        ? reader.boundedDouble(path + ".maximum-normalization-multiplier", 5.0D, 1.0D, 25.0D)
                        : 1.0D
        );
    }

    private static Settings.KnockbackSettings readKnockback(ConfigReader reader, String path, boolean readEnabled) {
        return new Settings.KnockbackSettings(
                !readEnabled || reader.bool(path + ".enabled", true),
                reader.boundedDouble(path + ".existing-velocity-multiplier", 0.0D, 0.0D, 5.0D),
                reader.boundedDouble(path + ".horizontal", 0.38D, 0.0D, 5.0D),
                reader.boundedDouble(path + ".vertical", 0.36D, -2.0D, 5.0D),
                reader.boundedDouble(path + ".maximum-vertical", 0.42D, 0.0D, 10.0D)
        );
    }

    private static Set<Settings.EntityCategory> readEntityCategories(ConfigReader reader, WarningSink warnings) {
        Set<Settings.EntityCategory> categories = new LinkedHashSet<>();
        for (String rawCategory : reader.stringList("scope.pve-entity-categories")) {
            String normalized = rawCategory.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                categories.add(Settings.EntityCategory.valueOf(normalized));
            } catch (IllegalArgumentException exception) {
                warnings.warn("Unknown PvE entity category '" + rawCategory + "'; it was skipped.");
            }
        }
        return Collections.unmodifiableSet(categories);
    }

    private static Set<String> readProjectileTypes(ConfigReader reader, WarningSink warnings) {
        Set<String> types = new LinkedHashSet<>();
        for (String rawType : reader.stringList("projectiles.allowed-types")) {
            String normalized = rawType.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            try {
                EntityType entityType = EntityType.valueOf(normalized);
                Class<? extends Entity> entityClass = entityType.getEntityClass();
                if (entityClass == null || !Projectile.class.isAssignableFrom(entityClass)) {
                    warnings.warn("Entity type '" + rawType + "' is not a projectile; it was skipped.");
                    continue;
                }
                types.add(normalized);
            } catch (IllegalArgumentException exception) {
                warnings.warn("Unknown projectile entity type '" + rawType + "'; it was skipped.");
            }
        }
        return Collections.unmodifiableSet(types);
    }

    private static Settings.Messages readMessages(FileConfiguration configuration, WarningSink warnings) {
        return new Settings.Messages(
                message(configuration, warnings, "messages.prefix", "&8[&bOldCombat&8] &7"),
                message(configuration, warnings, "messages.no-permission", "&cYou do not have permission to do that."),
                message(configuration, warnings, "messages.reloaded", "&aConfiguration reloaded."),
                message(configuration, warnings, "messages.reload-failed", "&cConfiguration reload failed. The previous settings are still active."),
                message(configuration, warnings, "messages.status", "&fDefault enabled=&b{default_enabled}&f. Overrides=&b{world_count}&f, enabled list=&b{enabled_list}&f, disabled list=&b{disabled_list}&f."),
                message(configuration, warnings, "messages.world-status", "&fWorld &b{world}&f: enabled=&b{enabled}&f, player-only=&b{player_only}&f, PvE=&b{pve}&f, attack speed bonus=&b{attack_speed}&f."),
                message(configuration, warnings, "messages.world-not-found", "&cWorld '{world}' is not loaded."),
                message(configuration, warnings, "messages.usage", "&fUsage: &b/oldcombat <reload|status|world <world>>")
        );
    }

    private static String message(
            FileConfiguration configuration,
            WarningSink warnings,
            String path,
            String fallback
    ) {
        Object value = configuration.get(path);
        if (value == null) {
            return fallback;
        }
        if (value instanceof String string) {
            return string;
        }
        warnings.warn(path + " must be text; using the built-in message.");
        return fallback;
    }

    static final class WarningSink {
        private final Logger logger;
        private final boolean enabled;

        WarningSink(Logger logger, boolean enabled) {
            this.logger = logger;
            this.enabled = enabled;
        }

        void warn(String message) {
            if (enabled) {
                logger.warning("[config] " + message);
            }
        }
    }
}

package net.mosspad.oldcombatmechanics.config;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Golem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable, validated runtime configuration model. */
public final class Settings {
    private Settings() {
    }

    public record PluginSettings(
            WorldSettings defaults,
            WorldControl worldControl,
            Map<String, WorldSettings> worlds,
            Map<Material, WeaponSettings> weapons,
            Messages messages
    ) {
        public PluginSettings {
            defaults = Objects.requireNonNull(defaults, "defaults");
            worldControl = Objects.requireNonNull(worldControl, "worldControl");
            worlds = Map.copyOf(Objects.requireNonNull(worlds, "worlds"));
            weapons = Map.copyOf(Objects.requireNonNull(weapons, "weapons"));
            messages = Objects.requireNonNull(messages, "messages");
        }

        public WorldSettings forWorld(String worldName) {
            String normalized = Objects.requireNonNull(worldName, "worldName").toLowerCase(Locale.ROOT);
            WorldSettings configured = worlds.getOrDefault(normalized, defaults);
            return worldControl.allows(normalized) ? configured : configured.withEnabled(false);
        }
    }

    public record WorldControl(Set<String> enabledWorlds, Set<String> disabledWorlds) {
        public WorldControl {
            enabledWorlds = Set.copyOf(Objects.requireNonNull(enabledWorlds, "enabledWorlds"));
            disabledWorlds = Set.copyOf(Objects.requireNonNull(disabledWorlds, "disabledWorlds"));
        }

        public boolean allows(String normalizedWorldName) {
            if (disabledWorlds.contains(normalizedWorldName)) {
                return false;
            }
            return enabledWorlds.isEmpty() || enabledWorlds.contains(normalizedWorldName);
        }
    }

    public record WorldSettings(
            boolean enabled,
            ScopeSettings scope,
            FastAttackSettings fastAttack,
            MeleeSettings melee,
            RodSettings rods,
            ProjectileSettings projectiles
    ) {
        public WorldSettings {
            scope = Objects.requireNonNull(scope, "scope");
            fastAttack = Objects.requireNonNull(fastAttack, "fastAttack");
            melee = Objects.requireNonNull(melee, "melee");
            rods = Objects.requireNonNull(rods, "rods");
            projectiles = Objects.requireNonNull(projectiles, "projectiles");
        }

        public WorldSettings withEnabled(boolean enabled) {
            return new WorldSettings(enabled, scope, fastAttack, melee, rods, projectiles);
        }

        public boolean allowsTarget(LivingEntity target) {
            if (target instanceof Player) {
                return true;
            }
            if (scope.playerOnly() || !scope.pveEnabled()) {
                return false;
            }
            return scope.pveEntityCategories().isEmpty()
                    || scope.pveEntityCategories().contains(entityCategory(target));
        }
    }

    public record ScopeSettings(
            boolean playerOnly,
            boolean pveEnabled,
            Set<EntityCategory> pveEntityCategories
    ) {
        public ScopeSettings {
            pveEntityCategories = Set.copyOf(Objects.requireNonNull(pveEntityCategories, "pveEntityCategories"));
        }
    }

    public enum EntityCategory {
        MONSTER,
        ANIMAL,
        WATER,
        VILLAGER,
        GOLEM,
        OTHER
    }

    public static EntityCategory entityCategory(LivingEntity entity) {
        if (entity instanceof Monster) {
            return EntityCategory.MONSTER;
        }
        if (entity instanceof WaterMob) {
            return EntityCategory.WATER;
        }
        if (entity instanceof Villager) {
            return EntityCategory.VILLAGER;
        }
        if (entity instanceof Golem) {
            return EntityCategory.GOLEM;
        }
        if (entity instanceof Animals) {
            return EntityCategory.ANIMAL;
        }
        return EntityCategory.OTHER;
    }

    public record FastAttackSettings(boolean enabled, double attackSpeedBonus) {
    }

    public record MeleeSettings(
            boolean enabled,
            boolean requirePositiveFinalDamage,
            boolean disableSweepingAttacks,
            DamageSettings damage,
            KnockbackSettings knockback,
            double sprintKnockbackMultiplier,
            SprintResetSettings sprintReset
    ) {
        public MeleeSettings {
            damage = Objects.requireNonNull(damage, "damage");
            knockback = Objects.requireNonNull(knockback, "knockback");
            sprintReset = Objects.requireNonNull(sprintReset, "sprintReset");
        }
    }

    public record DamageSettings(
            boolean enabled,
            double multiplier,
            double flatBonus,
            boolean normalizeCooldownScaling,
            double maximumNormalizationMultiplier
    ) {
        public static final DamageSettings IDENTITY = new DamageSettings(
                false, 1.0D, 0.0D, false, 1.0D
        );
    }

    public record KnockbackSettings(
            boolean enabled,
            double existingVelocityMultiplier,
            double horizontal,
            double vertical,
            double maximumVertical
    ) {
        public KnockbackSettings scaled(double horizontalMultiplier, double verticalMultiplier) {
            return new KnockbackSettings(
                    enabled,
                    existingVelocityMultiplier,
                    horizontal * horizontalMultiplier,
                    vertical * verticalMultiplier,
                    maximumVertical
            );
        }
    }

    public record SprintResetSettings(boolean enabled, int delayTicks) {
    }

    public record RodSettings(boolean enabled, KnockbackSettings pull) {
        public RodSettings {
            pull = Objects.requireNonNull(pull, "pull");
        }
    }

    public record ProjectileSettings(
            boolean enabled,
            boolean requirePositiveFinalDamage,
            Set<String> allowedTypes,
            DamageSettings damage,
            KnockbackSettings knockback
    ) {
        public ProjectileSettings {
            allowedTypes = Set.copyOf(Objects.requireNonNull(allowedTypes, "allowedTypes"));
            damage = Objects.requireNonNull(damage, "damage");
            knockback = Objects.requireNonNull(knockback, "knockback");
        }

        public boolean allowsType(String entityTypeName) {
            return allowedTypes.isEmpty()
                    || allowedTypes.contains(Objects.requireNonNull(entityTypeName, "entityTypeName").toUpperCase(Locale.ROOT));
        }
    }

    public record WeaponSettings(
            boolean enabled,
            DamageSettings damage,
            double horizontalKnockbackMultiplier,
            double verticalKnockbackMultiplier
    ) {
        public static final WeaponSettings IDENTITY = new WeaponSettings(
                true, DamageSettings.IDENTITY, 1.0D, 1.0D
        );

        public WeaponSettings {
            damage = Objects.requireNonNull(damage, "damage");
        }
    }

    public record Messages(
            String prefix,
            String noPermission,
            String reloaded,
            String reloadFailed,
            String status,
            String worldStatus,
            String worldNotFound,
            String usage
    ) {
        public Messages {
            prefix = Objects.requireNonNull(prefix, "prefix");
            noPermission = Objects.requireNonNull(noPermission, "noPermission");
            reloaded = Objects.requireNonNull(reloaded, "reloaded");
            reloadFailed = Objects.requireNonNull(reloadFailed, "reloadFailed");
            status = Objects.requireNonNull(status, "status");
            worldStatus = Objects.requireNonNull(worldStatus, "worldStatus");
            worldNotFound = Objects.requireNonNull(worldNotFound, "worldNotFound");
            usage = Objects.requireNonNull(usage, "usage");
        }
    }
}

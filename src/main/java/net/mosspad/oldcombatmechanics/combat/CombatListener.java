package net.mosspad.oldcombatmechanics.combat;

import net.mosspad.oldcombatmechanics.OldCombatMechanicsPlugin;
import net.mosspad.oldcombatmechanics.config.ConfigManager;
import net.mosspad.oldcombatmechanics.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

/** Handles direct melee, player-fired projectile, and fishing-hook combat paths. */
public final class CombatListener implements Listener {
    private final OldCombatMechanicsPlugin plugin;
    private final ConfigManager configManager;
    private final VelocityService velocityService;

    public CombatListener(
            OldCombatMechanicsPlugin plugin,
            ConfigManager configManager,
            VelocityService velocityService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        this.velocityService = Objects.requireNonNull(velocityService, "velocityService");
    }

    /** Applies event damage changes before normal damage-event listeners finish. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageAdjust(EntityDamageByEntityEvent event) {
        MeleeContext melee = resolveMelee(event);
        if (melee != null) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                if (melee.world().melee().disableSweepingAttacks()) {
                    event.setCancelled(true);
                }
                return;
            }

            applyDamage(event, melee.world().melee().damage(), melee.attacker().getAttackCooldown());
            applyDamage(event, melee.weapon().damage(), 1.0F);
            return;
        }

        ProjectileContext projectile = resolveProjectile(event);
        if (projectile != null) {
            applyDamage(event, projectile.settings().damage(), 1.0F);
        }
    }

    /**
     * Queues post-hit mechanics only after damage was not cancelled. MONITOR is used
     * for observation/queueing; the actual player velocity is still changed through
     * PlayerVelocityEvent at HIGHEST so later velocity listeners can see it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamagePost(EntityDamageByEntityEvent event) {
        MeleeContext melee = resolveMelee(event);
        if (melee != null) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                return;
            }
            if (melee.world().melee().requirePositiveFinalDamage() && event.getFinalDamage() <= 0.0D) {
                return;
            }

            Settings.KnockbackSettings knockback = melee.world().melee().knockback().scaled(
                    melee.weapon().horizontalKnockbackMultiplier(),
                    melee.weapon().verticalKnockbackMultiplier()
            );
            boolean sprinting = melee.attacker().isSprinting();
            if (sprinting) {
                knockback = knockback.scaled(melee.world().melee().sprintKnockbackMultiplier(), 1.0D);
            }

            applyDirectHitVelocity(melee.target(),
                    VelocityService.pushDirection(melee.attacker().getLocation(), melee.target().getLocation()),
                    knockback);
            if (sprinting) {
                resetSprintIfConfigured(melee.attacker(), melee.world().melee().sprintReset());
            }
            return;
        }

        ProjectileContext projectile = resolveProjectile(event);
        if (projectile == null) {
            return;
        }
        if (projectile.settings().requirePositiveFinalDamage() && event.getFinalDamage() <= 0.0D) {
            return;
        }

        applyDirectHitVelocity(projectile.target(),
                VelocityService.pushDirection(projectile.attacker().getLocation(), projectile.target().getLocation()),
                projectile.settings().knockback());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }

        Entity caught = event.getCaught();
        if (!(caught instanceof LivingEntity target)) {
            return;
        }

        Player fisher = event.getPlayer();
        Settings.WorldSettings world = configManager.settings().forWorld(fisher.getWorld().getName());
        if (!world.enabled() || !world.allowsTarget(target) || !world.rods().enabled()) {
            return;
        }

        applyNextTickVelocity(target,
                VelocityService.pullDirection(target.getLocation(), fisher.getLocation()),
                world.rods().pull());
    }

    private MeleeContext resolveMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof LivingEntity target)) {
            return null;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return null;
        }

        Settings.WorldSettings world = configManager.settings().forWorld(attacker.getWorld().getName());
        if (!world.enabled() || !world.melee().enabled() || !world.allowsTarget(target)) {
            return null;
        }

        Settings.WeaponSettings weapon = weaponSettings(attacker.getInventory().getItemInMainHand());
        if (!weapon.enabled()) {
            return null;
        }
        return new MeleeContext(attacker, target, world, weapon);
    }

    private ProjectileContext resolveProjectile(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile projectile)
                || !(projectile.getShooter() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity target)
                || event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
            return null;
        }

        Settings.WorldSettings world = configManager.settings().forWorld(attacker.getWorld().getName());
        Settings.ProjectileSettings settings = world.projectiles();
        if (!world.enabled() || !world.allowsTarget(target) || !settings.enabled()
                || !settings.allowsType(projectile.getType().name())) {
            return null;
        }
        return new ProjectileContext(attacker, target, settings);
    }

    private void applyDamage(
            EntityDamageByEntityEvent event,
            Settings.DamageSettings settings,
            float cooldown
    ) {
        if (!settings.enabled()) {
            return;
        }

        double multiplier = settings.multiplier();
        if (settings.normalizeCooldownScaling()) {
            double clampedCooldown = Math.max(0.0D, Math.min(1.0D, cooldown));
            double modernDamageFactor = 0.2D + 0.8D * clampedCooldown * clampedCooldown;
            multiplier *= Math.min(settings.maximumNormalizationMultiplier(), 1.0D / modernDamageFactor);
        }

        double newDamage = event.getDamage() * multiplier + settings.flatBonus();
        event.setDamage(Math.max(0.0D, newDamage));
    }

    private void applyDirectHitVelocity(
            LivingEntity target,
            Vector direction,
            Settings.KnockbackSettings settings
    ) {
        if (!settings.enabled()) {
            return;
        }
        if (target instanceof Player player) {
            velocityService.queueForNativePlayerVelocity(player, direction, settings);
        } else {
            applyNextTickVelocity(target, direction, settings);
        }
    }

    private void applyNextTickVelocity(
            LivingEntity target,
            Vector direction,
            Settings.KnockbackSettings settings
    ) {
        velocityService.applyNextTick(target, direction, settings);
    }

    private void resetSprintIfConfigured(Player attacker, Settings.SprintResetSettings settings) {
        if (!settings.enabled()) {
            return;
        }

        UUID playerId = attacker.getUniqueId();
        UUID worldId = attacker.getWorld().getUID();
        long runtimeGeneration = plugin.runtimeGeneration();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (runtimeGeneration != plugin.runtimeGeneration()) {
                return;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && player.getWorld().getUID().equals(worldId)) {
                player.setSprinting(false);
            }
        }, settings.delayTicks());
    }

    private Settings.WeaponSettings weaponSettings(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Settings.WeaponSettings.IDENTITY;
        }
        return configManager.settings().weapons().getOrDefault(item.getType(), Settings.WeaponSettings.IDENTITY);
    }

    private record MeleeContext(
            Player attacker,
            LivingEntity target,
            Settings.WorldSettings world,
            Settings.WeaponSettings weapon
    ) {
    }

    private record ProjectileContext(
            Player attacker,
            LivingEntity target,
            Settings.ProjectileSettings settings
    ) {
    }
}

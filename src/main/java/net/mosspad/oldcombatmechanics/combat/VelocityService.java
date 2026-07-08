package net.mosspad.oldcombatmechanics.combat;

import net.mosspad.oldcombatmechanics.OldCombatMechanicsPlugin;
import net.mosspad.oldcombatmechanics.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adjusts velocity without packets or server internals. It only keeps UUID-based,
 * one-tick state for player hits that are expected to trigger PlayerVelocityEvent.
 */
public final class VelocityService implements Listener {
    private final OldCombatMechanicsPlugin plugin;
    private final Map<UUID, PendingVelocity> pendingNativePlayerVelocity = new HashMap<>();
    private long generation;

    public VelocityService(OldCombatMechanicsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Queues a velocity adjustment for a normal hit whose native knockback is expected
     * to fire PlayerVelocityEvent in the current tick. A one-tick fallback is used
     * only when that event does not arrive.
     */
    public void queueForNativePlayerVelocity(Player target, Vector direction, Settings.KnockbackSettings settings) {
        if (!settings.enabled()) {
            return;
        }

        PendingVelocity pending = pending(target, direction, settings);
        pendingNativePlayerVelocity.put(pending.playerId(), pending);

        long taskGeneration = generation;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (taskGeneration != generation) {
                return;
            }
            PendingVelocity remaining = pendingNativePlayerVelocity.get(pending.playerId());
            if (remaining != pending) {
                return;
            }
            pendingNativePlayerVelocity.remove(pending.playerId());
            applyIfStillValid(remaining);
        }, 1L);
    }

    /** Applies a non-native velocity adjustment on the next tick. */
    public void applyNextTick(LivingEntity target, Vector direction, Settings.KnockbackSettings settings) {
        if (!settings.enabled()) {
            return;
        }

        PendingVelocity pending = pending(target, direction, settings);
        long taskGeneration = generation;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (taskGeneration == generation) {
                applyIfStillValid(pending);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        PendingVelocity pending = pendingNativePlayerVelocity.remove(event.getPlayer().getUniqueId());
        if (pending == null || !event.getPlayer().getWorld().getUID().equals(pending.worldId())) {
            return;
        }
        // A cancelled native velocity event is an explicit decision by another plugin.
        // Consume the pending entry so the one-tick fallback cannot reapply it.
        if (event.isCancelled()) {
            return;
        }
        event.setVelocity(combine(event.getVelocity(), pending));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingNativePlayerVelocity.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        pendingNativePlayerVelocity.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        pendingNativePlayerVelocity.remove(event.getPlayer().getUniqueId());
    }

    public void clear() {
        generation++;
        pendingNativePlayerVelocity.clear();
    }

    public static Vector pushDirection(Location attacker, Location target) {
        return horizontalDirection(attacker, target);
    }

    public static Vector pullDirection(Location target, Location pullToward) {
        return horizontalDirection(target, pullToward);
    }

    private PendingVelocity pending(LivingEntity target, Vector direction, Settings.KnockbackSettings settings) {
        return new PendingVelocity(
                target.getUniqueId(),
                target.getWorld().getUID(),
                normalizeHorizontal(direction),
                settings
        );
    }

    private void applyIfStillValid(PendingVelocity pending) {
        Entity entity = Bukkit.getEntity(pending.playerId());
        if (!(entity instanceof LivingEntity living) || !living.isValid()
                || !living.getWorld().getUID().equals(pending.worldId())) {
            return;
        }
        living.setVelocity(combine(living.getVelocity(), pending));
    }

    private static Vector horizontalDirection(Location from, Location toward) {
        return normalizeHorizontal(toward.toVector().subtract(from.toVector()));
    }

    private static Vector normalizeHorizontal(Vector direction) {
        Vector horizontal = direction.clone().setY(0.0D);
        if (horizontal.lengthSquared() < 0.0001D) {
            return new Vector(0.0D, 0.0D, 0.0D);
        }
        return horizontal.normalize();
    }

    private static Vector combine(Vector current, PendingVelocity pending) {
        Settings.KnockbackSettings settings = pending.settings();
        Vector result = current.clone().multiply(settings.existingVelocityMultiplier());
        Vector direction = pending.direction();
        result.setX(result.getX() + direction.getX() * settings.horizontal());
        result.setZ(result.getZ() + direction.getZ() * settings.horizontal());
        result.setY(Math.min(settings.maximumVertical(), result.getY() + settings.vertical()));
        return result;
    }

    private record PendingVelocity(
            UUID playerId,
            UUID worldId,
            Vector direction,
            Settings.KnockbackSettings settings
    ) {
        private PendingVelocity {
            direction = direction.clone();
        }
    }
}

package net.mosspad.oldcombatmechanics.combat;

import net.mosspad.oldcombatmechanics.OldCombatMechanicsPlugin;
import net.mosspad.oldcombatmechanics.config.ConfigManager;
import net.mosspad.oldcombatmechanics.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/** Applies one namespaced, removable attack-speed modifier per eligible player. */
public final class AttackSpeedService implements Listener {
    private final OldCombatMechanicsPlugin plugin;
    private final ConfigManager configManager;
    private final NamespacedKey modifierKey;

    public AttackSpeedService(OldCombatMechanicsPlugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
        modifierKey = new NamespacedKey(plugin, "classic_attack_speed");
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyFor(player);
        }
    }

    public void removeFromAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeFor(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        scheduleApply(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        scheduleApply(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        scheduleApply(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        removeFor(event.getPlayer());
    }

    private void scheduleApply(UUID playerId) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                applyFor(player);
            }
        });
    }

    private void applyFor(Player player) {
        removeFor(player);

        Settings.WorldSettings world = configManager.settings().forWorld(player.getWorld().getName());
        Settings.FastAttackSettings fastAttack = world.fastAttack();
        if (!world.enabled() || !fastAttack.enabled() || fastAttack.attackSpeedBonus() <= 0.0D) {
            return;
        }

        AttributeInstance instance = player.getAttribute(Attribute.ATTACK_SPEED);
        if (instance == null) {
            plugin.getLogger().warning("Player " + player.getName()
                    + " has no attack-speed attribute; classic fast attack was skipped.");
            return;
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                fastAttack.attackSpeedBonus(),
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
        );
        instance.addModifier(modifier);
    }

    private void removeFor(Player player) {
        AttributeInstance instance = player.getAttribute(Attribute.ATTACK_SPEED);
        if (instance == null) {
            return;
        }

        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getKey().equals(modifierKey)) {
                instance.removeModifier(modifier);
            }
        }
    }
}

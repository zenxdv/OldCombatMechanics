package net.mosspad.oldcombatmechanics.command;

import net.mosspad.oldcombatmechanics.OldCombatMechanicsPlugin;
import net.mosspad.oldcombatmechanics.config.ConfigManager;
import net.mosspad.oldcombatmechanics.config.Settings;
import net.mosspad.oldcombatmechanics.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class OldCombatCommand implements CommandExecutor, TabCompleter {
    private final OldCombatMechanicsPlugin plugin;
    private final ConfigManager configManager;

    public OldCombatCommand(OldCombatMechanicsPlugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configManager = Objects.requireNonNull(configManager, "configManager");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Settings.Messages messages = configManager.settings().messages();
        if (args.length == 0) {
            MessageUtil.send(sender, messages, messages.usage(), Map.of());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender, messages);
            case "status" -> status(sender, messages);
            case "world" -> world(sender, messages, args);
            default -> MessageUtil.send(sender, messages, messages.usage(), Map.of());
        }
        return true;
    }

    private void reload(CommandSender sender, Settings.Messages messages) {
        if (!hasPermission(sender, "oldcombat.reload")) {
            noPermission(sender, messages);
            return;
        }

        if (plugin.reloadPlugin()) {
            MessageUtil.send(sender, configManager.settings().messages(),
                    configManager.settings().messages().reloaded(), Map.of());
        } else {
            MessageUtil.send(sender, messages, messages.reloadFailed(), Map.of());
        }
    }

    private void status(CommandSender sender, Settings.Messages messages) {
        if (!hasPermission(sender, "oldcombat.status")) {
            noPermission(sender, messages);
            return;
        }

        Settings.PluginSettings settings = configManager.settings();
        MessageUtil.send(sender, messages, messages.status(), Map.of(
                "default_enabled", String.valueOf(settings.defaults().enabled()),
                "world_count", String.valueOf(settings.worlds().size()),
                "enabled_list", String.valueOf(settings.worldControl().enabledWorlds().size()),
                "disabled_list", String.valueOf(settings.worldControl().disabledWorlds().size())
        ));
    }

    private void world(CommandSender sender, Settings.Messages messages, String[] args) {
        if (!hasPermission(sender, "oldcombat.status")) {
            noPermission(sender, messages);
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, messages, messages.usage(), Map.of());
            return;
        }

        World world = Bukkit.getWorld(args[1]);
        if (world == null) {
            MessageUtil.send(sender, messages, messages.worldNotFound(), Map.of("world", args[1]));
            return;
        }

        Settings.WorldSettings settings = configManager.settings().forWorld(world.getName());
        MessageUtil.send(sender, messages, messages.worldStatus(), Map.of(
                "world", world.getName(),
                "enabled", String.valueOf(settings.enabled()),
                "player_only", String.valueOf(settings.scope().playerOnly()),
                "pve", String.valueOf(settings.scope().pveEnabled()),
                "attack_speed", String.valueOf(settings.fastAttack().attackSpeedBonus())
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (hasPermission(sender, "oldcombat.reload")) {
                options.add("reload");
            }
            if (hasPermission(sender, "oldcombat.status")) {
                options.add("status");
                options.add("world");
            }
            return partial(args[0], options);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")
                && hasPermission(sender, "oldcombat.status")) {
            List<String> names = Bukkit.getWorlds().stream().map(World::getName).toList();
            return partial(args[1], names);
        }
        return List.of();
    }

    private List<String> partial(String entered, List<String> options) {
        String normalized = entered.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission("oldcombat.admin") || sender.hasPermission(permission);
    }

    private void noPermission(CommandSender sender, Settings.Messages messages) {
        MessageUtil.send(sender, messages, messages.noPermission(), Map.of());
    }
}

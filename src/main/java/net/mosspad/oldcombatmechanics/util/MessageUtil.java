package net.mosspad.oldcombatmechanics.util;

import net.mosspad.oldcombatmechanics.config.Settings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Objects;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static void send(
            CommandSender sender,
            Settings.Messages messages,
            String message,
            Map<String, String> replacements
    ) {
        Objects.requireNonNull(sender, "sender");
        sender.sendMessage(colorize(replace(messages.prefix() + message, replacements)));
    }

    private static String replace(String input, Map<String, String> replacements) {
        String output = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }

    private static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

package me.doors.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import me.doors.DoorsPlugin;

public final class ColorUtils {
    private ColorUtils() {}

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void msg(CommandSender sender, String message) {
        String prefix = DoorsPlugin.getInstance().getConfig()
                .getString("messages.prefix", "&8[&6DOORS&8] ");
        sender.sendMessage(color(prefix + message));
    }

    public static String cfgMsg(String key) {
        return color(DoorsPlugin.getInstance().getConfig()
                .getString("messages." + key, "&7" + key));
    }

    public static String cfgMsg(String key, String... replacements) {
        String s = cfgMsg(key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return s;
    }
}

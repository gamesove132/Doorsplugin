package me.doors;

import me.doors.utils.ColorUtils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DoorsCommand implements CommandExecutor, TabCompleter {

    private final DoorsPlugin plugin;

    public DoorsCommand(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        // /doorsgive [player]
        if (command.getName().equalsIgnoreCase("doorsgive")) {
            if (!sender.hasPermission("doors.admin")) { deny(sender); return true; }
            Player target = (args.length > 0)
                    ? plugin.getServer().getPlayerExact(args[0])
                    : (sender instanceof Player p ? p : null);
            if (target == null) { ColorUtils.msg(sender, "&cГравець не знайдений."); return true; }
            target.getInventory().addItem(plugin.getSelectorItem().create());
            ColorUtils.msg(sender, "&aВидано DOORS-сокирку гравцю &e" + target.getName());
            return true;
        }

        // /doors ...
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {

            case "start" -> {
                if (!sender.hasPermission("doors.admin")) { deny(sender); return true; }
                plugin.getGameManager().startNewGame();
                ColorUtils.msg(sender, "&aНова гра DOORS підготована!");
            }

            case "stop" -> {
                if (!sender.hasPermission("doors.admin")) { deny(sender); return true; }
                plugin.getGameManager().forceStopAll();
                ColorUtils.msg(sender, "&cВсі ігри зупинені.");
            }

            case "join" -> {
                if (!(sender instanceof Player p)) { ColorUtils.msg(sender, "&cТільки для гравців."); return true; }
                if (args.length >= 2) {
                    // Join specific portal slot: /doors join <1-4>
                    try {
                        int slot = Integer.parseInt(args[1]);
                        plugin.getLobbyManager().tryJoinPortal(p, slot);
                    } catch (NumberFormatException ex) {
                        ColorUtils.msg(p, "&cВкажіть номер порталу 1-4.");
                    }
                } else {
                    plugin.getGameManager().joinGame(p);
                }
            }

            case "leave" -> {
                if (!(sender instanceof Player p)) { ColorUtils.msg(sender, "&cТільки для гравців."); return true; }
                if (plugin.getGameManager().isInGame(p)) plugin.getGameManager().leaveGame(p);
                else if (plugin.getLobbyManager().isInPortal(p)) plugin.getLobbyManager().leavePortal(p);
                else ColorUtils.msg(p, "&cВи не в грі і не в черзі.");
            }

            case "reload" -> {
                if (!sender.hasPermission("doors.admin")) { deny(sender); return true; }
                plugin.reloadConfig();
                ColorUtils.msg(sender, "&aКонфіг перезавантажено.");
            }

            case "setlobby" -> {
                if (!sender.hasPermission("doors.admin")) { deny(sender); return true; }
                if (!(sender instanceof Player p)) { ColorUtils.msg(sender, "&cТільки для гравців."); return true; }
                // Save current location as lobby spawn
                plugin.getConfig().set("lobby.world",   p.getWorld().getName());
                plugin.getConfig().set("lobby.spawn-x", p.getLocation().getX());
                plugin.getConfig().set("lobby.spawn-y", p.getLocation().getY());
                plugin.getConfig().set("lobby.spawn-z", p.getLocation().getZ());
                plugin.saveConfig();
                ColorUtils.msg(p, "&aЛобі-спаун встановлено тут.");
            }

            case "info" -> {
                ColorUtils.msg(sender, "&6DOORS Plugin v2.0");
                ColorUtils.msg(sender, "&7Ігор активних: &e" + getActiveGameCount());
                ColorUtils.msg(sender, "&7Гравців в грі: &e" + getPlayersInGame());
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ColorUtils.color("&8━━━━━━━━━━━━ &6DOORS &8━━━━━━━━━━━━"));
        s.sendMessage(ColorUtils.color("&e/doors join [1-4] &7— приєднатись / вибрати портал"));
        s.sendMessage(ColorUtils.color("&e/doors leave &7— вийти з гри або черги"));
        s.sendMessage(ColorUtils.color("&e/doors info &7— інформація про сервер"));
        if (s.hasPermission("doors.admin")) {
            s.sendMessage(ColorUtils.color("&c/doors start &7— створити нову гру"));
            s.sendMessage(ColorUtils.color("&c/doors stop &7— зупинити всі ігри"));
            s.sendMessage(ColorUtils.color("&c/doors reload &7— перезавантажити конфіг"));
            s.sendMessage(ColorUtils.color("&c/doors setlobby &7— встановити спаун лобі"));
            s.sendMessage(ColorUtils.color("&c/doorsgive [player] &7— видати сокирку-селектор"));
        }
        s.sendMessage(ColorUtils.color("&8━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private void deny(CommandSender s) { ColorUtils.msg(s, "&cНедостатньо прав."); }

    private int getActiveGameCount() {
        // Reflection workaround — we expose a simple count
        return 0; // placeholder; GameManager doesn't expose count publicly yet
    }

    private int getPlayersInGame() { return 0; }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("doorsgive")) {
            return null; // player names
        }
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of("join", "leave", "info"));
            if (sender.hasPermission("doors.admin"))
                base.addAll(List.of("start", "stop", "reload", "setlobby"));
            return base;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return List.of("1", "2", "3", "4");
        }
        return List.of();
    }
}

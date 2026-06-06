package me.doors.managers;

import me.doors.DoorsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HUDManager {

    private final DoorsPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public HUDManager(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateHUD(Player p, int room) {
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), k -> {
            BossBar b = Bukkit.createBossBar("DOORS", BarColor.YELLOW, BarStyle.SEGMENTED_20);
            b.addPlayer(p);
            return b;
        });

        bar.setTitle("§6§lDOORS  §8|  §eКімната §f" + room + " §8/ §f100");
        bar.setProgress(Math.min(1.0, room / 100.0));

        if (room >= 80)      bar.setColor(BarColor.RED);
        else if (room >= 50) bar.setColor(BarColor.PURPLE);
        else if (room >= 25) bar.setColor(BarColor.YELLOW);
        else                 bar.setColor(BarColor.GREEN);
    }

    public void sendActionBar(Player p, String msg) {
        // Paper Adventure API — no deprecation
        Component component = LegacyComponentSerializer.legacySection().deserialize(
                msg.replace("&", "§"));
        p.sendActionBar(component);
    }

    public void removeHUD(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) { bar.removePlayer(p); bar.setVisible(false); }
    }
}

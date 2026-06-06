package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * DUPE — handled via DoorListener.
 * This entity just marks that a room has a "fake" door.
 * When player interacts with a wrong extra door, DoorListener calls onDupeTriggered().
 */
public class DupeEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public DupeEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        // No tick-based logic for Dupe — triggered by DoorListener
        // Just return a no-op runnable
        return new BukkitRunnable() {
            @Override public void run() {}
        };
    }

    /** Called by DoorListener when player opens wrong door */
    public static void onDupeTriggered(DoorsPlugin plugin, Player player) {
        int dmg = plugin.getConfig().getInt("entities.dupe.damage", 40);
        player.damage(dmg);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 30, 0));
        plugin.getHudManager().sendActionBar(player, "§4§lDUPE — неправильні двері!");
        plugin.getScreamManager().playDeath(player); // reuse jumpscare sound
    }
}

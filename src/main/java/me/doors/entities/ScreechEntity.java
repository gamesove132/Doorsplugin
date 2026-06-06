package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SCREECH — "psst!" sound cue.
 * Player must look around to find it; if they stop moving → jumpscare + damage.
 */
public class ScreechEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public ScreechEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        int idleThreshold = plugin.getConfig().getInt("entities.screech.idle-ticks", 30);
        int dmg = plugin.getConfig().getInt("entities.screech.damage", 20);

        BukkitRunnable task = new BukkitRunnable() {
            int idleTicks = 0;
            org.bukkit.Location lastLoc = player.getLocation().clone();
            boolean psstPlayed = false;

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }

                // Play psst cue once
                if (!psstPlayed) {
                    plugin.getScreamManager().playScreechPsst(player);
                    psstPlayed = true;
                }

                double moved = player.getLocation().distanceSquared(lastLoc);
                if (moved < 0.05) {
                    idleTicks++;
                    plugin.getHudManager().sendActionBar(player, "§7... " + "psst!".repeat(Math.min(idleTicks / 8, 3)));
                } else {
                    idleTicks = Math.max(0, idleTicks - 3);
                }
                lastLoc = player.getLocation().clone();

                if (idleTicks >= idleThreshold) {
                    // Attack!
                    plugin.getScreamManager().playScreechAttack(player);
                    player.damage(dmg);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 1));
                    cancel();
                }
            }
        };

        task.runTaskTimer(plugin, 20L, 3L);
        return task;
    }
}

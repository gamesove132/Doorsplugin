package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.managers.GameSession;
import me.doors.rooms.Room;
import me.doors.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SEEK — triggered at rooms 40 and 70.
 * Follows player along Z-axis with increasing speed.
 * Player must run to survive.
 */
public class SeekEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final GameSession session;
    private final Room room;

    public SeekEntity(DoorsPlugin plugin, Player player, GameSession session, Room room) {
        this.plugin = plugin; this.player = player; this.session = session; this.room = room;
    }

    public BukkitRunnable start() {
        BukkitRunnable task = new BukkitRunnable() {
            double seekZ = room.getOrigin().getZ() - 20;
            double speed = plugin.getConfig().getDouble("entities.seek.speed", 0.55);
            int duration = plugin.getConfig().getInt("entities.seek.duration-ticks", 600);
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }
                if (tick++ >= duration) { cancel(); return; }

                // Accelerate slightly over time
                if (tick % 60 == 0) speed = Math.min(speed + 0.05, 1.2);
                seekZ += speed;

                plugin.getScreamManager().playSeekApproach(player);

                // Visual: dark shadow at seek position
                Location seekLoc = new Location(player.getWorld(),
                        player.getLocation().getX(), player.getLocation().getY() + 1, seekZ);
                player.getWorld().spawnParticle(Particle.SOUL, seekLoc, 12, 1.5, 2, 1.5, 0.01);

                double gap = player.getLocation().getZ() - seekZ;

                if (gap < 0) { // Seek passed player
                    player.setHealth(0.0);
                    plugin.getScreamManager().playDeath(player);
                    cancel(); return;
                }

                // ActionBar feedback
                if (gap < 5) {
                    plugin.getHudManager().sendActionBar(player, "§4§l☠ SEEK ЗОВСІМ ПОРУЧ! ☠");
                } else if (gap < 15) {
                    plugin.getHudManager().sendActionBar(player, "§c§l⚡ ТІКАЙ! ⚡");
                } else {
                    plugin.getHudManager().sendActionBar(player, "§e§lВОНО ЗА ТОБОЮ! НЕ ЗУПИНЯЙСЯ!");
                }
            }
        };

        task.runTaskTimer(plugin, 15L, 1L);
        return task;
    }
}

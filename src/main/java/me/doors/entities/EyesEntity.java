package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * EYES — appears in dark rooms.
 * Damages player while they look at it; player must look away.
 */
public class EyesEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public EyesEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        plugin.getScreamManager().playEyesAmbient(player);

        BukkitRunnable task = new BukkitRunnable() {
            final Location eyeLoc = room.getCenter().add(
                    (Math.random() - 0.5) * (room.getWidth() - 6),
                    2.2,
                    (Math.random() - 0.5) * (room.getLength() - 6));
            int lookTicks = 0;
            final double threshold = plugin.getConfig().getDouble("entities.eyes.look-threshold", 0.97);
            final int grace = plugin.getConfig().getInt("entities.eyes.grace-ticks", 8);
            final int dmg   = plugin.getConfig().getInt("entities.eyes.damage-per-tick", 2);
            int lifetime = 200; // 10 sec

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }
                if (--lifetime <= 0) { cancel(); return; }

                // Render two red eye particles
                eyeLoc.getWorld().spawnParticle(Particle.FLAME, eyeLoc.clone().add(-0.35, 0, 0), 1, 0, 0, 0, 0);
                eyeLoc.getWorld().spawnParticle(Particle.FLAME, eyeLoc.clone().add( 0.35, 0, 0), 1, 0, 0, 0, 0);

                if (isLooking()) {
                    lookTicks++;
                    if (lookTicks > grace) {
                        plugin.getHudManager().sendActionBar(player, "§5§lНЕ ДИВИСЬ НА НЬОГО!");
                        double hp = player.getHealth() - dmg;
                        if (hp <= 0) { player.setHealth(0.0); plugin.getScreamManager().playDeath(player); cancel(); }
                        else player.setHealth(hp);
                    }
                } else {
                    if (lookTicks > 0) lookTicks = Math.max(0, lookTicks - 2);
                }
            }

            private boolean isLooking() {
                Location eye = player.getEyeLocation();
                double dist = eye.distance(eyeLoc);
                if (dist > 20) return false;
                Vector toEntity = eyeLoc.clone().subtract(eye).toVector().normalize();
                double dot = player.getLocation().getDirection().normalize().dot(toEntity);
                return dot >= threshold;
            }
        };

        task.runTaskTimer(plugin, 5L, 4L);
        return task;
    }
}

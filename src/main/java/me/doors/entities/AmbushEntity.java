package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** AMBUSH — like Rush, but bounces back and forth multiple times */
public class AmbushEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public AmbushEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        BukkitRunnable task = new BukkitRunnable() {
            int bounces = plugin.getConfig().getInt("entities.ambush.bounces", 8);
            final double speed = plugin.getConfig().getDouble("entities.ambush.speed", 0.7);
            boolean forward = true;
            double z = room.getOrigin().getZ();
            final double minZ = z, maxZ = z + room.getLength();

            @Override
            public void run() {
                if (!player.isOnline() || bounces <= 0) { cancel(); return; }

                z += forward ? speed : -speed;
                if (z >= maxZ) { forward = false; bounces--; }
                if (z <= minZ) { forward = true;  bounces--; }

                Location loc = new Location(room.getOrigin().getWorld(),
                        room.getCenter().getX(), room.getOrigin().getY() + 2, z);
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 4, 0.3, 0.3, 0.3, 0);
                loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.9f, 0.9f);

                if (player.getLocation().distanceSquared(loc) < 6) {
                    if (!player.isSneaking()) player.damage(10.0);
                }
            }
        };
        task.runTaskTimer(plugin, 25L, 1L);
        return task;
    }
}

package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * RUSH — faithful recreation:
 * 1. Lights flicker (40 ticks warning)
 * 2. Rush charges through the room along Z axis
 * 3. If player is NOT crouched near a wall/closet → instant death
 */
public class RushEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public RushEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        BukkitRunnable task = new BukkitRunnable() {
            int tick = 0;
            int phase = 0; // 0=flicker, 1=rush
            double rushZ = room.getOrigin().getZ() - 5;
            final double endZ = room.getOrigin().getZ() + room.getLength() + 5;
            final double speed = plugin.getConfig().getDouble("entities.rush.speed-blocks-per-tick", 0.9);
            final int flickerTicks = plugin.getConfig().getInt("entities.rush.flicker-ticks", 40);

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }

                if (phase == 0) {
                    // Flicker phase
                    if (tick % 6 < 3) plugin.getScreamManager().playRushFlicker(player);
                    if (tick == 0) plugin.getScreamManager().playRushWarning(player);
                    if (tick >= flickerTicks) { phase = 1; }
                } else {
                    // Rush charge
                    rushZ += speed;
                    spawnParticles();

                    double pz = player.getLocation().getZ();
                    if (Math.abs(pz - rushZ) < 2.5) {
                        if (isSafe()) {
                            plugin.getScreamManager().playRushPass(player);
                        } else {
                            player.setHealth(0.0);
                            plugin.getScreamManager().playDeath(player);
                            cancel(); return;
                        }
                    }
                    if (rushZ > endZ) cancel();
                }
                tick++;
            }

            private void spawnParticles() {
                double ox = room.getOrigin().getX();
                int oy = room.getOrigin().getBlockY();
                for (double x = ox + 1; x < ox + room.getWidth() - 1; x += 2) {
                    Location loc = new Location(room.getOrigin().getWorld(), x, oy + 2, rushZ);
                    loc.getWorld().spawnParticle(Particle.SMALL_FLAME, loc, 2, 0.2, 0.2, 0.2, 0);
                }
                Location mid = new Location(room.getOrigin().getWorld(),
                        ox + room.getWidth() / 2.0, oy + 1, rushZ);
                mid.getWorld().playSound(mid, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 1.3f + (float)(speed * 0.3));
            }

            /** Player is safe if crouching AND near a wall (within 1.5 blocks of side wall) OR near a closet */
            private boolean isSafe() {
                if (!player.isSneaking()) return false;
                double px = player.getLocation().getX();
                double wallW = room.getOrigin().getX() + 1;
                double wallE = room.getOrigin().getX() + room.getWidth() - 1;
                boolean nearWall = (px - wallW < 1.5) || (wallE - px < 1.5);
                // Also check if near a closet (barrel)
                boolean nearCloset = room.getClosetLocations().stream()
                        .anyMatch(loc -> player.getLocation().distanceSquared(loc) < 4);
                return nearWall || nearCloset;
            }
        };

        task.runTaskTimer(plugin, 1L, 1L);
        return task;
    }
}

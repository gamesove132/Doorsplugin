package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * FIGURE — blind entity with excellent hearing.
 * Patrols room 50 (library) and room 100 (boss).
 *
 * Room 50: player must find 8 books and solve the lock while crouching.
 * Room 100: circuit breaker mini-game (ender chest interaction).
 *
 * Detection: sprinting = loud; walking = safe; crouching = silent.
 * Boss room (100): walking also triggers detection — must crouch.
 */
public class FigureEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;
    private final boolean isBoss;

    public FigureEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
        this.isBoss = (room.getNumber() == 100);
    }

    public BukkitRunnable start() {
        plugin.getScreamManager().playFigureHeartbeat(player);

        BukkitRunnable task = new BukkitRunnable() {
            double fz = room.getCenter().getZ();
            double dir = 1.0;
            final double patrolSpeed = isBoss
                    ? plugin.getConfig().getDouble("entities.figure.patrol-speed", 0.15) * 2
                    : plugin.getConfig().getDouble("entities.figure.patrol-speed", 0.15);
            final double hearRange = isBoss
                    ? plugin.getConfig().getDouble("entities.figure.boss-hearing-range", 8.0)
                    : plugin.getConfig().getDouble("entities.figure.hearing-range", 10.0);
            int heartbeatCooldown = 0;
            boolean alerted = false;
            int alertTick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }

                // Patrol
                fz += dir * patrolSpeed;
                if (fz >= room.getOrigin().getZ() + room.getLength() - 2) dir = -1;
                if (fz <= room.getOrigin().getZ() + 2) dir = 1;

                // Heartbeat
                if (--heartbeatCooldown <= 0) {
                    plugin.getScreamManager().playFigureHeartbeat(player);
                    heartbeatCooldown = 20;
                }

                Location figLoc = new Location(room.getOrigin().getWorld(),
                        room.getCenter().getX(), room.getOrigin().getY() + 1, fz);
                figLoc.getWorld().spawnParticle(Particle.SMOKE, figLoc, 4, 0.4, 1, 0.4, 0.005);

                double dist = player.getLocation().distance(figLoc);

                // Noise detection
                boolean loud = isBoss ? !player.isSneaking() : player.isSprinting();

                if (dist < hearRange && loud) {
                    if (!alerted) {
                        alerted = true; alertTick = 0;
                        plugin.getScreamManager().playFigureDetect(player);
                    }
                    alertTick++;
                    // Rush toward player
                    fz += (player.getLocation().getZ() > fz ? 0.6 : -0.6);

                    plugin.getHudManager().sendActionBar(player,
                            isBoss ? "§5§l☠ ФІГУРА БАЧИТЬ ТЕБЕ!" : "§5§lФІГУРА ПОЧУЛА! ПРИСІДАЙ!");

                    if (dist < 2.0) {
                        player.setHealth(0.0);
                        plugin.getScreamManager().playDeath(player);
                        cancel(); return;
                    }
                } else {
                    alerted = false;
                    if (dist < hearRange * 1.5) {
                        plugin.getHudManager().sendActionBar(player,
                                isBoss ? "§8Тихіше... §7вона поряд." : "§8Йди тихо... §7не поспішай.");
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 5L, 2L);
        return task;
    }
}

package me.doors.entities;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * HALT — spawns at end of dark hallway.
 * Shows "TURN AROUND" text; player must turn and walk backward,
 * then forward again when text disappears — like the Roblox version.
 */
public class HaltEntity {

    private final DoorsPlugin plugin;
    private final Player player;
    private final Room room;

    public HaltEntity(DoorsPlugin plugin, Player player, Room room) {
        this.plugin = plugin; this.player = player; this.room = room;
    }

    public BukkitRunnable start() {
        plugin.getScreamManager().playHaltWarning(player);

        BukkitRunnable task = new BukkitRunnable() {
            int phase = 0;           // 0=forward, 1=turnAround, 2=forward2
            int phaseTick = 0;
            // Halt sits at the far end of the room
            final Location haltLoc = room.getExitDoor() != null
                    ? room.getExitDoor().clone().add(0, 0, 1)
                    : room.getCenter();
            float lastYaw = player.getLocation().getYaw();
            int damage = plugin.getConfig().getInt("entities.halt.damage", 60);

            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getGameManager().isInGame(player)) { cancel(); return; }

                haltLoc.getWorld().spawnParticle(Particle.SOUL, haltLoc, 6, 0.5, 1, 0.5, 0.01);
                phaseTick++;

                switch (phase) {
                    case 0 -> { // Walking toward Halt
                        plugin.getHudManager().sendActionBar(player,
                                "§b§l✦ HALT ✦  §7Щось чекає попереду...");
                        double dist = player.getLocation().distance(haltLoc);
                        if (dist < 4) { phase = 1; phaseTick = 0; }
                        // Slow player
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5, 1, true, false));
                    }
                    case 1 -> { // "TURN AROUND"
                        plugin.getHudManager().sendActionBar(player, "§c§l⚠ РОЗВЕРНИСЬ! ⚠");
                        // Check if player turned ~180°
                        float yaw = player.getLocation().getYaw();
                        float diff = Math.abs(yaw - lastYaw) % 360;
                        if (diff > 180) diff = 360 - diff;
                        if (diff > 100 && phaseTick > 5) {
                            phase = 2; phaseTick = 0;
                        }
                        // Punish if not turning fast enough
                        if (phaseTick > 40) {
                            player.damage(damage);
                            cancel();
                        }
                    }
                    case 2 -> { // Walk forward (away from Halt)
                        plugin.getHudManager().sendActionBar(player, "§a§lЙДИ! Не зупиняйся!");
                        if (phaseTick > 60) cancel(); // Halt fades after ~3s
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 10L, 2L);
        return task;
    }
}

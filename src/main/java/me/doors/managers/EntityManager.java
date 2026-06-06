package me.doors.managers;

import me.doors.DoorsPlugin;
import me.doors.entities.*;
import me.doors.rooms.Room;
import me.doors.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EntityManager {

    private final DoorsPlugin plugin;
    private final Random rng = new Random();
    private final Map<UUID, List<BukkitRunnable>> active = new HashMap<>();

    public EntityManager(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleRoomEnter(Player player, GameSession session, Room room) {
        int n = room.getNumber();

        // Figure — fixed rooms 50 and 100
        List<Integer> figureRooms = plugin.getConfig().getIntegerList("entities.figure.spawn-rooms");
        if (figureRooms.contains(n)) {
            spawn(player, new FigureEntity(plugin, player, room).start());
            return;
        }

        // Seek — fixed trigger rooms
        List<Integer> seekRooms = plugin.getConfig().getIntegerList("entities.seek.trigger-rooms");
        if (seekRooms.contains(n)) {
            warnAndSpawn(player, ColorUtils.cfgMsg("messages.seek-warning"),
                    new SeekEntity(plugin, player, session, room).start());
            return;
        }

        // Halt — after room 60
        if (n >= plugin.getConfig().getInt("entities.halt.first-room", 60)) {
            rollSpawn(player, room, "halt.spawn-chance",
                    () -> new HaltEntity(plugin, player, room).start(),
                    ColorUtils.cfgMsg("messages.halt-warning"));
        }

        // Dupe — after room 21
        if (n >= plugin.getConfig().getInt("entities.dupe.first-room", 21)) {
            rollSpawn(player, room, "dupe.spawn-chance",
                    () -> new DupeEntity(plugin, player, room).start(),
                    ColorUtils.cfgMsg("messages.dupe-warning"));
        }

        // Rush
        rollSpawn(player, room, "rush.spawn-chance",
                () -> new RushEntity(plugin, player, room).start(),
                ColorUtils.cfgMsg("messages.rush-warning"));

        // Ambush (only in AMBUSH type rooms)
        if (room.getType() == Room.Type.AMBUSH) {
            rollSpawn(player, room, "ambush.spawn-chance",
                    () -> new AmbushEntity(plugin, player, room).start(), null);
        }

        // Eyes (only in DARK / AMBUSH rooms)
        if (room.getType() == Room.Type.DARK || room.getType() == Room.Type.AMBUSH) {
            rollSpawn(player, room, "eyes.spawn-chance",
                    () -> new EyesEntity(plugin, player, room).start(),
                    ColorUtils.cfgMsg("messages.eyes-warning"));
        }

        // Screech — dark rooms
        if (room.getType() == Room.Type.DARK) {
            rollSpawn(player, room, "screech.spawn-chance",
                    () -> new ScreechEntity(plugin, player, room).start(), null);
        }
    }

    private void rollSpawn(Player p, Room room, String chanceKey,
                           java.util.function.Supplier<BukkitRunnable> factory, String warning) {
        double chance = plugin.getConfig().getDouble("entities." + chanceKey, 0.1);
        if (rng.nextDouble() < chance) {
            if (warning != null) ColorUtils.msg(p, warning);
            spawn(p, factory.get());
        }
    }

    private void warnAndSpawn(Player p, String warning, BukkitRunnable task) {
        if (warning != null) ColorUtils.msg(p, warning);
        spawn(p, task);
    }

    private void spawn(Player p, BukkitRunnable task) {
        active.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>()).add(task);
    }

    public void cleanupForPlayer(Player p) {
        List<BukkitRunnable> list = active.remove(p.getUniqueId());
        if (list == null) return;
        for (BukkitRunnable r : list) {
            try { r.cancel(); } catch (Exception ignored) {}
        }
    }
}

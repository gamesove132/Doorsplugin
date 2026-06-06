package me.doors.managers;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import me.doors.rooms.RoomGenerator;
import me.doors.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {

    private final DoorsPlugin plugin;
    private final RoomGenerator generator;
    private final Map<String, GameSession> sessions  = new HashMap<>();
    private final Map<UUID, String>        playerSid = new HashMap<>();

    public GameManager(DoorsPlugin plugin) {
        this.plugin    = plugin;
        this.generator = new RoomGenerator(plugin);
    }

    // ── Public API ───────────────────────────────────────────────────

    public GameSession startNewGame() {
        World w = getGameWorld();
        if (w == null) { plugin.getLogger().warning("Game world not found!"); return null; }

        String id = UUID.randomUUID().toString().substring(0, 8);
        plugin.getLogger().info("Generating DOORS for session " + id + " ...");

        int sx = plugin.getConfig().getInt("game.start-x", 0);
        int sy = plugin.getConfig().getInt("game.start-y", 64);
        int sz = plugin.getConfig().getInt("game.start-z", 0);
        List<Room> rooms = generator.generateAll(w, sx, sy, sz);

        GameSession session = new GameSession(id, rooms);
        sessions.put(id, session);
        plugin.getLogger().info("Session " + id + " ready — 100 rooms built.");
        return session;
    }

    public void joinGame(Player player) {
        if (isInGame(player)) { ColorUtils.msg(player, cfg("already-in-game")); return; }

        GameSession session = getWaiting();
        if (session == null) {
            session = startNewGame();
            if (session == null) { ColorUtils.msg(player, "&cНе вдалося створити гру."); return; }
        }

        session.addPlayer(player);
        playerSid.put(player.getUniqueId(), session.getId());
        session.setState(GameSession.State.RUNNING);

        preparePlayer(player);
        teleportToRoom(player, session, 1);
        plugin.getHudManager().updateHUD(player, 1);
        ColorUtils.msg(player, cfg("join"));
        broadcast(session, "&e" + player.getName() + " &7потрапив у DOORS...");
    }

    public void leaveGame(Player player) {
        String sid = playerSid.remove(player.getUniqueId());
        if (sid == null) { ColorUtils.msg(player, cfg("no-game")); return; }
        GameSession s = sessions.get(sid);
        if (s != null) {
            s.removePlayer(player);
            plugin.getEntityManager().cleanupForPlayer(player);
            if (s.isEmpty()) sessions.remove(sid);
        }
        plugin.getHudManager().removeHUD(player);
        returnToLobby(player);
        ColorUtils.msg(player, cfg("leave"));
    }

    /**
     * Called when player right-clicks the exit door.
     * Checks lever requirement, runs elevator cutscene at checkpoints.
     */
    public void tryAdvanceRoom(Player player) {
        GameSession session = getSessionFor(player);
        if (session == null) return;

        Room current = session.getCurrentRoom(player);
        if (current == null) return;

        // Lever room: must pull lever first
        if (current.getType() == Room.Type.LEVER && !current.isLeverPulled()) {
            ColorUtils.msg(player, "&cТреба потягнути важіль перш ніж виходити!");
            return;
        }

        int next = current.getNumber() + 1;
        if (next > 100) { winGame(player, session); return; }

        // Elevator checkpoints (rooms 50 and 100)
        List<Integer> checkpoints = plugin.getConfig().getIntegerList("elevator.checkpoints");
        if (checkpoints.contains(current.getNumber())) {
            playElevatorCutscene(player, session, next);
        } else {
            doAdvance(player, session, next);
        }
    }

    /** Called when a player interacts with a lever room */
    public void onLeverPulled(Player player) {
        GameSession session = getSessionFor(player);
        if (session == null) return;
        Room room = session.getCurrentRoom(player);
        if (room == null || room.getType() != Room.Type.LEVER) return;
        room.setLeverPulled(true);
        ColorUtils.msg(player, "&aВажіль потягнуто! Двері відчинились.");
        player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
        // Open the exit door
        if (room.getExitDoor() != null) {
            openDoorBlock(room.getExitDoor());
        }
    }

    public void playerDied(Player player) {
        GameSession session = getSessionFor(player);
        if (session == null) return;
        int room = session.getPlayerRoom(player);

        broadcast(session, ColorUtils.cfgMsg("messages.death", "{room}", String.valueOf(room)));
        plugin.getEntityManager().cleanupForPlayer(player);
        plugin.getHudManager().removeHUD(player);
        session.removePlayer(player);
        playerSid.remove(player.getUniqueId());
        if (session.isEmpty()) sessions.remove(session.getId());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0); player.setFoodLevel(20);
            returnToLobby(player);
        }, 60L);
    }

    public void forceStopAll() {
        for (GameSession s : new ArrayList<>(sessions.values())) {
            for (UUID uid : new HashSet<>(s.getPlayerUUIDs())) {
                Player p = Bukkit.getPlayer(uid);
                if (p != null) {
                    plugin.getEntityManager().cleanupForPlayer(p);
                    plugin.getHudManager().removeHUD(p);
                    returnToLobby(p);
                }
            }
        }
        sessions.clear(); playerSid.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void doAdvance(Player player, GameSession session, int next) {
        session.setPlayerRoom(player, next);
        Room nextRoom = session.getRoom(next);
        teleportToRoom(player, session, next);
        plugin.getHudManager().updateHUD(player, next);
        plugin.getScreamManager().playRoomEnterSound(player, nextRoom.getType());
        ColorUtils.msg(player, ColorUtils.cfgMsg("messages.room",
                "{room}", String.valueOf(next)));
        plugin.getEntityManager().handleRoomEnter(player, session, nextRoom);
    }

    private void playElevatorCutscene(Player player, GameSession session, int next) {
        int ticks = plugin.getConfig().getInt("elevator.cutscene-ticks", 100);
        ColorUtils.msg(player, ColorUtils.cfgMsg("messages.elevator-arrive"));
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1f, 0.6f);
        player.setGameMode(GameMode.SPECTATOR); // Freeze player

        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                player.setGameMode(GameMode.ADVENTURE);
                ColorUtils.msg(player, ColorUtils.cfgMsg("messages.elevator-depart"));
                doAdvance(player, session, next);
            }
        }.runTaskLater(plugin, ticks);
    }

    public void teleportToRoom(Player player, GameSession session, int n) {
        Room room = session.getRoom(n);
        if (room == null) return;
        player.teleport(room.getSpawnPoint());
        preparePlayer(player);
        giveItems(player, n);
    }

    private void preparePlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0); player.setFoodLevel(20);
        player.getInventory().clear();
    }

    private void giveItems(Player player, int room) {
        player.getInventory().addItem(new ItemStack(Material.TORCH, 16));
        if (room >= 50) player.getInventory().addItem(new ItemStack(Material.CANDLE, 8));
    }

    private void winGame(Player player, GameSession session) {
        broadcast(session, ColorUtils.cfgMsg("messages.win"));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        plugin.getHudManager().removeHUD(player);
        session.removePlayer(player);
        playerSid.remove(player.getUniqueId());
        if (session.isEmpty()) sessions.remove(session.getId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> returnToLobby(player), 120L);
    }

    public void returnToLobby(Player player) {
        String wName = plugin.getConfig().getString("lobby.world", "world");
        World w = Bukkit.getWorld(wName);
        if (w == null) w = Bukkit.getWorlds().get(0);
        double x = plugin.getConfig().getDouble("lobby.spawn-x", 0.5);
        double y = plugin.getConfig().getDouble("lobby.spawn-y", 65.0);
        double z = plugin.getConfig().getDouble("lobby.spawn-z", 0.5);
        player.teleport(new Location(w, x, y, z));
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0); player.setFoodLevel(20);
    }

    private World getGameWorld() {
        String name = plugin.getConfig().getString("game.game-world", "doors_world");
        World w = Bukkit.getWorld(name);
        if (w == null) {
            WorldCreator wc = new WorldCreator(name)
                    .environment(World.Environment.NORMAL)
                    .type(WorldType.FLAT);
            w = wc.createWorld();
            if (w != null) {
                w.setTime(18000);
                w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                w.setDifficulty(Difficulty.HARD);
            }
        }
        return w;
    }

    private void openDoorBlock(Location loc) {
        Block b = loc.getBlock();
        if (b.getBlockData() instanceof org.bukkit.block.data.type.Door d) {
            d.setOpen(true); b.setBlockData(d);
        }
    }

    private GameSession getWaiting() {
        for (GameSession s : sessions.values())
            if (s.getState() == GameSession.State.WAITING) return s;
        return null;
    }

    public GameSession getSessionFor(Player p) {
        String sid = playerSid.get(p.getUniqueId());
        return sid != null ? sessions.get(sid) : null;
    }

    public boolean isInGame(Player p) { return playerSid.containsKey(p.getUniqueId()); }

    /** Alias used by listeners */
    public GameSession getSessionForPlayer(Player p) { return getSessionFor(p); }

    private void broadcast(GameSession s, String msg) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6DOORS&8] ");
        for (UUID uid : s.getPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) p.sendMessage(ColorUtils.color(prefix + msg));
        }
    }

    private String cfg(String key) {
        return plugin.getConfig().getString("messages." + key, key);
    }
}

package me.doors.lobby;

import me.doors.DoorsPlugin;
import me.doors.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Manages 4 lobby portals (like Roblox DOORS lobby).
 *
 * Portal slots: 1=solo, 2=duo, 3=trio, 4=quad.
 * Step on a portal → enter queue.
 * When full OR solo-wait timer expires → game starts.
 */
public class LobbyManager {

    private final DoorsPlugin plugin;
    private final List<LobbyPortal> portals = new ArrayList<>();
    private final Map<UUID, Integer> playerPortal = new HashMap<>(); // uuid → portal slot
    private BukkitRunnable tickTask;

    public LobbyManager(DoorsPlugin plugin) {
        this.plugin = plugin;
        initPortals();
        startTicker();
    }

    // ── Init ─────────────────────────────────────────────────────────

    private void initPortals() {
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        World w = Bukkit.getWorld(lobbyWorld);
        if (w == null) { plugin.getLogger().warning("Lobby world not found: " + lobbyWorld); return; }

        int[] maxPlayers = {1, 2, 3, 4};
        String[] keys = {"portal-1", "portal-2", "portal-3", "portal-4"};

        for (int i = 0; i < 4; i++) {
            double x = plugin.getConfig().getDouble("lobby." + keys[i] + "-x", i * 3 + 0.5);
            double y = plugin.getConfig().getDouble("lobby." + keys[i] + "-y", 65);
            double z = plugin.getConfig().getDouble("lobby." + keys[i] + "-z", 0.5);
            Location center = new Location(w, x, y, z);
            portals.add(new LobbyPortal(i + 1, maxPlayers[i], center));
            buildPortal(w, (int) x, (int) y - 1, (int) z, i + 1);
        }
    }

    /** Builds a small visual portal platform (MAGENTA_GLAZED_TERRACOTTA + nether portal) */
    private void buildPortal(World w, int x, int y, int z, int slot) {
        Material[] colors = {
            Material.YELLOW_GLAZED_TERRACOTTA,
            Material.ORANGE_GLAZED_TERRACOTTA,
            Material.MAGENTA_GLAZED_TERRACOTTA,
            Material.PURPLE_GLAZED_TERRACOTTA
        };
        Material base = colors[slot - 1];

        // 3x3 platform
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                w.getBlockAt(x + dx, y, z + dz).setType(base);
            }
        }
        // Center portal frame (end rod or beacon)
        w.getBlockAt(x, y + 1, z).setType(Material.BEACON);
        // Label sign above
        Block signBlock = w.getBlockAt(x, y + 3, z);
        signBlock.setType(Material.OAK_SIGN);
    }

    // ── Ticker ───────────────────────────────────────────────────────

    private void startTicker() {
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (LobbyPortal portal : portals) {
                    if (portal.isEmpty()) { portal.setCountdown(-1); continue; }

                    // Check who is still standing on the portal
                    checkPlayerPositions(portal);

                    if (portal.isEmpty()) { portal.setCountdown(-1); continue; }

                    if (portal.isFull()) {
                        // Instant start
                        launchPortal(portal);
                    } else {
                        // Solo-wait countdown
                        int soloWait = plugin.getConfig().getInt("lobby.solo-wait-seconds", 15);
                        if (!portal.isCounting()) portal.setCountdown(soloWait);
                        int cd = portal.getCountdown();
                        broadcastPortal(portal, cd);
                        portal.setCountdown(cd - 1);
                        if (cd <= 0) launchPortal(portal);
                    }
                }
            }
        };
        tickTask.runTaskTimer(plugin, 20L, 20L); // every second
    }

    private void checkPlayerPositions(LobbyPortal portal) {
        for (UUID uid : new HashSet<>(portal.getWaiting())) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline()) {
                portal.removePlayer(uid);
                playerPortal.remove(uid);
                continue;
            }
            if (p.getLocation().distanceSquared(portal.getCenter()) > 9) {
                portal.removePlayer(uid);
                playerPortal.remove(uid);
            }
        }
    }

    private void broadcastPortal(LobbyPortal portal, int cd) {
        String template = plugin.getConfig().getString("messages.lobby-portal-wait",
                "&7Гравці: {count}/{max} — старт через &e{time}s");
        String msg = template
                .replace("{count}", String.valueOf(portal.getWaitingCount()))
                .replace("{max}",   String.valueOf(portal.getMaxPlayers()))
                .replace("{time}",  String.valueOf(cd));
        for (UUID uid : portal.getWaiting()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) plugin.getHudManager().sendActionBar(p, ColorUtils.color(msg));
        }
    }

    private void launchPortal(LobbyPortal portal) {
        Set<UUID> players = new HashSet<>(portal.getWaiting());
        portal.clear();
        portal.setCountdown(-1);

        // Remove all from portal tracking
        players.forEach(playerPortal::remove);

        // Start game and join all
        plugin.getGameManager().startNewGame(); // pre-generate
        for (UUID uid : players) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) plugin.getGameManager().joinGame(p);
        }
    }

    // ── Public API ───────────────────────────────────────────────────

    /** Called when player steps on a portal pressure plate / block */
    public void tryJoinPortal(Player player, int slot) {
        if (slot < 1 || slot > 4) return;
        if (plugin.getGameManager().isInGame(player)) {
            ColorUtils.msg(player, "&cВи вже в грі!"); return;
        }
        // Leave previous portal if any
        leavePortal(player);

        LobbyPortal portal = portals.get(slot - 1);
        if (portal.isFull()) {
            ColorUtils.msg(player, "&cПортал " + slot + " повний (" + portal.getMaxPlayers() + "/" + portal.getMaxPlayers() + ")");
            return;
        }

        portal.addPlayer(player.getUniqueId());
        playerPortal.put(player.getUniqueId(), slot);
        player.teleport(portal.getCenter().add(0, 1, 0));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        ColorUtils.msg(player, "&aВи в черзі порталу &e" + slot + " &7(" +
                portal.getWaitingCount() + "/" + portal.getMaxPlayers() + ")");
    }

    /** Called when player right-clicks the selector item — open portal chooser */
    public void openSelectorMenu(Player player) {
        // Build a simple chat-based menu since we have no GUI dependency
        player.sendMessage(ColorUtils.color("&8══════════════════════════════"));
        player.sendMessage(ColorUtils.color("  &6&lDOORS &f— Вибір порталу"));
        player.sendMessage(ColorUtils.color("&8══════════════════════════════"));
        for (LobbyPortal p : portals) {
            String status = p.isFull() ? "&c[ПОВНИЙ]" : ("&a" + p.getWaitingCount() + "/" + p.getMaxPlayers());
            player.sendMessage(ColorUtils.color(
                    "  &e/doors join " + p.getSlot() + "  &7→ Портал " + p.getSlot() +
                    " (" + p.getMaxPlayers() + " гравці)  " + status));
        }
        player.sendMessage(ColorUtils.color("&8══════════════════════════════"));
    }

    public void leavePortal(Player player) {
        Integer slot = playerPortal.remove(player.getUniqueId());
        if (slot == null) return;
        LobbyPortal portal = portals.get(slot - 1);
        portal.removePlayer(player.getUniqueId());
        ColorUtils.msg(player, "&7Ви вийшли з черги порталу " + slot);
    }

    public boolean isInPortal(Player player) { return playerPortal.containsKey(player.getUniqueId()); }

    /** Is this Location on one of the portal platforms? Returns slot (1-4) or -1 */
    public int getPortalSlotAt(Location loc) {
        for (LobbyPortal p : portals) {
            if (loc.getWorld() != null &&
                    loc.getWorld().equals(p.getCenter().getWorld()) &&
                    loc.distanceSquared(p.getCenter()) <= 4) {
                return p.getSlot();
            }
        }
        return -1;
    }

    public List<LobbyPortal> getPortals() { return Collections.unmodifiableList(portals); }

    public void cleanupPortals() {
        if (tickTask != null) tickTask.cancel();
    }
}

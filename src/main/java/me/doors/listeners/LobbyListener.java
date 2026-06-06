package me.doors.listeners;

import me.doors.DoorsPlugin;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;

public class LobbyListener implements Listener {

    private final DoorsPlugin plugin;

    public LobbyListener(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Helper ────────────────────────────────────────────────────────

    private boolean isInLobby(Player p) {
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        return p.getWorld().getName().equals(lobbyWorld);
    }

    // ── Events ────────────────────────────────────────────────────────

    /**
     * Портальна платформа — перевіряємо позицію тільки в лобі-світі.
     * В інших світах (Multiverse) — нічого не робимо.
     */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!isInLobby(player)) return;
        if (plugin.getGameManager().isInGame(player)) return;
        // Перевіряємо тільки коли міняється блок (дешевше)
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        int slot = plugin.getLobbyManager().getPortalSlotAt(player.getLocation());
        if (slot > 0 && !plugin.getLobbyManager().isInPortal(player)) {
            plugin.getLobbyManager().tryJoinPortal(player, slot);
        }
    }

    /**
     * ПКМ сокиркою → меню порталів.
     * Тільки в лобі-світі.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK &&
            e.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player p = e.getPlayer();
        if (!isInLobby(p)) return;
        if (plugin.getGameManager().isInGame(p)) return;
        if (!plugin.getSelectorItem().isSelector(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        plugin.getLobbyManager().openSelectorMenu(p);
    }

    /**
     * Видаємо сокирку тільки коли гравець заходить у лобі-світ.
     * Зайшов через /mvtp, /spawn або просто join — без різниці.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (isInLobby(p)) giveSelectorIfNeeded(p);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (isInLobby(p)) giveSelectorIfNeeded(p);
        // Якщо виходить з лобі — PlayerListener.onWorldChange прибере сокирку
    }

    private void giveSelectorIfNeeded(Player p) {
        for (org.bukkit.inventory.ItemStack item : p.getInventory().getContents()) {
            if (plugin.getSelectorItem().isSelector(item)) return;
        }
        p.getInventory().addItem(plugin.getSelectorItem().create());
    }
}

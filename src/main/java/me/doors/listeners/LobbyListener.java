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

    /** Player moves onto a portal platform — check position each move */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (plugin.getGameManager().isInGame(e.getPlayer())) return;
        // Only check when block changes (cheaper)
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;

        Player player = e.getPlayer();
        int slot = plugin.getLobbyManager().getPortalSlotAt(player.getLocation());
        if (slot > 0 && !plugin.getLobbyManager().isInPortal(player)) {
            plugin.getLobbyManager().tryJoinPortal(player, slot);
        }
    }

    /** Right-click with selector item → open menu */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK &&
            e.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player p = e.getPlayer();
        if (plugin.getGameManager().isInGame(p)) return;
        if (!plugin.getSelectorItem().isSelector(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);
        plugin.getLobbyManager().openSelectorMenu(p);
    }

    /** Give selector item when player joins lobby world */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        if (p.getWorld().getName().equals(lobbyWorld)) {
            giveSelectorIfNeeded(p);
        }
    }

    /** Give selector item when player changes world into lobby */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        if (p.getWorld().getName().equals(lobbyWorld)) {
            giveSelectorIfNeeded(p);
        }
    }

    private void giveSelectorIfNeeded(Player p) {
        // Only give if they don't already have one
        for (org.bukkit.inventory.ItemStack item : p.getInventory().getContents()) {
            if (plugin.getSelectorItem().isSelector(item)) return;
        }
        p.getInventory().addItem(plugin.getSelectorItem().create());
    }
}

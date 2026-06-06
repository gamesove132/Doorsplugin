package me.doors.listeners;

import me.doors.DoorsPlugin;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {

    private final DoorsPlugin plugin;

    public PlayerListener(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!plugin.getGameManager().isInGame(p)) return;
        e.setDeathMessage(null);
        plugin.getGameManager().playerDied(p);
    }

    /** Cancel fall damage inside game */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.getGameManager().isInGame(p)) return;
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.getGameManager().isInGame(p)) plugin.getGameManager().leaveGame(p);
        if (plugin.getLobbyManager().isInPortal(p)) plugin.getLobbyManager().leavePortal(p);
    }

    /** Prevent breaking blocks inside game world */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (plugin.getGameManager().isInGame(p)) { e.setCancelled(true); return; }
        // Also cancel if using selector item anywhere
        if (plugin.getSelectorItem().isSelector(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (plugin.getGameManager().isInGame(e.getPlayer())) e.setCancelled(true);
    }

    /** Prevent selector item drop */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.getSelectorItem().isSelector(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    /** Prevent selector item from being moved into other inventories unexpectedly */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemMove(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getCurrentItem() != null && plugin.getSelectorItem().isSelector(e.getCurrentItem())) {
            // Only allow within player inventory
            if (e.getView().getTopInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) {
                e.setCancelled(true);
            }
        }
    }
}

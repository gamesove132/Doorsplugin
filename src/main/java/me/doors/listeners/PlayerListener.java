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

    // ── Helpers ───────────────────────────────────────────────────────

    /** True if player is in the DOORS lobby world */
    private boolean isInLobby(Player p) {
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");
        return p.getWorld().getName().equals(lobbyWorld);
    }

    /** True if player is in the DOORS game world */
    private boolean isInGameWorld(Player p) {
        String gameWorld = plugin.getConfig().getString("game.game-world", "doors_world");
        return p.getWorld().getName().equals(gameWorld);
    }

    // ── Events ────────────────────────────────────────────────────────

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

    /**
     * Prevent breaking blocks:
     *  - завжди в game world
     *  - тільки в lobby world якщо тримає сокирку (щоб не зломати лобі)
     *  - в інших світах (survival, анархія) — НЕ скасовуємо
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        // Game world — завжди блокуємо
        if (isInGameWorld(p)) { e.setCancelled(true); return; }
        // Lobby world — блокуємо тільки сокирку щоб не ламати платформи порталів
        if (isInLobby(p) && plugin.getSelectorItem().isSelector(p.getInventory().getItemInMainHand())) {
            e.setCancelled(true);
        }
        // Інші світи — нічого не робимо, не заважаємо Multiverse
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        // Тільки в game world
        if (isInGameWorld(e.getPlayer())) e.setCancelled(true);
    }

    /**
     * Prevent selector drop тільки в лобі і game world.
     * В інших світах гравець може дропнути що завгодно.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!isInLobby(p) && !isInGameWorld(p)) return;
        if (plugin.getSelectorItem().isSelector(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    /** Prevent selector item moved in lobby/game */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemMove(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!isInLobby(p) && !isInGameWorld(p)) return;
        if (e.getCurrentItem() != null && plugin.getSelectorItem().isSelector(e.getCurrentItem())) {
            if (e.getView().getTopInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) {
                e.setCancelled(true);
            }
        }
    }

    /** Прибрати сокирку коли гравець виходить з лобі в інший світ */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        String fromWorld = e.getFrom().getName();
        String lobbyWorld = plugin.getConfig().getString("lobby.world", "world");

        // Виходить з лобі → прибрати сокирку щоб не заносити в survival/анархію
        if (fromWorld.equals(lobbyWorld) && !isInGameWorld(p)) {
            p.getInventory().removeIf(item -> plugin.getSelectorItem().isSelector(item));
        }
    }
}

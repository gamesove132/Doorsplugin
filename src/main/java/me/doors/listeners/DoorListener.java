package me.doors.listeners;

import me.doors.DoorsPlugin;
import me.doors.entities.DupeEntity;
import me.doors.managers.GameSession;
import me.doors.rooms.Room;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Lever;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;

public class DoorListener implements Listener {

    private final DoorsPlugin plugin;
    private static final double DOOR_RADIUS_SQ = 2.25; // 1.5^2

    public DoorListener(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;

        Player player = e.getPlayer();
        if (!plugin.getGameManager().isInGame(player)) return;

        GameSession session = plugin.getGameManager().getSessionForPlayer(player);
        if (session == null) return;

        Room room = session.getCurrentRoom(player);
        if (room == null) return;

        // ── Lever interaction ─────────────────────────────────────────
        if (block.getBlockData() instanceof Lever) {
            if (room.getLeverLocation() != null &&
                    block.getLocation().distanceSquared(room.getLeverLocation()) <= DOOR_RADIUS_SQ) {
                plugin.getGameManager().onLeverPulled(player);
                e.setCancelled(true);
            }
            return;
        }

        // ── Door interaction ──────────────────────────────────────────
        if (!isDoor(block.getType())) return;

        // Normalize to lower half
        Block lower = getLowerDoor(block);

        // Is this the EXIT door?
        if (room.getExitDoor() != null &&
                lower.getLocation().distanceSquared(room.getExitDoor()) <= DOOR_RADIUS_SQ) {
            // Open door visually first
            openDoor(lower);
            plugin.getGameManager().tryAdvanceRoom(player);
            e.setCancelled(true);
            return;
        }

        // Is this a DUPE door? (any other door in the room that is not entrance)
        if (room.getEntranceDoor() != null &&
                lower.getLocation().distanceSquared(room.getEntranceDoor()) > DOOR_RADIUS_SQ &&
                lower.getLocation().distanceSquared(room.getExitDoor()) > DOOR_RADIUS_SQ) {
            // This is a fake/extra door — Dupe trigger
            int roomNum = room.getNumber();
            int dupeFirst = plugin.getConfig().getInt("entities.dupe.first-room", 21);
            if (roomNum >= dupeFirst) {
                DupeEntity.onDupeTriggered(plugin, player);
                e.setCancelled(true);
            }
        }
    }

    private Block getLowerDoor(Block b) {
        if (b.getBlockData() instanceof Door d) {
            if (d.getHalf() == org.bukkit.block.data.Bisected.Half.TOP) {
                return b.getRelative(BlockFace.DOWN);
            }
        }
        return b;
    }

    private void openDoor(Block lower) {
        if (lower.getBlockData() instanceof Door d) {
            d.setOpen(true);
            lower.setBlockData(d);
            Block upper = lower.getRelative(BlockFace.UP);
            if (upper.getBlockData() instanceof Door du) {
                du.setOpen(true);
                upper.setBlockData(du);
            }
        }
    }

    private boolean isDoor(Material m) {
        return switch (m) {
            case OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR,
                    ACACIA_DOOR, DARK_OAK_DOOR, MANGROVE_DOOR,
                    CHERRY_DOOR, BAMBOO_DOOR, IRON_DOOR -> true;
            default -> false;
        };
    }
}

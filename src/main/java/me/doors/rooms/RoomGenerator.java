package me.doors.rooms;

import me.doors.DoorsPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.Powerable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class RoomGenerator {

    private final DoorsPlugin plugin;
    private final Random rng = new Random();
    private final int W, H, L, GAP;

    public RoomGenerator(DoorsPlugin plugin) {
        this.plugin = plugin;
        W   = plugin.getConfig().getInt("game.room-width",   15);
        H   = plugin.getConfig().getInt("game.room-height",   8);
        L   = plugin.getConfig().getInt("game.room-length",  20);
        GAP = plugin.getConfig().getInt("game.corridor-gap",  1);
    }

    public List<Room> generateAll(World world, int sx, int sy, int sz) {
        List<Room> rooms = new ArrayList<>();
        int z = sz;
        for (int i = 1; i <= 100; i++) {
            Room.Type type = pickType(i);
            Room room = new Room(i, type, new Location(world, sx, sy, z), W, H, L);
            buildRoom(room, world);
            rooms.add(room);
            z += L + GAP;
        }
        // Build elevator rooms at 50 and 100
        buildElevatorShaft(world, rooms.get(49)); // 0-based
        buildElevatorShaft(world, rooms.get(99));
        return rooms;
    }

    // ────────────────────────────────────────────────────────────────
    //  Type selection
    // ────────────────────────────────────────────────────────────────

    private Room.Type pickType(int n) {
        if (n == 1)   return Room.Type.NORMAL;
        if (n == 50)  return Room.Type.LIBRARY;
        if (n == 100) return Room.Type.BOSS;
        // Checkpoints have elevator rooms built on top
        int nW = plugin.getConfig().getInt("rooms.normal-weight",   50);
        int dW = plugin.getConfig().getInt("rooms.dark-weight",     22);
        int aW = plugin.getConfig().getInt("rooms.ambush-weight",   10);
        int tW = plugin.getConfig().getInt("rooms.treasure-weight", 12);
        int lW = plugin.getConfig().getInt("rooms.lever-weight",     6);
        int total = nW + dW + aW + tW + lW;
        int roll = rng.nextInt(total);
        if (roll < nW)                   return Room.Type.NORMAL;
        else if (roll < nW+dW)           return Room.Type.DARK;
        else if (roll < nW+dW+aW)        return Room.Type.AMBUSH;
        else if (roll < nW+dW+aW+tW)     return Room.Type.TREASURE;
        else                             return Room.Type.LEVER;
    }

    // ────────────────────────────────────────────────────────────────
    //  Room construction
    // ────────────────────────────────────────────────────────────────

    private void buildRoom(Room room, World w) {
        int ox = room.getOrigin().getBlockX();
        int oy = room.getOrigin().getBlockY();
        int oz = room.getOrigin().getBlockZ();

        Material wall    = wallMat(room.getType());
        Material floor   = floorMat(room.getType());
        Material ceiling = ceilMat(room.getType());

        // Scaffold
        for (int x = ox; x <= ox + W; x++) {
            for (int z = oz; z <= oz + L; z++) {
                w.getBlockAt(x, oy,     z).setType(floor);
                w.getBlockAt(x, oy + H, z).setType(ceiling);
                for (int y = oy + 1; y < oy + H; y++) {
                    boolean isWall = (x == ox || x == ox + W || z == oz || z == oz + L);
                    w.getBlockAt(x, y, z).setType(isWall ? wall : Material.AIR);
                }
            }
        }

        // Entrance (north wall, room > 1)
        int midX = ox + W / 2;
        if (room.getNumber() > 1) {
            carveAndPlaceDoor(w, midX, oy + 1, oz, BlockFace.SOUTH, room, false);
        }
        // Exit (south wall)
        carveAndPlaceDoor(w, midX, oy + 1, oz + L, BlockFace.NORTH, room, true);

        // Lighting
        if (room.getType() != Room.Type.DARK && room.getType() != Room.Type.AMBUSH) {
            placeLighting(room, w, ox, oy, oz);
        }

        // Type-specific contents
        switch (room.getType()) {
            case TREASURE -> placeTreasure(room, w, ox, oy, oz);
            case LIBRARY  -> placeLibrary(room, w, ox, oy, oz);
            case BOSS     -> placeBossRoom(room, w, ox, oy, oz);
            case LEVER    -> placeLever(room, w, ox, oy, oz);
            default       -> placeClosets(room, w, ox, oy, oz);
        }
    }

    private void carveAndPlaceDoor(World w, int x, int y, int z,
                                   BlockFace facing, Room room, boolean isExit) {
        // Carve 2 blocks tall opening in wall
        w.getBlockAt(x, y,     z).setType(Material.AIR);
        w.getBlockAt(x, y + 1, z).setType(Material.AIR);

        // Place door
        Block lower = w.getBlockAt(x, y, z);
        Block upper = w.getBlockAt(x, y + 1, z);
        lower.setType(Material.OAK_DOOR);
        upper.setType(Material.OAK_DOOR);

        if (lower.getBlockData() instanceof Door d) {
            d.setFacing(facing); d.setHalf(Bisected.Half.BOTTOM); d.setOpen(false);
            lower.setBlockData(d);
        }
        if (upper.getBlockData() instanceof Door d) {
            d.setFacing(facing); d.setHalf(Bisected.Half.TOP); d.setOpen(false);
            upper.setBlockData(d);
        }

        if (isExit) room.setExitDoor(lower.getLocation());
        else        room.setEntranceDoor(lower.getLocation());
    }

    private void placeLighting(Room room, World w, int ox, int oy, int oz) {
        for (int x = ox + 2; x < ox + W; x += 4) {
            for (int z = oz + 3; z < oz + L - 1; z += 5) {
                w.getBlockAt(x, oy + H - 1, z).setType(Material.LANTERN);
            }
        }
    }

    private void placeClosets(Room room, World w, int ox, int oy, int oz) {
        // 1-2 closets (barrels) per room so players can hide from Rush
        int count = 1 + rng.nextInt(2);
        for (int i = 0; i < count; i++) {
            int side = (i % 2 == 0) ? ox + 1 : ox + W - 2;
            int cz = oz + 3 + rng.nextInt(Math.max(1, L - 6));
            Block b = w.getBlockAt(side, oy + 1, cz);
            if (b.getType() == Material.AIR) {
                b.setType(Material.BARREL);
                room.getClosetLocations().add(b.getLocation());
            }
        }
    }

    private void placeTreasure(Room room, World w, int ox, int oy, int oz) {
        placeLighting(room, w, ox, oy, oz);
        // Two chests
        for (int i = 0; i < 2; i++) {
            int cx = ox + 2 + i * 3, cz = oz + L / 2;
            Block b = w.getBlockAt(cx, oy + 1, cz);
            b.setType(Material.CHEST);
            room.getChestLocations().add(b.getLocation());
            // Fill chest
            if (b.getState() instanceof org.bukkit.block.Chest chest) {
                fillTreasureChest(chest.getInventory());
                chest.update();
            }
        }
    }

    private void fillTreasureChest(Inventory inv) {
        Material[] loot = {
            Material.GOLDEN_APPLE, Material.TORCH, Material.BREAD,
            Material.CANDLE, Material.GLOW_BERRIES, Material.ENDER_PEARL
        };
        for (int i = 0; i < 3; i++) {
            int slot = rng.nextInt(27);
            int qty  = 1 + rng.nextInt(4);
            inv.setItem(slot, new ItemStack(loot[rng.nextInt(loot.length)], qty));
        }
    }

    private void placeLibrary(Room room, World w, int ox, int oy, int oz) {
        // Very dim candles only
        for (int x = ox + 3; x < ox + W - 2; x += 5) {
            w.getBlockAt(x, oy + 1, oz + L / 2).setType(Material.CANDLE);
        }
        // Bookshelves along both side walls
        for (int z = oz + 1; z < oz + L - 1; z++) {
            for (int y = oy + 1; y <= oy + 3; y++) {
                w.getBlockAt(ox + 1,     y, z).setType(Material.BOOKSHELF);
                w.getBlockAt(ox + W - 2, y, z).setType(Material.BOOKSHELF);
            }
        }
        // Place 8 books (item frames with books on shelves)
        placeBooks(room, w, ox, oy, oz);
        // Desk with code-locked chest (puzzle chest)
        Block desk = w.getBlockAt(ox + W / 2, oy + 1, oz + L - 4);
        desk.setType(Material.ENDER_CHEST); // Represents the combination lock
        room.getChestLocations().add(desk.getLocation());
    }

    private void placeBooks(Room room, World w, int ox, int oy, int oz) {
        List<int[]> spots = new ArrayList<>();
        for (int z = oz + 2; z < oz + L - 2; z += 3) {
            spots.add(new int[]{ox + 1, oy + 2, z});
            spots.add(new int[]{ox + W - 2, oy + 2, z});
        }
        Collections.shuffle(spots, rng);
        for (int i = 0; i < Math.min(8, spots.size()); i++) {
            int[] p = spots.get(i);
            Location loc = new Location(w, p[0], p[1], p[2]);
            room.getBookLocations().add(loc);
        }
    }

    private void placeBossRoom(Room room, World w, int ox, int oy, int oz) {
        // Obsidian floor
        for (int x = ox; x <= ox + W; x++)
            for (int z = oz; z <= oz + L; z++)
                w.getBlockAt(x, oy, z).setType(Material.OBSIDIAN);
        // Pillars
        for (int px : new int[]{ox + 2, ox + W - 2}) {
            for (int pz : new int[]{oz + 2, oz + L - 2}) {
                for (int y = oy + 1; y < oy + H; y++)
                    w.getBlockAt(px, y, pz).setType(Material.OBSIDIAN);
            }
        }
        // Soul fire + lanterns
        w.getBlockAt(ox + W / 2, oy + 1, oz + L / 2).setType(Material.SOUL_FIRE);
        w.getBlockAt(ox + W / 2, oy + H - 1, oz + L / 4 + 1).setType(Material.SOUL_LANTERN);
        w.getBlockAt(ox + W / 2, oy + H - 1, oz + 3 * L / 4 - 1).setType(Material.SOUL_LANTERN);
        // Circuit breaker chest (represents puzzle)
        Block puzzle = w.getBlockAt(ox + W / 2, oy + 1, oz + L - 3);
        puzzle.setType(Material.ENDER_CHEST);
        room.getChestLocations().add(puzzle.getLocation());
    }

    private void placeLever(Room room, World w, int ox, int oy, int oz) {
        placeLighting(room, w, ox, oy, oz);
        // Lever on back wall of a small alcove
        Block base = w.getBlockAt(ox + W / 2, oy + 1, oz + L - 3);
        base.setType(Material.STONE);
        Block leverBlock = w.getBlockAt(ox + W / 2, oy + 2, oz + L - 3);
        leverBlock.setType(Material.LEVER);
        if (leverBlock.getBlockData() instanceof Powerable lev) {
            lev.setPowered(false);
            leverBlock.setBlockData(lev);
        }
        room.setLeverLocation(leverBlock.getLocation());
    }

    private void buildElevatorShaft(World w, Room room) {
        int ox = room.getOrigin().getBlockX();
        int oy = room.getOrigin().getBlockY();
        int oz = room.getOrigin().getBlockZ();
        // Build a 5×5×8 elevator shaft on the side of the room
        int ex = ox + W + 2;
        for (int x = ex; x <= ex + 4; x++) {
            for (int y = oy; y <= oy + H; y++) {
                for (int z = oz + L / 2 - 2; z <= oz + L / 2 + 2; z++) {
                    boolean isWall = (x == ex || x == ex + 4 ||
                                      z == oz + L / 2 - 2 || z == oz + L / 2 + 2);
                    Material mat = isWall ? Material.IRON_BLOCK :
                                   (y == oy ? Material.STONE : Material.AIR);
                    if (y == oy + H) mat = Material.IRON_BLOCK;
                    w.getBlockAt(x, y, z).setType(mat);
                }
            }
        }
        // Lanterns in shaft
        w.getBlockAt(ex + 2, oy + H - 1, oz + L / 2).setType(Material.LANTERN);
        Location elevLoc = new Location(w, ex + 2.5, oy + 1, oz + L / 2 + 0.5);
        room.setElevatorLocation(elevLoc);
    }

    // ────────────────────────────────────────────────────────────────
    //  Material helpers
    // ────────────────────────────────────────────────────────────────

    private Material wallMat(Room.Type t) {
        return switch (t) {
            case BOSS, LIBRARY  -> Material.SMOOTH_STONE;
            case DARK, AMBUSH   -> Material.DEEPSLATE_BRICKS;
            case TREASURE       -> Material.POLISHED_DIORITE;
            default             -> Material.STONE_BRICKS;
        };
    }

    private Material floorMat(Room.Type t) {
        return switch (t) {
            case BOSS     -> Material.OBSIDIAN;
            case LIBRARY  -> Material.DARK_OAK_PLANKS;
            case TREASURE -> Material.POLISHED_GRANITE;
            case DARK, AMBUSH -> Material.DEEPSLATE;
            default       -> Material.STONE;
        };
    }

    private Material ceilMat(Room.Type t) {
        return switch (t) {
            case BOSS         -> Material.BLACKSTONE;
            case DARK, AMBUSH -> Material.DEEPSLATE_TILES;
            default           -> Material.STONE_BRICKS;
        };
    }
}

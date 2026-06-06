package me.doors.rooms;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

public class Room {

    public enum Type {
        NORMAL,    // Rooms 1-49, 51-99 (basic)
        DARK,      // No lighting, Screech danger
        AMBUSH,    // Ambush entity spawns here
        TREASURE,  // Chests with items
        LEVER,     // Must pull lever to proceed
        LIBRARY,   // Room 50 — Figure boss, book puzzle
        ELEVATOR,  // Rooms 50+100 — checkpoint lift
        BOSS       // Room 100 — Figure boss, circuit puzzle
    }

    private final int number;
    private final Type type;
    private final Location origin;
    private final int width, height, length;

    // Door positions
    private Location entranceDoor;
    private Location exitDoor;

    // Special locations
    private Location leverLocation;
    private Location elevatorLocation;
    private final List<Location> chestLocations   = new ArrayList<>();
    private final List<Location> closetLocations  = new ArrayList<>();
    private final List<Location> bookLocations     = new ArrayList<>();

    private boolean leverPulled = false;
    private boolean cleared     = false;

    public Room(int number, Type type, Location origin, int w, int h, int l) {
        this.number = number; this.type = type;
        this.origin = origin.clone();
        this.width = w; this.height = h; this.length = l;
    }

    // ── Getters / setters ────────────────────────────────────────────

    public int getNumber()   { return number; }
    public Type getType()    { return type; }
    public Location getOrigin() { return origin.clone(); }
    public int getWidth()    { return width; }
    public int getHeight()   { return height; }
    public int getLength()   { return length; }

    public Location getEntranceDoor() { return entranceDoor != null ? entranceDoor.clone() : null; }
    public void setEntranceDoor(Location l) { this.entranceDoor = l.clone(); }

    public Location getExitDoor() { return exitDoor != null ? exitDoor.clone() : null; }
    public void setExitDoor(Location l) { this.exitDoor = l.clone(); }

    public Location getLeverLocation() { return leverLocation != null ? leverLocation.clone() : null; }
    public void setLeverLocation(Location l) { this.leverLocation = l.clone(); }

    public Location getElevatorLocation() { return elevatorLocation != null ? elevatorLocation.clone() : null; }
    public void setElevatorLocation(Location l) { this.elevatorLocation = l.clone(); }

    public List<Location> getChestLocations()  { return chestLocations; }
    public List<Location> getClosetLocations() { return closetLocations; }
    public List<Location> getBookLocations()   { return bookLocations; }

    public boolean isLeverPulled() { return leverPulled; }
    public void setLeverPulled(boolean v) { this.leverPulled = v; }

    public boolean isCleared() { return cleared; }
    public void setCleared(boolean v) { this.cleared = v; }

    /** Teleport spawn: slightly inside room, past entrance */
    public Location getSpawnPoint() {
        World w = origin.getWorld();
        return new Location(w,
                origin.getX() + width / 2.0,
                origin.getY() + 1,
                origin.getZ() + 2.5,
                180f, 0f);
    }

    /** Center of the room at floor level */
    public Location getCenter() {
        World w = origin.getWorld();
        return new Location(w,
                origin.getX() + width / 2.0,
                origin.getY(),
                origin.getZ() + length / 2.0);
    }
}

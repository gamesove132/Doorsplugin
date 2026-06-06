package me.doors.lobby;

import org.bukkit.Location;

import java.util.*;

/**
 * Represents one lobby portal (1-4 players max).
 */
public class LobbyPortal {

    private final int slot;          // 1,2,3,4
    private final int maxPlayers;    // how many players this portal allows
    private final Location center;
    private final Set<UUID> waiting = new LinkedHashSet<>();
    private int countdown = -1;      // -1 = not counting; >0 = seconds left

    public LobbyPortal(int slot, int maxPlayers, Location center) {
        this.slot = slot;
        this.maxPlayers = maxPlayers;
        this.center = center.clone();
    }

    public int getSlot()       { return slot; }
    public int getMaxPlayers() { return maxPlayers; }
    public Location getCenter(){ return center.clone(); }

    public Set<UUID> getWaiting()     { return Collections.unmodifiableSet(waiting); }
    public int getWaitingCount()      { return waiting.size(); }
    public boolean isFull()           { return waiting.size() >= maxPlayers; }
    public boolean isEmpty()          { return waiting.isEmpty(); }
    public boolean hasPlayer(UUID id) { return waiting.contains(id); }

    public void addPlayer(UUID id)    { waiting.add(id); }
    public void removePlayer(UUID id) { waiting.remove(id); }
    public void clear()               { waiting.clear(); }

    public int getCountdown()         { return countdown; }
    public void setCountdown(int v)   { this.countdown = v; }
    public boolean isCounting()       { return countdown >= 0; }
}

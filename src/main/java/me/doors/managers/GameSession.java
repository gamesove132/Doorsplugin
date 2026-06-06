package me.doors.managers;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.entity.Player;

import java.util.*;

public class GameSession {

    public enum State { WAITING, RUNNING, FINISHED }

    private final String id;
    private State state = State.WAITING;
    private final List<Room> rooms;
    private final Set<UUID> players           = new LinkedHashSet<>();
    private final Map<UUID, Integer> roomNums = new HashMap<>();

    public GameSession(String id, List<Room> rooms) {
        this.id = id; this.rooms = rooms;
    }

    public String getId()    { return id; }
    public State getState()  { return state; }
    public void setState(State s) { this.state = s; }
    public List<Room> getRooms() { return rooms; }

    public void addPlayer(Player p) {
        players.add(p.getUniqueId());
        roomNums.put(p.getUniqueId(), 1);
    }

    public void removePlayer(Player p) {
        players.remove(p.getUniqueId());
        roomNums.remove(p.getUniqueId());
    }

    public boolean hasPlayer(Player p) { return players.contains(p.getUniqueId()); }
    public Set<UUID> getPlayerUUIDs()  { return Collections.unmodifiableSet(players); }
    public boolean isEmpty()           { return players.isEmpty(); }

    public int getPlayerRoom(Player p)            { return roomNums.getOrDefault(p.getUniqueId(), 1); }
    public void setPlayerRoom(Player p, int room) { roomNums.put(p.getUniqueId(), room); }

    public Room getRoom(int n) {
        if (n < 1 || n > rooms.size()) return null;
        return rooms.get(n - 1);
    }

    public Room getCurrentRoom(Player p) { return getRoom(getPlayerRoom(p)); }
}

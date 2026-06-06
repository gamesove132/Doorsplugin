package me.doors;

import me.doors.items.SelectorItem;
import me.doors.listeners.DoorListener;
import me.doors.listeners.LobbyListener;
import me.doors.listeners.PlayerListener;
import me.doors.managers.*;
import me.doors.lobby.LobbyManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DoorsPlugin extends JavaPlugin {

    private static DoorsPlugin instance;

    private GameManager gameManager;
    private HUDManager hudManager;
    private EntityManager entityManager;
    private ScreamManager screamManager;
    private LobbyManager lobbyManager;
    private SelectorItem selectorItem;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.selectorItem  = new SelectorItem(this);
        this.screamManager = new ScreamManager(this);
        this.hudManager    = new HUDManager(this);
        this.entityManager = new EntityManager(this);
        this.gameManager   = new GameManager(this);
        this.lobbyManager  = new LobbyManager(this);

        getServer().getPluginManager().registerEvents(new DoorListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);

        DoorsCommand cmd = new DoorsCommand(this);
        PluginCommand doors = getCommand("doors");
        if (doors != null) { doors.setExecutor(cmd); doors.setTabCompleter(cmd); }

        PluginCommand give = getCommand("doorsgive");
        if (give != null) give.setExecutor(cmd);

        getLogger().info("DoorsPlugin v2.0 enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceStopAll();
        if (lobbyManager != null) lobbyManager.cleanupPortals();
        getLogger().info("DoorsPlugin disabled.");
    }

    public static DoorsPlugin getInstance() { return instance; }
    public GameManager    getGameManager()  { return gameManager; }
    public HUDManager     getHudManager()   { return hudManager; }
    public EntityManager  getEntityManager(){ return entityManager; }
    public ScreamManager  getScreamManager(){ return screamManager; }
    public LobbyManager   getLobbyManager() { return lobbyManager; }
    public SelectorItem   getSelectorItem() { return selectorItem; }
}

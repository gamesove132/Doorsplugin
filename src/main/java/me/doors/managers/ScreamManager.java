package me.doors.managers;

import me.doors.DoorsPlugin;
import me.doors.rooms.Room;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ScreamManager {

    private final DoorsPlugin plugin;

    public ScreamManager(DoorsPlugin plugin) {
        this.plugin = plugin;
    }

    public void playRoomEnterSound(Player p, Room.Type type) {
        switch (type) {
            case DARK, AMBUSH -> {
                p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 0.7f, 0.6f);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 0));
            }
            case TREASURE -> p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1.2f);
            case LIBRARY  -> {
                p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 0.3f, 0.4f);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0));
            }
            case BOSS     -> {
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 0.5f);
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
            }
            default -> p.playSound(p.getLocation(), Sound.BLOCK_WOODEN_DOOR_OPEN, 0.7f, 1.0f);
        }
    }

    // Rush
    public void playRushFlicker(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
    }
    public void playRushWarning(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 0.4f);
        p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.3f);
    }
    public void playRushPass(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.5f, 1.6f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 1));
    }

    // Eyes
    public void playEyesAmbient(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.4f, 0.5f);
    }

    // Screech
    public void playScreechPsst(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_SILVERFISH_AMBIENT, 0.8f, 1.5f);
    }
    public void playScreechAttack(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_BITE, 1f, 0.4f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 50, 0));
    }

    // Halt
    public void playHaltWarning(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 1f, 0.3f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 1));
    }

    // Seek
    public void playSeekApproach(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_RAVAGER_STEP, 1f, 0.3f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 30, 0));
    }

    // Figure
    public void playFigureHeartbeat(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.7f, 0.5f);
    }
    public void playFigureDetect(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_NEARBY_CLOSER, 1f, 0.7f);
    }

    // Death
    public void playDeath(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 0.8f);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 0.4f);
    }
}

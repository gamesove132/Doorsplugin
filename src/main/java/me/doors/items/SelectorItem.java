package me.doors.items;

import me.doors.DoorsPlugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Custom selector item — an Iron Axe that:
 *  - Has custom name/lore
 *  - Is tagged with NBT so it's identified uniquely
 *  - Cannot be used to break blocks (handled in LobbyListener)
 *  - Can only be stacked to 1
 *  - Triggers the lobby portal UI when right-clicked
 */
public class SelectorItem {

    public static final String NBT_KEY = "doors_selector";

    private final DoorsPlugin plugin;
    private final NamespacedKey key;

    public SelectorItem(DoorsPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, NBT_KEY);
    }

    /** Create a new selector item stack */
    public ItemStack create() {
        Material mat = Material.matchMaterial(
                plugin.getConfig().getString("selector-item.material", "IRON_AXE"));
        if (mat == null) mat = Material.IRON_AXE;

        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                "&6&lDOORS &f— &eВибір лобі"));
        meta.setLore(List.of(
                ChatColor.translateAlternateColorCodes('&', "&7ПКМ: &fВибрати портал / кількість гравців"),
                ChatColor.translateAlternateColorCodes('&', "&8Натисни ПКМ в лобі для вибору"),
                "",
                ChatColor.translateAlternateColorCodes('&', "&8[&6DOORS&8]")
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

        // NBT tag — uniquely identifies this item
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /** Check if an item is the DOORS selector */
    public boolean isSelector(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public NamespacedKey getKey() { return key; }
}

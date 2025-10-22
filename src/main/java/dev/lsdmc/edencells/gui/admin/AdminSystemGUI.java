package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;


public final class AdminSystemGUI {

    public static final String HOLDER_KEY = "system";

    private final EdenCells plugin;
    private final NamespacedKey actionKey;

    public AdminSystemGUI(EdenCells plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "sys_action");
    }

    public void open(Player admin) {
        Inventory inv = Bukkit.createInventory(new AdminGuiHolder(HOLDER_KEY), 27,
                MessageUtils.fromMiniMessage("<color:#9D4EDD>System</color>"));

        
        inv.setItem(18, btn(Material.BARRIER, "Back", "back_dashboard", List.of()));

        inv.setItem(10, btn(Material.MAP, "Quick Check", "quick_check", List.of(
                Component.text("Validate groups/regions (non-destructive)", NamedTextColor.GRAY)
        )));

        inv.setItem(11, btn(Material.RECOVERY_COMPASS, "Full Sync", "full_sync", List.of(
                Component.text("Validate, cleanup doors, sync ownership", NamedTextColor.GRAY)
        )));

        inv.setItem(12, btn(Material.IRON_DOOR, "Cleanup Doors", "cleanup_doors", List.of(
                Component.text("Remove invalid/orphaned links", NamedTextColor.GRAY)
        )));

        inv.setItem(13, btn(Material.BOOK, "Reload", "reload_all", List.of(
                Component.text("Reload configs, groups, NPCs", NamedTextColor.GRAY)
        )));

        inv.setItem(14, btn(Material.WRITABLE_BOOK, "Save All", "save_all", List.of(
                Component.text("Persist groups, doors, NPCs", NamedTextColor.GRAY)
        )));

        inv.setItem(15, btn(Material.ENDER_EYE, "NPC Reload", "npc_reload", List.of(
                Component.text("Reload teleport NPCs config", NamedTextColor.GRAY)
        )));

        inv.setItem(16, btn(Material.COMPARATOR, "Rate Limit Stats", "rl_stats", List.of(
                Component.text("Show active limiter entries", NamedTextColor.GRAY)
        )));

        admin.openInventory(inv);
    }

    private ItemStack btn(Material mat, String title, String action, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GOLD));
        if (lore != null && !lore.isEmpty()) meta.lore(new ArrayList<>(lore));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }
}



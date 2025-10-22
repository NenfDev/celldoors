package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class AdminDashboardGUI {

    private final EdenCells plugin;

    public AdminDashboardGUI(EdenCells plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AdminGuiHolder("dashboard"), 27,
                MessageUtils.fromMiniMessage("<color:#9D4EDD>Admin Dashboard</color>"));

        
        inv.setItem(11, button(Material.PLAYER_HEAD, Component.text("Players", NamedTextColor.GOLD),
                List.of(Component.text("Manage players & cells", NamedTextColor.YELLOW))));

        
        inv.setItem(13, button(Material.CHEST, Component.text("Groups", NamedTextColor.GOLD),
                List.of(Component.text("Manage cell groups", NamedTextColor.YELLOW))));

        
        inv.setItem(15, button(Material.COMPASS, Component.text("System", NamedTextColor.GOLD),
                List.of(Component.text("Sync & diagnostics", NamedTextColor.YELLOW))));

        player.openInventory(inv);
    }

    private ItemStack button(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(new ArrayList<>(lore));
        item.setItemMeta(meta);
        return item;
    }
}



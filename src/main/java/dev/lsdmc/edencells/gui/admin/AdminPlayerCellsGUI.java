package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class AdminPlayerCellsGUI {

    public static final String HOLDER_KEY = "player_cells";

    private final EdenCells plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey regionKey;
    private final NamespacedKey playerKey;

    public AdminPlayerCellsGUI(EdenCells plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "pc_action");
        this.regionKey = new NamespacedKey(plugin, "pc_region");
        this.playerKey = new NamespacedKey(plugin, "pc_player");
    }

    public void open(Player admin, OfflinePlayer target) {
        open(admin, target, null);
    }

    public void open(Player admin, OfflinePlayer target, String selectedRegionId) {
        CellManager cm = plugin.getCellManager();
        List<Region> cells = cm.getPlayerCells(target);

        String rawName = Objects.toString(target.getName(), "Unknown");
        
        String safeName = rawName.replaceAll("§.", "");
        safeName = safeName.replaceAll("&[0-9a-fk-orxA-FK-ORX]", "");
        net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.text("Player Cells: ", NamedTextColor.LIGHT_PURPLE)
                .append(net.kyori.adventure.text.Component.text(safeName, NamedTextColor.LIGHT_PURPLE));
        Inventory inv = Bukkit.createInventory(new AdminGuiHolder(HOLDER_KEY), 54, title);

        int slot = 0;
        for (Region cell : cells) {
            inv.setItem(slot++, cellItem(cell, target));
            if (slot >= 45) break;
        }

        
        
        String label = selectedRegionId != null ? ("Selected: " + selectedRegionId) : "Selected: (click a cell)";
        inv.setItem(48, info(Material.PAPER, label));

        inv.setItem(45, tool(Material.ENDER_PEARL, "Teleport", "tp_cell", selectedRegionId, target));
        inv.setItem(46, tool(Material.BARRIER, "Back", "back_players", selectedRegionId, target));
        inv.setItem(47, tool(Material.HOPPER, "Set Owner", "set_owner", selectedRegionId, target));
        inv.setItem(49, info(Material.PAPER, "Owner: " + Objects.toString(target.getName(), "Unknown")));
        inv.setItem(51, tool(Material.REDSTONE, "Remove Member", "remove_member", selectedRegionId, target));
        inv.setItem(52, tool(Material.SLIME_BALL, "Add Member", "add_member", selectedRegionId, target));
        inv.setItem(53, tool(Material.BOOK, "Sell/Unrent", "sell_unrent", selectedRegionId, target));

        admin.openInventory(inv);
    }

    private ItemStack cellItem(Region cell, OfflinePlayer target) {
        ItemStack item = new ItemStack(Material.OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(cell.getRegion().getId(), NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        boolean sold = plugin.getCellManager().isSold(cell);
        lore.add(Component.text("Status: " + (sold ? "Owned/Rented" : "Available"), sold ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        meta.lore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, "select_cell");
        pdc.set(regionKey, PersistentDataType.STRING, cell.getRegion().getId());
        pdc.set(playerKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tool(Material mat, String title, String action, String regionId, OfflinePlayer target) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GOLD));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, action);
        if (regionId != null) pdc.set(regionKey, PersistentDataType.STRING, regionId);
        pdc.set(playerKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack info(Material mat, String title) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.AQUA));
        item.setItemMeta(meta);
        return item;
    }
}



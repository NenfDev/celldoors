package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.models.CellGroupManager;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public final class AdminGroupsGUI {

    public static final String HOLDER_KEY = "groups";

    private final EdenCells plugin;
    private final CellGroupManager groups;
    private final NamespacedKey actionKey;
    private final NamespacedKey groupKey;
    private final NamespacedKey pageKey;

    public AdminGroupsGUI(EdenCells plugin) {
        this.plugin = plugin;
        this.groups = plugin.getCellGroupManager();
        this.actionKey = new NamespacedKey(plugin, "action");
        this.groupKey = new NamespacedKey(plugin, "group");
        this.pageKey = new NamespacedKey(plugin, "page");
    }

    public void open(Player admin, int page) {
        Map<String, CellGroup> all = groups.getAllGroups();
        List<CellGroup> sorted = all.values().stream()
                .sorted(Comparator.comparing(CellGroup::getName))
                .collect(Collectors.toList());

        int perPage = 36; 
        int totalPages = Math.max(1, (int) Math.ceil(sorted.size() / (double) perPage));
        int current = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(new AdminGuiHolder(HOLDER_KEY), 54,
                MessageUtils.fromMiniMessage("<color:#9D4EDD>Cell Groups</color>"));

        
        inv.setItem(46, tool(Material.BARRIER, Component.text("Back", NamedTextColor.RED), "back_dashboard", null, null));

        
        inv.setItem(1, tool(Material.ANVIL, Component.text("Create Group", NamedTextColor.GOLD), "create_group", null, null));
        inv.setItem(2, tool(Material.PAPER, Component.text("Reload Groups", NamedTextColor.AQUA), "reload_groups", null, null));
        inv.setItem(3, tool(Material.BOOK, Component.text("Save Groups", NamedTextColor.GREEN), "save_groups", null, null));

        
        int global = groups.getGlobalCellLimit();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Global limit: " + (global > 0 ? global : "∞"), NamedTextColor.YELLOW));
        lore.add(Component.text("Click to change", NamedTextColor.GRAY));
        inv.setItem(5, tool(Material.COMPARATOR, Component.text("Global Cell Limit", NamedTextColor.GOLD), "edit_global_limit", null, current, lore));

        
        int start = current * perPage;
        int end = Math.min(start + perPage, sorted.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            CellGroup g = sorted.get(i);
            inv.setItem(slot++, groupItem(g, current));
        }

        
        inv.setItem(45, nav("Prev", current > 0, current - 1));
        inv.setItem(49, pageInfo(current + 1, totalPages));
        inv.setItem(53, nav("Next", current < totalPages - 1, current + 1));

        admin.openInventory(inv);
    }

    private ItemStack groupItem(CellGroup group, int page) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(group.getName(), NamedTextColor.GOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Display: " + group.getDisplayName(), NamedTextColor.YELLOW));
        lore.add(Component.text("Regions: " + group.size(), NamedTextColor.YELLOW));
        lore.add(Component.text("Limit: " + (group.getCellLimit() >= 0 ? group.getCellLimit() : "∞"), NamedTextColor.YELLOW));
        double cost = group.getTeleportCost();
        lore.add(Component.text("Teleport: " + (cost >= 0 ? cost : plugin.getConfigManager().getTeleportationDefaultCost()), NamedTextColor.YELLOW));
        lore.add(Component.text("Access: " + group.getTeleportAccess(), NamedTextColor.YELLOW));
        lore.add(Component.text("Perm: " + (group.getRequiredPermission() != null ? group.getRequiredPermission() : "(none)"), NamedTextColor.YELLOW));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, "open_group");
        pdc.set(groupKey, PersistentDataType.STRING, group.getName());
        pdc.set(pageKey, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tool(Material mat, Component name, String action, String group, Integer page) {
        return tool(mat, name, action, group, page, null);
    }

    private ItemStack tool(Material mat, Component name, String action, String group, Integer page, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore != null) meta.lore(new ArrayList<>(lore));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, action);
        if (group != null) pdc.set(groupKey, PersistentDataType.STRING, group);
        if (page != null) pdc.set(pageKey, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack nav(String label, boolean enabled, int targetPage) {
        ItemStack item = new ItemStack(enabled ? Material.ARROW : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, enabled ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, enabled ? "groups_page" : "noop");
        pdc.set(pageKey, PersistentDataType.INTEGER, targetPage);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pageInfo(int current, int total) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Page " + current + "/" + total, NamedTextColor.WHITE));
        item.setItemMeta(meta);
        return item;
    }
}



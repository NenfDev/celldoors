package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
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


public final class AdminGroupEditGUI {

    public static final String HOLDER_KEY = "group_edit";

    private final EdenCells plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey groupKey;

    public AdminGroupEditGUI(EdenCells plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "action");
        this.groupKey = new NamespacedKey(plugin, "group");
    }

    public void open(Player admin, String groupId) {
        CellGroup group = plugin.getCellGroupManager().getGroup(groupId);
        if (group == null) {
            MessageUtils.sendError(admin, "Group not found: " + groupId);
            return;
        }

        Inventory inv = Bukkit.createInventory(new AdminGuiHolder(HOLDER_KEY), 54,
                MessageUtils.fromMiniMessage("<color:#9D4EDD>Edit Group:</color> <color:#FFB3C6>" + group.getName() + "</color>"));

        inv.setItem(10, button(Material.NAME_TAG, "Display Name", "edit_display", groupId,
                List.of(Component.text(group.getDisplayName(), NamedTextColor.YELLOW))));
        inv.setItem(12, button(Material.HOPPER, "Cell Limit", "edit_limit", groupId,
                List.of(Component.text(limitText(group), NamedTextColor.YELLOW))));
        inv.setItem(14, button(Material.ENDER_PEARL, "Teleport Cost", "edit_cost", groupId,
                List.of(Component.text(costText(group), NamedTextColor.YELLOW))));
        inv.setItem(16, button(Material.COMPARATOR, "Teleport Access", "edit_access", groupId,
                List.of(Component.text(group.getTeleportAccess(), NamedTextColor.YELLOW), Component.text("all/owner/permission", NamedTextColor.GRAY))));
        inv.setItem(28, button(Material.TRIPWIRE_HOOK, "Permission", "edit_permission", groupId,
                List.of(Component.text(permText(group), NamedTextColor.YELLOW))));
        inv.setItem(30, button(Material.PAPER, "Add Regions", "edit_regions_add", groupId,
                List.of(Component.text("Count: " + group.size(), NamedTextColor.YELLOW), Component.text("Enter IDs (space/comma)", NamedTextColor.GRAY))));
        inv.setItem(31, button(Material.MAP, "Remove Regions", "edit_regions_remove", groupId,
                List.of(Component.text("Count: " + group.size(), NamedTextColor.YELLOW), Component.text("Enter IDs (space/comma)", NamedTextColor.GRAY))));
        inv.setItem(32, button(Material.TARGET, "Priority", "edit_priority", groupId,
                List.of(Component.text(String.valueOf(group.getPriority()), NamedTextColor.YELLOW))));

        inv.setItem(34, button(Material.BOOK, "Save", "save_groups", groupId, List.of()));

        
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bMeta = back.getItemMeta();
        bMeta.displayName(Component.text("Back", NamedTextColor.RED));
        bMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back_groups");
        bMeta.getPersistentDataContainer().set(groupKey, PersistentDataType.STRING, groupId);
        back.setItemMeta(bMeta);
        inv.setItem(49, back);

        admin.openInventory(inv);
    }

    private String limitText(CellGroup group) {
        int limit = group.getCellLimit();
        return limit >= 0 ? String.valueOf(limit) : "∞ (-1)";
    }

    private String costText(CellGroup group) {
        double cost = group.getTeleportCost();
        return cost >= 0 ? String.valueOf(cost) : "default";
    }

    private String permText(CellGroup group) {
        return group.getRequiredPermission() != null ? group.getRequiredPermission() : "(none)";
    }

    private ItemStack button(Material mat, String title, String action, String group, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.GOLD));
        if (lore != null && !lore.isEmpty()) meta.lore(new ArrayList<>(lore));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(actionKey, PersistentDataType.STRING, action);
        pdc.set(groupKey, PersistentDataType.STRING, group);
        item.setItemMeta(meta);
        return item;
    }
}



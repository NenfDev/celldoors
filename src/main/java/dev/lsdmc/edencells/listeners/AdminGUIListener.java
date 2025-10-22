package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.gui.admin.AdminGuiHolder;
import dev.lsdmc.edencells.gui.admin.AdminPlayersGUI;
import dev.lsdmc.edencells.gui.admin.AdminGroupsGUI;
import dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI;
import dev.lsdmc.edencells.gui.admin.AdminSystemGUI;
import dev.lsdmc.edencells.gui.admin.AdminPlayerCellsGUI;
import dev.lsdmc.edencells.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class AdminGUIListener implements Listener {

    private final EdenCells plugin;

    public AdminGUIListener(EdenCells plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AdminGuiHolder adminHolder)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String page = adminHolder.getPageKey();
        switch (page) {
            case "dashboard" -> handleDashboardClick(player, clicked);
            case "players" -> handlePlayersClick(player, clicked, event.getSlot());
            case AdminGroupsGUI.HOLDER_KEY -> handleGroupsClick(player, clicked);
            case AdminGroupEditGUI.HOLDER_KEY -> handleGroupEditClick(player, clicked);
            case AdminSystemGUI.HOLDER_KEY -> handleSystemClick(player, clicked);
            case AdminPlayerCellsGUI.HOLDER_KEY -> handlePlayerCellsClick(player, clicked);
        }
    }

    private void handleDashboardClick(Player player, ItemStack clicked) {
        Material type = clicked.getType();
        if (type == Material.PLAYER_HEAD) {
            new AdminPlayersGUI(plugin).open(player, 0);
        } else if (type == Material.CHEST) {
            new AdminGroupsGUI(plugin).open(player, 0);
        } else if (type == Material.COMPASS) {
            new AdminSystemGUI(plugin).open(player);
        }
    }

    private void handlePlayersClick(Player player, ItemStack clicked, int slot) {
        
        if (slot == 45) {
            
            new AdminPlayersGUI(plugin).open(player, Math.max(0, getCurrentPage(player) - 1));
        } else if (slot == 53) {
            new AdminPlayersGUI(plugin).open(player, getCurrentPage(player) + 1);
        } else if (slot == 46) {
            new dev.lsdmc.edencells.gui.admin.AdminDashboardGUI(plugin).open(player);
        } else if (slot == 49) {
            
        } else if (clicked.getType() == Material.PLAYER_HEAD) {
            
            var meta = clicked.getItemMeta();
            if (meta != null) {
                var pdc = meta.getPersistentDataContainer();
                String uuidStr = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "admin_player_uuid"), org.bukkit.persistence.PersistentDataType.STRING, "");
                if (!uuidStr.isEmpty()) {
                    try {
                        java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(uuid);
                        if (target != null) {
                            new AdminPlayerCellsGUI(plugin).open(player, target);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    
    private void handleSystemClick(Player player, ItemStack clicked) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        String action = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "sys_action"), org.bukkit.persistence.PersistentDataType.STRING, "");
        if (action.isEmpty()) return;

        switch (action) {
            case "back_dashboard" -> new dev.lsdmc.edencells.gui.admin.AdminDashboardGUI(plugin).open(player);
            case "quick_check" -> plugin.getSyncManager().performQuickCheck().thenAccept(result ->
                    MessageUtils.send(player, "<color:#51CF66>Quick check done. Valid: " + result.getValidRegions() + ", Invalid: " + result.getInvalidRegions() + "</color>")
            );
            case "full_sync" -> plugin.getSyncManager().performFullSync(player);
            case "cleanup_doors" -> {
                int removed = plugin.getDoorManager().cleanupInvalidLinks();
                MessageUtils.send(player, "<color:#FFB3C6>Removed " + removed + " invalid door links.</color>");
            }
            case "reload_all" -> {
                plugin.reload();
                plugin.getCellGroupManager().reloadGroups();
                plugin.getTeleportNPCManager().reloadTeleportNPCs();
                MessageUtils.send(player, "<color:#51CF66>Reload complete.</color>");
            }
            case "save_all" -> {
                plugin.getCellGroupManager().saveGroups();
                plugin.getDoorManager().saveDoors();
                plugin.getTeleportNPCManager().saveNPCs();
                MessageUtils.send(player, "<color:#51CF66>Saved groups, doors, and NPCs.</color>");
            }
            case "npc_reload" -> {
                plugin.getTeleportNPCManager().reloadTeleportNPCs();
                MessageUtils.send(player, "<color:#51CF66>Teleport NPCs reloaded.</color>");
            }
            case "rl_stats" -> MessageUtils.send(player, plugin.getSecurityManager().getRateLimitStats());
        }
    }

    
    private void handlePlayerCellsClick(Player player, ItemStack clicked) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        var keyAction = new org.bukkit.NamespacedKey(plugin, "pc_action");
        var keyRegion = new org.bukkit.NamespacedKey(plugin, "pc_region");
        var keyPlayer = new org.bukkit.NamespacedKey(plugin, "pc_player");
        String action = pdc.getOrDefault(keyAction, org.bukkit.persistence.PersistentDataType.STRING, "");
        String regionId = pdc.getOrDefault(keyRegion, org.bukkit.persistence.PersistentDataType.STRING, "");
        String playerUuid = pdc.getOrDefault(keyPlayer, org.bukkit.persistence.PersistentDataType.STRING, "");
        if (action.isEmpty() || playerUuid.isEmpty()) return;

        java.util.UUID uuid = java.util.UUID.fromString(playerUuid);
        org.bukkit.OfflinePlayer target = plugin.getServer().getOfflinePlayer(uuid);
        if (target == null) return;

        switch (action) {
            case "back_players" -> new dev.lsdmc.edencells.gui.admin.AdminPlayersGUI(plugin).open(player, 0);
            case "select_cell" -> new AdminPlayerCellsGUI(plugin).open(player, target, regionId);
            case "tp_cell" -> {
                if (regionId.isEmpty()) {
                    MessageUtils.sendInfo(player, "Select a cell first.");
                    return;
                }
                var region = plugin.findRegionById(regionId);
                if (region == null) {
                    MessageUtils.sendError(player, "Region not found.");
                    return;
                }
                try {
                    region.teleport(player, false);
                    MessageUtils.sendSuccess(player, "Teleported to cell " + regionId);
                } catch (Exception e) {
                    MessageUtils.sendError(player, "Teleport failed: " + e.getMessage());
                }
            }
            case "set_owner" -> {
                if (regionId.isEmpty()) {
                    
                    AdminPlayerPrompt.beginSetOwnerPrompt(player, target);
                } else {
                    
                    var region = plugin.findRegionById(regionId);
                    if (region == null) { MessageUtils.sendError(player, "Region not found."); return; }
                    try {
                        org.bukkit.OfflinePlayer offline = plugin.getServer().getOfflinePlayer(target.getUniqueId());
                        region.setOwner(offline);
                        region.queueSave();
                        plugin.getDoorManager().syncDoorOwnershipForRegion(regionId);
                        MessageUtils.sendSuccess(player, "Owner set to " + target.getName());
                    } catch (Exception ex) {
                        MessageUtils.sendError(player, "Failed to set owner: " + ex.getMessage());
                    }
                }
            }
            case "add_member" -> {
                if (regionId.isEmpty()) { MessageUtils.sendInfo(player, "Select a cell first."); return; }
                AdminPlayerPrompt.beginMemberAddPrompt(player, target, regionId);
            }
            case "remove_member" -> {
                if (regionId.isEmpty()) { MessageUtils.sendInfo(player, "Select a cell first."); return; }
                AdminPlayerPrompt.beginMemberRemovePrompt(player, target, regionId);
            }
            case "sell_unrent" -> {
                if (regionId.isEmpty()) { MessageUtils.sendInfo(player, "Select a cell first."); return; }
                var region = plugin.findRegionById(regionId);
                if (region != null) {
                    plugin.getCellManager().sellCell(player, region);
                } else {
                    MessageUtils.sendError(player, "Region not found.");
                }
            }
            case "toggle_type" -> MessageUtils.sendInfo(player, "Toggling type requires ARM-side change (not implemented).");
        }
    }

    private void handleGroupsClick(Player player, ItemStack clicked) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "action"), PersistentDataType.STRING, "");
        if (action.isEmpty()) return;

        switch (action) {
            case "back_dashboard" -> new dev.lsdmc.edencells.gui.admin.AdminDashboardGUI(plugin).open(player);
            case "open_group" -> {
                String groupId = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "group"), PersistentDataType.STRING, "");
                if (!groupId.isEmpty()) {
                    new AdminGroupEditGUI(plugin).open(player, groupId);
                }
            }
            case "groups_page" -> {
                int target = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "page"), PersistentDataType.INTEGER, 0);
                new AdminGroupsGUI(plugin).open(player, Math.max(0, target));
            }
            case "create_group" -> {
                AdminGroupPromptListener.beginCreateGroupPrompt(player);
            }
            case "reload_groups" -> {
                plugin.getCellGroupManager().reloadGroups();
                new AdminGroupsGUI(plugin).open(player, 0);
            }
            case "save_groups" -> {
                plugin.getCellGroupManager().saveGroups();
                new AdminGroupsGUI(plugin).open(player, 0);
            }
            case "edit_global_limit" -> {
                int current = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "page"), PersistentDataType.INTEGER, 0);
                AdminGroupPromptListener.beginGlobalLimitPrompt(player, current);
            }
        }
    }

    private void handleGroupEditClick(Player player, ItemStack clicked) {
        var meta = clicked.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String action = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "action"), PersistentDataType.STRING, "");
        String groupId = pdc.getOrDefault(new org.bukkit.NamespacedKey(plugin, "group"), PersistentDataType.STRING, "");
        if (action.isEmpty() || groupId.isEmpty()) {
            if ("back_groups".equals(action)) {
                new AdminGroupsGUI(plugin).open(player, 0);
            }
            return;
        }

        switch (action) {
            case "back_groups" -> new AdminGroupsGUI(plugin).open(player, 0);
            case "save_groups" -> {
                plugin.getCellGroupManager().saveGroups();
                new AdminGroupEditGUI(plugin).open(player, groupId);
            }
            case "edit_limit" -> AdminGroupPromptListener.beginGroupLimitPrompt(player, groupId);
            case "edit_cost" -> AdminGroupPromptListener.beginGroupCostPrompt(player, groupId);
            case "edit_access" -> AdminGroupPromptListener.beginGroupAccessPrompt(player, groupId);
            case "edit_permission" -> AdminGroupPromptListener.beginGroupPermissionPrompt(player, groupId);
            case "edit_regions_add" -> AdminGroupPromptListener.beginRegionAddPrompt(player, groupId);
            case "edit_regions_remove" -> AdminGroupPromptListener.beginRegionRemovePrompt(player, groupId);
            case "edit_priority" -> AdminGroupPromptListener.beginGroupPriorityPrompt(player, groupId);
            case "edit_display" -> AdminGroupPromptListener.beginGroupDisplayPrompt(player, groupId);
        }
    }

    private int getCurrentPage(Player player) {
        
        try {
            ItemStack center = player.getOpenInventory().getTopInventory().getItem(49);
            if (center != null && center.hasItemMeta() && center.getItemMeta().hasDisplayName()) {
                String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(center.getItemMeta().displayName());
                if (name.toLowerCase().startsWith("page ")) {
                    String[] parts = name.split(" ");
                    String[] nums = parts[1].split("/");
                    return Math.max(0, Integer.parseInt(nums[0]) - 1);
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }
}



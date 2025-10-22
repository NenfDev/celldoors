package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.gui.CellGUI;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import dev.lsdmc.edencells.utils.PermissionManager;
import net.alex9849.arm.regions.Region;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;


public final class GUIListener implements Listener {
    
    private final EdenCells plugin;
    private final CellManager cellManager;
    private final SecurityManager security;
    
    public GUIListener(EdenCells plugin, CellManager cellManager, SecurityManager security) {
        this.plugin = plugin;
        this.cellManager = cellManager;
        this.security = security;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        var session = CellGUI.getSession(player);
        if (session == null) {
            return;
        }
        
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        
        if (plugin.getConfigManager().playGuiClickSounds()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
        
        
        int slot = event.getSlot();
        String sessionType = session.type();
        
        switch (sessionType) {
            case "purchase" -> handlePurchaseClick(player, slot, session);
            case "management" -> handleManagementClick(player, slot, session);
            case "viewer" -> handleViewerClick(player, slot, session);
            case "selection" -> handleCellSelectionClick(player, slot, session);
            case "period_selection" -> handlePeriodSelectionClick(player, slot, session);
            case "members" -> handleMembersClick(player, slot, session);
            
            case "vacant" -> handlePurchaseClick(player, slot, session);
            case "occupied" -> handleManagementClick(player, slot, session);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            CellGUI.closeSession(player);
        }
    }
    
    private void handleCellSelectionClick(Player player, int slot, CellGUI.GUISession session) {
        
        if (session.data() instanceof java.util.List<?> list && !list.isEmpty()) {
            @SuppressWarnings("unchecked")
            var cells = (java.util.List<Region>) list;
            
            
            if (session.inventory().getItem(slot) != null && 
                session.inventory().getItem(slot).getType() == Material.BARRIER) {
                player.closeInventory();
                return;
        }
        
            
            if (slot < cells.size()) {
                Region selectedCell = cells.get(slot);
            player.closeInventory();
                
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openCellGUI(player, selectedCell);
            }
        }
    }
    
    private void handlePurchaseClick(Player player, int slot, CellGUI.GUISession session) {
        if (!(session.data() instanceof Region cell)) return;
        
        if (slot == 22) { 
            
            if (!PermissionManager.checkPermission(player, Constants.Permissions.CELL_PURCHASE)) {
                return;
            }
            
            double price = cellManager.getPrice(cell);
            if (!plugin.getEconomy().has(player, price)) {
                MessageUtils.sendError(player, "You cannot afford this cell!");
                return;
            }
            
            
            player.closeInventory();
            boolean success = cellManager.purchaseCell(player, cell);
            
            if (success) {
                
                player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 1.0f, 1.0f);
            }
            
        } else if (slot == 40) { 
            player.closeInventory();
        }
    }
    
    private void handleManagementClick(Player player, int slot, CellGUI.GUISession session) {
        if (!(session.data() instanceof Region cell)) return;
        
        switch (slot) {
            case 15 -> { 
                
                if (!PermissionManager.checkPermission(player, Constants.Permissions.CELL_GUI)) {
                    return;
                }
                player.closeInventory();
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openMembersGUI(player, cell);
            }
            
            case 20 -> { 
                if (cell instanceof net.alex9849.arm.regions.RentRegion) {
                    player.closeInventory();
                    
                    var cellGUI = new CellGUI(plugin, cellManager, security);
                    cellGUI.openPeriodSelectionGUI(player, cell);
                }
            }
            
            case 24 -> { 
                if (!PermissionManager.checkPermission(player, Constants.Permissions.CELL_SELL)) {
                    return;
                }
                player.closeInventory();
                
                boolean success = cellManager.sellCell(player, cell);
                if (success) {
                    player.playSound(player.getLocation(), "minecraft:block.note_block.chime", 1.0f, 1.0f);
                }
            }
            
            case 49 -> player.closeInventory(); 
        }
    }
    
    private void handleViewerClick(Player player, int slot, CellGUI.GUISession session) {
        if (slot == 31) { 
            player.closeInventory();
        }
        
    }
    
    private void handlePeriodSelectionClick(Player player, int slot, CellGUI.GUISession session) {
        if (!(session.data() instanceof Region cell)) return;
        
        
        switch (slot) {
            case 27 -> { 
                player.closeInventory();
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openCellGUI(player, cell);
                return;
            }
            case 31 -> { 
                player.closeInventory();
                return;
            }
        }
        
        
        ItemStack clickedItem = session.inventory().getItem(slot);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        
        int periods = extractPeriodsFromItem(clickedItem);
        
        if (periods > 0) {
            player.closeInventory();
            boolean success = cellManager.extendRental(player, cell, periods);
            if (success) {
                player.playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f);
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openCellGUI(player, cell);
            } else {
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openPeriodSelectionGUI(player, cell);
            }
        }
    }
    
    private void handleMembersClick(Player player, int slot, CellGUI.GUISession session) {
        if (!(session.data() instanceof Region cell)) return;
        
        
        switch (slot) {
            case 45 -> { 
                player.closeInventory();
                
                MessageUtils.sendInfo(player, "To add a member, use: /cell addmember <playername>");
                MessageUtils.sendInfo(player, "Cost: " + plugin.formatCurrency(plugin.getMemberAddCost()));
                return;
            }
            case 46 -> { 
                player.closeInventory();
                
                var cellGUI = new CellGUI(plugin, cellManager, security);
                cellGUI.openCellGUI(player, cell);
                return;
            }
            case 53 -> { 
                player.closeInventory();
                return;
            }
        }
        
        
        if (slot >= 9 && slot < 45) {
            ItemStack clickedItem = session.inventory().getItem(slot);
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                SkullMeta skullMeta = (SkullMeta) clickedItem.getItemMeta();
                if (skullMeta != null && skullMeta.getOwningPlayer() != null) {
                    String memberName = skullMeta.getOwningPlayer().getName();
                    if (memberName != null) {
                        player.closeInventory();
                        
                        MessageUtils.sendInfo(player, "To remove " + memberName + ", use: /cell removemember " + memberName);
                        MessageUtils.sendInfo(player, "Cost: " + plugin.formatCurrency(plugin.getMemberRemoveCost()));
                    }
                }
            }
        }
    }
    
    
    private int extractPeriodsFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return 0;
        }
        
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());
        
        
        if (displayName.toLowerCase().contains("day") || displayName.toLowerCase().contains("period")) {
            try {
                
                String[] parts = displayName.split(" ");
                if (parts.length > 0) {
                    return Integer.parseInt(parts[0]);
                }
            } catch (NumberFormatException e) {
                
                if (displayName.startsWith("1 Day") || displayName.startsWith("1 Period")) {
                    return 1;
                }
            }
        }
        
        return 0;
    }
} 
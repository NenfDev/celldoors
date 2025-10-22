package dev.lsdmc.edencells.gui;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.regions.Region;
import org.bukkit.entity.Player;


public final class CellGUIManager {
    
    private final EdenCells plugin;
    private final CellGUI cellGUI;
    
    public CellGUIManager(EdenCells plugin) {
        this.plugin = plugin;
        this.cellGUI = new CellGUI(plugin, plugin.getCellManager(), plugin.getSecurityManager());
    }
    
    
    public void openMainCellGUI(Player player) {
        try {
            var cells = plugin.getCellManager().getPlayerCells(player);
            
            if (cells.isEmpty()) {
                MessageUtils.sendInfo(player, "You don't own any cells yet!");
                MessageUtils.sendInfo(player, "Look for cell signs to purchase or rent cells.");
                return;
            }
            
            if (cells.size() == 1) {
                
                cellGUI.openCellGUI(player, cells.get(0));
            } else {
                
                showCellList(player, cells);
            }
            
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open cell interface!");
            plugin.getLogger().warning("Error opening main cell GUI for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    
    public void openCellManagementGUI(Player player, Region region) {
        try {
            cellGUI.openCellGUI(player, region);
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open cell management interface!");
            plugin.getLogger().warning("Error opening cell management GUI: " + e.getMessage());
        }
    }
    
    
    public void openPeriodSelectionGUI(Player player, Region region) {
        try {
            cellGUI.openPeriodSelectionGUI(player, region);
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open rental extension interface!");
            plugin.getLogger().warning("Error opening period selection GUI: " + e.getMessage());
        }
    }
    
    
    public void openMembersGUI(Player player, Region region) {
        try {
            cellGUI.openMembersGUI(player, region);
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open members interface!");
            plugin.getLogger().warning("Error opening members GUI: " + e.getMessage());
        }
    }
    
    
    public void openCellInfoGUI(Player player, Region region) {
        try {
            cellGUI.openCellGUI(player, region);
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open cell information interface!");
            plugin.getLogger().warning("Error opening cell info GUI: " + e.getMessage());
        }
    }
    
    
    public void openCellPurchaseGUI(Player player, Region region) {
        try {
            cellGUI.openCellGUI(player, region);
        } catch (Exception e) {
            MessageUtils.sendError(player, "Failed to open cell purchase interface!");
            plugin.getLogger().warning("Error opening cell purchase GUI: " + e.getMessage());
        }
    }
    
    
    private void showCellList(Player player, java.util.List<Region> cells) {
        MessageUtils.sendInfo(player, "=== Your Cells ===");
        
        for (int i = 0; i < cells.size(); i++) {
            Region cell = cells.get(i);
            var info = plugin.getCellManager().getCellInfo(cell);
            
            MessageUtils.sendInfo(player, "%d. %s - %s (%s)", 
                i + 1, info.get("id"), info.get("world"), info.get("type"));
        }
        
        MessageUtils.sendInfo(player, "Right-click cell signs to manage individual cells.");
        MessageUtils.sendInfo(player, "Use /cell info <cellId> to view specific cell details.");
    }
    
    
    public void openVacantCellGUI(Player player, Region region) {
        openCellPurchaseGUI(player, region);
    }
    
    
    public void openOccupiedCellGUI(Player player, Region region) {
        if (plugin.getCellManager().isOwner(player, region)) {
            openCellManagementGUI(player, region);
        } else {
            openCellInfoGUI(player, region);
        }
    }
}



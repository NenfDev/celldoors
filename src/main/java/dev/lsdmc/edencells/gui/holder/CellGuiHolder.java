package dev.lsdmc.edencells.gui.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;


public class CellGuiHolder implements InventoryHolder {
    
    private final String pageKey;
    private Inventory inventory;
    
    public CellGuiHolder(String pageKey) {
        this.pageKey = pageKey;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
    
    public String getPageKey() {
        return pageKey;
    }
}



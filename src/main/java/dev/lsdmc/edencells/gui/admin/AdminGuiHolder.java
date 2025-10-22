package dev.lsdmc.edencells.gui.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminGuiHolder implements InventoryHolder {

    private final String pageKey;
    private Inventory inventory;

    public AdminGuiHolder(String pageKey) {
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



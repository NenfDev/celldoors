package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.gui.CellGUI;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;


public final class CellSignListener implements Listener {
    
    private final EdenCells plugin;
    private final List<String> cellKeywords;
    
    public CellSignListener(EdenCells plugin) {
        this.plugin = plugin;
        this.cellKeywords = plugin.getConfigManager().getCellSignKeywords();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        
        if (!isCellSign(sign)) {
            return;
        }
        
        
        AdvancedRegionMarket arm = AdvancedRegionMarket.getInstance();
        if (arm == null) {
            return;
        }
        
        Region region = arm.getRegionManager().getRegion(sign);
        if (region == null) {
            return;
        }
        
        
        event.setCancelled(true);
        
        plugin.debug("Intercepted ARM sign click: Player=" + player.getName() + 
                    ", Region=" + region.getRegion().getId());
        
        
        openCellGUI(player, region);
    }
    
    
    private boolean isCellSign(Sign sign) {
        for (Component line : sign.lines()) {
            String lineText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(line).toLowerCase();
            
            for (String keyword : cellKeywords) {
                if (lineText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    private void openCellGUI(Player player, Region region) {
        try {
            var cellGUI = new CellGUI(plugin, plugin.getCellManager(), plugin.getSecurityManager());
            cellGUI.openCellGUI(player, region);
            
            
            player.playSound(player.getLocation(), "minecraft:ui.button.click", 1.0f, 1.0f);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error opening cell GUI for player " + player.getName() + 
                                     " and region " + region.getRegion().getId() + ": " + e.getMessage());
            
            
            MessageUtils.sendError(player, "Unable to open cell interface. Please try again.");
        }
    }
}



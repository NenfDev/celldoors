package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.DoorManager;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public final class DoorInteractionListener implements Listener {
    
    private final EdenCells plugin;
    private final DoorManager doorManager;
    private final SecurityManager security;
    
    public DoorInteractionListener(EdenCells plugin, DoorManager doorManager, SecurityManager security) {
        this.plugin = plugin;
        this.doorManager = doorManager;
        this.security = security;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onDoorInteract(PlayerInteractEvent event) {
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        
        
        if (!doorManager.isValidDoor(block.getType())) {
            return;
        }
        
        var player = event.getPlayer();
        var location = block.getLocation();
        
        
        String linkedRegion = doorManager.getLinkedRegion(location);
        if (linkedRegion == null) {
            return; 
        }
        
        
        if (security.isRateLimited(player, "door_interact")) {
            MessageUtils.sendError(player, "You're interacting with doors too quickly!");
            event.setCancelled(true);
            return;
        }
        
        
        if (!doorManager.canAccessDoor(player, location)) {
            event.setCancelled(true);
            
            
            MessageUtils.sendError(player, "You don't have access to this cell!");
            
            
            if (plugin.getConfigManager().areDoorSoundsEnabled()) {
                player.playSound(location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            }
            
            
            security.auditLog(player, "DOOR_ACCESS_DENIED", linkedRegion, 
                "Attempted to access linked door");
            
            return;
        }
        
        
        event.setCancelled(true);
        
        
        if (!doorManager.toggleDoorForPlayer(block, player)) {
            
            return;
        }
        
        
        security.auditLog(player, "DOOR_ACCESS", linkedRegion, 
            "Accessed linked door");
            
        plugin.debug("Player " + player.getName() + " accessed door linked to region " + linkedRegion);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onDoorBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        
        if (!doorManager.isValidDoor(block.getType())) {
            return;
        }
        
        var player = event.getPlayer();
        
        
        String linkedRegion = doorManager.getLinkedRegion(block.getLocation());
        if (linkedRegion == null) {
            return; 
        }
        
        
        if (!(player.hasPermission(Constants.Permissions.ADMIN_DOORS) ||
              player.hasPermission(Constants.Permissions.DOOR_LINK))) {
            event.setCancelled(true);
            MessageUtils.sendError(player, "You cannot break doors linked to cells!");
            MessageUtils.sendInfo(player, "Contact an admin to unlink this door first.");
            return;
        }
        
        
        doorManager.unlinkDoor(block.getLocation());
        MessageUtils.sendInfo(player, "Door unlinked from region: %s", linkedRegion);
        
        
        security.auditLog(player, "DOOR_BREAK", linkedRegion, 
            "Broke and unlinked door");
    }
} 
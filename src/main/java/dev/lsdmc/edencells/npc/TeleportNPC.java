package dev.lsdmc.edencells.npc;

import dev.lsdmc.edencells.EdenCells;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;


@TraitName("edencells-teleport")
public class TeleportNPC extends Trait {

    private EdenCells plugin;

    public TeleportNPC() {
        super("edencells-teleport");
    }

    @Override
    public void onAttach() {
        plugin = JavaPlugin.getPlugin(EdenCells.class);
    }

    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(NPCRightClickEvent event) {
        if (!event.getNPC().equals(this.getNPC())) {
            return;
        }

        var player = event.getClicker();
        var npcId = event.getNPC().getId();

        
        if (plugin != null && plugin.getTeleportNPCManager() != null) {
            plugin.getTeleportNPCManager().handleTeleport(player, npcId);
        }
    }

    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        
        if (!isOurNPC(entity)) {
            return;
        }

        
        Integer npcId = getNPCIdFromEntity(entity);
        if (npcId == null) {
            return;
        }

        
        if (plugin != null && plugin.getTeleportNPCManager() != null) {
            plugin.getTeleportNPCManager().handleTeleport(player, npcId);
        }
    }

    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        
        if (!isOurNPC(entity)) {
            return;
        }

        
        Integer npcId = getNPCIdFromEntity(entity);
        if (npcId == null) {
            return;
        }

        
        if (plugin != null && plugin.getTeleportNPCManager() != null) {
            plugin.getTeleportNPCManager().handleTeleport(player, npcId);
        }
    }

    
    private boolean isOurNPC(Entity entity) {
        
        
        if (plugin == null || plugin.getTeleportNPCManager() == null) {
            return false;
        }

        
        
        return entity.hasMetadata("edencells-teleport-npc");
    }

    
    private Integer getNPCIdFromEntity(Entity entity) {
        
        
        
        
        if (plugin == null || plugin.getTeleportNPCManager() == null) {
            return null;
        }

        
        
        return null;
    }
}
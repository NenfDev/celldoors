package dev.lsdmc.edencells.managers;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import dev.lsdmc.edencells.utils.PermissionManager;
import net.alex9849.arm.regions.Region;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class TeleportNPCManager {

    private final EdenCells plugin;
    private final CellManager cellManager;
    private final Economy economy;
    private final SecurityManager security;

    
    private final Map<Integer, TeleportNpcData> npcConfigs = new HashMap<>();
    
    
    private final ConcurrentHashMap<UUID, Long> lastUse = new ConcurrentHashMap<>();

    
    private File npcsFile;
    private FileConfiguration npcsConfig;

    public record TeleportNpcData(
            String groupId,
            String displayName,
            String worldName
    ) {}

    public TeleportNPCManager(EdenCells plugin, CellManager cellManager, Economy economy, SecurityManager security) {
        this.plugin = plugin;
        this.cellManager = cellManager;
        this.economy = economy;
        this.security = security;

        setupNPCStorage();
        loadNPCs();
    }

    
    private void setupNPCStorage() {
        npcsFile = new File(plugin.getDataFolder(), "teleport-npcs.yml");
        if (!npcsFile.exists()) {
            try {
                npcsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create teleport-npcs.yml: " + e.getMessage());
            }
        }
        npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);
    }

    
    public void loadNPCs() {
        npcConfigs.clear();

        if (!npcsConfig.contains("npcs")) {
            return;
        }

        for (String npcIdStr : npcsConfig.getConfigurationSection("npcs").getKeys(false)) {
            try {
                int npcId = Integer.parseInt(npcIdStr);
                String path = "npcs." + npcIdStr;

                String groupId = npcsConfig.getString(path + ".group-id", "");
                String displayName = npcsConfig.getString(path + ".display-name", "Unknown");
                String worldName = npcsConfig.getString(path + ".world", "world");

                if (!groupId.isEmpty()) {
                    TeleportNpcData data = new TeleportNpcData(groupId, displayName, worldName);
                    npcConfigs.put(npcId, data);

                    
                    if (CitizensAPI.hasImplementation()) {
                        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                        if (npc != null && !npc.hasTrait(dev.lsdmc.edencells.npc.TeleportNPC.class)) {
                            npc.addTrait(dev.lsdmc.edencells.npc.TeleportNPC.class);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid NPC ID in config: " + npcIdStr);
            }
        }

        plugin.getLogger().info("Loaded " + npcConfigs.size() + " teleport NPCs");
    }

    
    public void saveNPCs() {
        
        npcsConfig.set("npcs", null);

        
        for (Map.Entry<Integer, TeleportNpcData> entry : npcConfigs.entrySet()) {
            int npcId = entry.getKey();
            TeleportNpcData data = entry.getValue();
            String path = "npcs." + npcId;

            npcsConfig.set(path + ".group-id", data.groupId());
            npcsConfig.set(path + ".display-name", data.displayName());
            npcsConfig.set(path + ".world", data.worldName());
        }

        try {
            npcsConfig.save(npcsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save teleport-npcs.yml: " + e.getMessage());
        }
    }

    
    public NPC createTeleportNPC(String name, Location location, String groupId) {
        if (!CitizensAPI.hasImplementation()) {
            return null;
        }

        
        CellGroup cellGroup = plugin.getCellGroupManager().getGroup(groupId);
        if (cellGroup == null) {
            plugin.getLogger().warning("Cannot create NPC for non-existent cell group: " + groupId);
            return null;
        }

        
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, name);
        npc.spawn(location);

        
        npc.addTrait(dev.lsdmc.edencells.npc.TeleportNPC.class);

        
        TeleportNpcData data = new TeleportNpcData(
                groupId,
                name,
                location.getWorld().getName()
        );

        npcConfigs.put(npc.getId(), data);
        saveNPCs(); 

        return npc;
    }

    
    public boolean setExistingNPCAsTeleport(int npcId, String groupId) {
        if (!CitizensAPI.hasImplementation()) {
            return false;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            return false;
        }

        
        CellGroup cellGroup = plugin.getCellGroupManager().getGroup(groupId);
        if (cellGroup == null) {
            return false;
        }

        
        if (!npc.hasTrait(dev.lsdmc.edencells.npc.TeleportNPC.class)) {
            npc.addTrait(dev.lsdmc.edencells.npc.TeleportNPC.class);
        }

        
        TeleportNpcData data = new TeleportNpcData(
                groupId,
                npc.getName(),
                npc.getStoredLocation().getWorld().getName()
        );

        npcConfigs.put(npcId, data);
        saveNPCs(); 

        return true;
    }

    
    public boolean removeTeleportNPC(int npcId) {
        if (!CitizensAPI.hasImplementation()) {
            return false;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc != null) {
            npc.destroy();
            npcConfigs.remove(npcId);
            saveNPCs(); 
            return true;
        }

        return false;
    }

    
    public boolean unsetTeleportNPC(int npcId) {
        if (!CitizensAPI.hasImplementation()) {
            return false;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc != null && npc.hasTrait(dev.lsdmc.edencells.npc.TeleportNPC.class)) {
            npc.removeTrait(dev.lsdmc.edencells.npc.TeleportNPC.class);
            npcConfigs.remove(npcId);
            saveNPCs(); 
            return true;
        }

        return false;
    }

    
    public void handleTeleport(Player player, int npcId) {
        
        plugin.debug("=== TELEPORT DEBUG START ===");
        plugin.debug("Plugin instance: " + (plugin != null ? "NOT NULL" : "NULL"));
        plugin.debug("ConfigManager: " + (plugin.getConfigManager() != null ? "NOT NULL" : "NULL"));
        plugin.debug("Teleportation enabled: " + plugin.getConfigManager().isTeleportationEnabled());
        
        
        plugin.getConfigManager().debugConfig();
        
        
        try {
            String testMessage = plugin.getConfigManager().getTeleportationMessage("no_permission");
            plugin.debug("Direct message test: " + testMessage);
        } catch (Exception e) {
            plugin.debug("Exception getting message: " + e.getMessage());
            e.printStackTrace();
        }
        
        
        try {
            String rawMessage = plugin.getConfig().getString("teleportation.messages.no_permission", "RAW_CONFIG_MISSING");
            plugin.debug("Raw config test: " + rawMessage);
        } catch (Exception e) {
            plugin.debug("Exception getting raw config: " + e.getMessage());
            e.printStackTrace();
        }
        
        plugin.debug("=== TELEPORT DEBUG END ===");
        
        
        if (!plugin.getConfigManager().isTeleportationEnabled()) {
            String message = plugin.getConfigManager().getTeleportationMessage("disabled");
            plugin.debug("Sending disabled message: " + message);
            MessageUtils.send(player, message);
            playSound(player, plugin.getConfigManager().getTeleportationSoundError());
            return;
        }

        
        TeleportNpcData data = npcConfigs.get(npcId);
        if (data == null) {
            
            return;
        }

        String groupId = data.groupId();

        
        boolean hasNpcAccess = PermissionManager.hasNpcAccess(player, groupId);
        if (!hasNpcAccess) {
            
            CellGroup primary = plugin.getCellGroupManager().getGroup(groupId);
            if (primary != null) {
                java.util.Set<String> primaryRegions = primary.getRegions();
                for (var other : plugin.getCellGroupManager().getAllGroups().values()) {
                    if (other == null || other.getName().equalsIgnoreCase(groupId)) continue;
                    boolean overlaps = other.getRegions().stream().anyMatch(primaryRegions::contains);
                    if (!overlaps) continue;
                    if (PermissionManager.hasGroupAccess(player, other)) {
                        hasNpcAccess = true;
                        break;
                    }
                }
            }
        }
        if (!hasNpcAccess) {
            String message = plugin.getConfigManager().getTeleportationMessage("no_permission");
            plugin.debug("Sending no_permission message: " + message);
            MessageUtils.send(player, message);
            playSound(player, plugin.getConfigManager().getTeleportationSoundDenied());
            return;
        }

        
        if (!PermissionManager.hasBypassCooldown(player)) {
            long remainingCooldown = getRemainingCooldown(player);
            if (remainingCooldown > 0) {
                String message = plugin.getConfigManager().getTeleportationMessage("cooldown")
                        .replace("%seconds%", String.valueOf(remainingCooldown));
                plugin.debug("Sending cooldown message: " + message);
                MessageUtils.send(player, message);
                playSound(player, plugin.getConfigManager().getTeleportationSoundDenied());
                return;
            }
        }

        
        CellGroup cellGroup = plugin.getCellGroupManager().getGroup(groupId);
        if (cellGroup == null) {
            plugin.getLogger().warning("Cell group not found: " + groupId);
            return;
        }

        Region targetCell = findPlayerCellInGroup(player, cellGroup);
        if (targetCell == null) {
            
            java.util.Set<String> primaryRegions = cellGroup.getRegions();
            Region fallback = null;
            for (var other : plugin.getCellGroupManager().getAllGroups().values()) {
                if (other == null || other.getName().equalsIgnoreCase(cellGroup.getName())) continue;
                boolean overlaps = other.getRegions().stream().anyMatch(primaryRegions::contains);
                if (!overlaps) continue;
                if (!PermissionManager.hasGroupAccess(player, other)) continue;
                fallback = findPlayerCellInGroup(player, other);
                if (fallback != null) {
                    targetCell = fallback;
                    cellGroup = other; 
                    break;
                }
            }
            if (targetCell == null) {
                String message = plugin.getConfigManager().getTeleportationMessage("not_owner_in_group")
                        .replace("%group%", cellGroup.getDisplayName());
                plugin.debug("Sending not_owner_in_group message: " + message);
                MessageUtils.send(player, message);
                playSound(player, plugin.getConfigManager().getTeleportationSoundDenied());
                return;
            }
        }

        
        double cost = determineTeleportCost(player, cellGroup);
        if (cost > 0) {
            
            java.util.Set<String> primaryRegions = cellGroup.getRegions();
            for (var other : plugin.getCellGroupManager().getAllGroups().values()) {
                if (other == null || other.getName().equalsIgnoreCase(cellGroup.getName())) continue;
                boolean overlaps = other.getRegions().stream().anyMatch(primaryRegions::contains);
                if (!overlaps) continue;
                
                if (PermissionManager.hasGroupAccess(player, other)) {
                    double otherCost = other.getTeleportCost();
                    if (otherCost == 0.0) {
                        cost = 0.0;
                        break;
                    }
                }
            }
        }

        
        if (cost > 0) {
            if (economy == null) {
                String message = plugin.getConfigManager().getTeleportationMessage("economy_missing");
                plugin.debug("Sending economy_missing message: " + message);
                MessageUtils.send(player, message);
                playSound(player, plugin.getConfigManager().getTeleportationSoundError());
                return;
            }

            if (!economy.has(player, cost)) {
                String message = plugin.getConfigManager().getTeleportationMessage("insufficient_funds")
                        .replace("%cost%", plugin.formatCurrency(cost));
                plugin.debug("Sending insufficient_funds message: " + message);
                MessageUtils.send(player, message);
                playSound(player, plugin.getConfigManager().getTeleportationSoundDenied());
                return;
            }

            
            if (!economy.withdrawPlayer(player, cost).transactionSuccess()) {
                String message = plugin.getConfigManager().getTeleportationMessage("insufficient_funds")
                        .replace("%cost%", plugin.formatCurrency(cost));
                plugin.debug("Sending insufficient_funds message (withdraw failed): " + message);
                MessageUtils.send(player, message);
                playSound(player, plugin.getConfigManager().getTeleportationSoundDenied());
                return;
            }

            
            String message = plugin.getConfigManager().getTeleportationMessage("charged")
                    .replace("%cost%", plugin.formatCurrency(cost))
                    .replace("%group%", cellGroup.getDisplayName());
            plugin.debug("Sending charged message: " + message);
            MessageUtils.send(player, message);
        }

        
        try {
            
            targetCell.teleport(player, false);

            
            String message = plugin.getConfigManager().getTeleportationMessage("teleported")
                    .replace("%group%", cellGroup.getDisplayName());
            plugin.debug("Sending teleported message: " + message);
            MessageUtils.send(player, message);
            playSound(player, plugin.getConfigManager().getTeleportationSoundSuccess());

            
            recordCooldown(player);

            plugin.debug("Player " + player.getName() + " teleported to cell " + targetCell.getRegion().getId() +
                    " via NPC " + npcId + " (group: " + groupId + ", cost: " + cost + ")");

        } catch (Exception e) {
            
            if (cost > 0 && economy != null) {
                economy.depositPlayer(player, cost);
                MessageUtils.sendError(player, "Teleport failed! Payment has been refunded.");
            } else {
                MessageUtils.sendError(player, "Teleport failed!");
            }
            playSound(player, plugin.getConfigManager().getTeleportationSoundError());
            plugin.getLogger().warning("Error teleporting " + player.getName() + " to cell " +
                    targetCell.getRegion().getId() + ": " + e.getMessage());
        }
    }

    
    private Region findPlayerCellInGroup(Player player, CellGroup cellGroup) {
        List<Region> playerCells = cellManager.getPlayerCells(player);

        for (Region cell : playerCells) {
            if (cellGroup.containsRegion(cell.getRegion().getId())) {
                return cell;
            }
        }

        return null;
    }

    
    private double determineTeleportCost(Player player, CellGroup cellGroup) {
        
        List<String> freeGroups = plugin.getConfigManager().getTeleportationFreeGroups();
        if (freeGroups.contains(cellGroup.getName())) {
            return 0.0;
        }

        
        if (PermissionManager.hasBypassPayment(player)) {
            return 0.0;
        }

        
        double groupCost = cellGroup.getTeleportCost();
        if (groupCost >= 0) {
            return groupCost;
        }

        
        return plugin.getConfigManager().getTeleportationDefaultCost();
    }

    
    public long getRemainingCooldown(Player player) {
        Long lastUseTime = lastUse.get(player.getUniqueId());
        if (lastUseTime == null) {
            return 0;
        }

        long cooldownMs = plugin.getConfigManager().getTeleportationCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - lastUseTime;
        long remaining = cooldownMs - elapsed;

        return remaining > 0 ? remaining / 1000 : 0;
    }

    
    private void recordCooldown(Player player) {
        lastUse.put(player.getUniqueId(), System.currentTimeMillis());
    }

    
    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            
            try {
                player.getWorld().playSound(player.getLocation(), soundName, 1.0f, 1.0f);
            } catch (Exception ex) {
                plugin.debug("Could not play sound: " + soundName);
            }
        }
    }

    
    public TeleportNpcData getNPCConfig(int npcId) {
        return npcConfigs.get(npcId);
    }

    
    public double getGroupCost(String groupId) {
        CellGroup group = plugin.getCellGroupManager().getGroup(groupId);
        if (group != null && group.getTeleportCost() >= 0) {
            return group.getTeleportCost();
        }
        return plugin.getConfigManager().getTeleportationDefaultCost();
    }

    
    public String getGroupPermission(String groupId) {
        CellGroup group = plugin.getCellGroupManager().getGroup(groupId);
        return group != null ? group.getRequiredPermission() : null;
    }

    
    public void reloadTeleportNPCs() {
        loadNPCs();
    }

    
    public void enable() {
        
        lastUse.clear();
    }

    
    public void disable() {
        
        lastUse.clear();
        
        saveNPCs();
    }
}
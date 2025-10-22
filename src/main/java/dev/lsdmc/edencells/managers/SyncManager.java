package dev.lsdmc.edencells.managers;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.models.CellGroupManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public final class SyncManager {
    
    private final EdenCells plugin;
    private final AdvancedRegionMarket arm;
    private final CellGroupManager cellGroupManager;
    private final DoorManager doorManager;
    
    
    private final Map<String, Object> lastSyncStats = new ConcurrentHashMap<>();
    private long lastSyncTime = 0;
    
    
    public static class SyncResult {
        private final int validRegions;
        private final int invalidRegions;
        private final int orphanedDoors;
        private final int fixedOwnership;
        private final int missingGroups;
        private final int syncedDoorOwnerships;
        private final List<String> errors;
        private final long syncTime;
        
        public SyncResult(int validRegions, int invalidRegions, int orphanedDoors, 
                         int fixedOwnership, int missingGroups, int syncedDoorOwnerships, 
                         List<String> errors, long syncTime) {
            this.validRegions = validRegions;
            this.invalidRegions = invalidRegions;
            this.orphanedDoors = orphanedDoors;
            this.fixedOwnership = fixedOwnership;
            this.missingGroups = missingGroups;
            this.syncedDoorOwnerships = syncedDoorOwnerships;
            this.errors = new ArrayList<>(errors);
            this.syncTime = syncTime;
        }
        
        
        public int getValidRegions() { return validRegions; }
        public int getInvalidRegions() { return invalidRegions; }
        public int getOrphanedDoors() { return orphanedDoors; }
        public int getFixedOwnership() { return fixedOwnership; }
        public int getMissingGroups() { return missingGroups; }
        public int getSyncedDoorOwnerships() { return syncedDoorOwnerships; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public long getSyncTime() { return syncTime; }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasIssues() { return invalidRegions > 0 || orphanedDoors > 0 || hasErrors(); }
    }
    
    public SyncManager(EdenCells plugin) {
        this.plugin = plugin;
        this.arm = plugin.getARM();
        this.cellGroupManager = plugin.getCellGroupManager();
        this.doorManager = plugin.getDoorManager();
    }
    
    
    public CompletableFuture<SyncResult> performFullSync(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<String> errors = new ArrayList<>();
            
            if (sender != null) {
                MessageUtils.send(sender, "<color:#9D4EDD>Starting comprehensive ARM data synchronization...</color>");
            }
            
            plugin.debug("Starting full ARM sync");
            
            
            int[] regionStats = validateCellGroupRegions(sender, errors);
            int validRegions = regionStats[0];
            int invalidRegions = regionStats[1];
            
            
            int orphanedDoors = cleanupOrphanedDoors(sender, errors);
            
            
            int fixedOwnership = fixOwnershipInconsistencies(sender, errors);
            
            
            int missingGroups = identifyMissingGroups(sender, errors);
            
            
            int syncedDoorOwnerships = syncDoorOwnerships(sender, errors);
            
            long syncTime = System.currentTimeMillis() - startTime;
            
            if (sender != null) {
                MessageUtils.send(sender, "<color:#51CF66>Synchronization completed in " + syncTime + "ms</color>");
            }
            
            plugin.debug("Full ARM sync completed in " + syncTime + "ms");
            
            
            updateSyncStats(validRegions, invalidRegions, orphanedDoors, fixedOwnership, 
                missingGroups, syncedDoorOwnerships, syncTime);
            
            SyncResult result = new SyncResult(validRegions, invalidRegions, orphanedDoors, 
                                              fixedOwnership, missingGroups, syncedDoorOwnerships, 
                                              errors, syncTime);
            
            if (sender != null) {
                reportSyncResults(sender, result);
            }
            
            return result;
        });
    }
    
    
    private int syncDoorOwnerships(CommandSender sender, List<String> errors) {
        if (sender != null) {
            MessageUtils.send(sender, "<color:#FFB3C6>Syncing door ownerships...</color>");
        }
        
        try {
            int syncedDoors = doorManager.syncAllDoorOwnerships();
            
            if (sender != null) {
                MessageUtils.send(sender, "<color:#51CF66>✓ Synced " + syncedDoors + " door ownerships</color>");
            }
            
            plugin.debug("Synced " + syncedDoors + " door ownerships");
            return syncedDoors;
            
        } catch (Exception e) {
            String error = "Failed to sync door ownerships: " + e.getMessage();
            errors.add(error);
            
            if (sender != null) {
                MessageUtils.send(sender, "<color:#FF6B6B>✗ " + error + "</color>");
            }
            
            plugin.getLogger().warning(error);
            return 0;
        }
    }
    
    
    private int[] validateCellGroupRegions(CommandSender sender, List<String> errors) {
        int validRegions = 0;
        int invalidRegions = 0;
        
        if (sender != null) {
            MessageUtils.send(sender, "<color:#06FFA5>Step 1: Validating cell group regions...</color>");
        }
        
        for (CellGroup group : cellGroupManager.getAllGroups().values()) {
            Set<String> regionsToRemove = new HashSet<>();
            
            for (String regionId : group.getRegions()) {
                Region region = plugin.findRegionById(regionId);
                if (region == null) {
                    invalidRegions++;
                    regionsToRemove.add(regionId);
                    errors.add("Invalid region in group '" + group.getName() + "': " + regionId);
                    plugin.debug("Found invalid region: " + regionId + " in group: " + group.getName());
                } else {
                    validRegions++;
                    
                    
                    if (region.getRegion() == null) {
                        errors.add("Region '" + regionId + "' has null WorldGuard region data");
                    }
                }
            }
            
            
            for (String regionId : regionsToRemove) {
                group.removeRegion(regionId);
            }
            
            if (!regionsToRemove.isEmpty()) {
                if (sender != null) {
                    MessageUtils.send(sender, "  <color:#FF6B6B>Removed " + regionsToRemove.size() + 
                                    " invalid regions from group '" + group.getName() + "'</color>");
                }
            }
        }
        
        
        if (invalidRegions > 0) {
            cellGroupManager.saveGroups();
        }
        
        return new int[]{validRegions, invalidRegions};
    }
    
    
    private int cleanupOrphanedDoors(CommandSender sender, List<String> errors) {
        if (sender != null) {
            MessageUtils.send(sender, "<color:#06FFA5>Step 2: Cleaning up orphaned door links...</color>");
        }
        
        int orphanedDoors = 0;
        Map<String, String> doorLinks = doorManager.getAllDoorLinks();
        Set<String> doorsToRemove = new HashSet<>();
        
        for (Map.Entry<String, String> entry : doorLinks.entrySet()) {
            String doorKey = entry.getKey();
            String regionId = entry.getValue();
            
            
            Region region = plugin.findRegionById(regionId);
            if (region == null) {
                orphanedDoors++;
                doorsToRemove.add(doorKey);
                errors.add("Orphaned door link: " + doorKey + " -> " + regionId);
                plugin.debug("Found orphaned door link: " + doorKey + " -> " + regionId);
                continue;
            }
            
            
            if (!isValidDoorLocation(doorKey)) {
                orphanedDoors++;
                doorsToRemove.add(doorKey);
                errors.add("Invalid door location: " + doorKey);
                plugin.debug("Found invalid door location: " + doorKey);
            }
        }
        
        
        for (String doorKey : doorsToRemove) {
            Location doorLoc = parseLocationFromKey(doorKey);
            if (doorLoc != null) {
                doorManager.unlinkDoor(doorLoc);
            }
        }
        
        if (orphanedDoors > 0 && sender != null) {
            MessageUtils.send(sender, "  <color:#FF6B6B>Removed " + orphanedDoors + " orphaned door links</color>");
        }
        
        return orphanedDoors;
    }
    
    
    private int fixOwnershipInconsistencies(CommandSender sender, List<String> errors) {
        if (sender != null) {
            MessageUtils.send(sender, "<color:#06FFA5>Step 3: Checking ownership data consistency...</color>");
        }
        
        int fixedOwnership = 0;
        
        
        for (Region region : arm.getRegionManager()) {
            if (region == null || region.getRegion() == null) {
                continue;
            }
            
            String regionId = region.getRegion().getId();
            
            
            UUID ownerId = region.getOwner();
            List<UUID> members = region.getRegion().getMembers();
            
            
            if (ownerId == null && !members.isEmpty()) {
                errors.add("Region '" + regionId + "' has members but no owner");
                fixedOwnership++;
            }
            
            
            for (UUID memberId : members) {
                if (memberId == null) {
                    errors.add("Region '" + regionId + "' has null member UUID");
                    fixedOwnership++;
                }
            }
            
            
            if (ownerId == null && members.isEmpty()) {
                
                plugin.debug("Region '" + regionId + "' has no owner or members");
            }
        }
        
        if (fixedOwnership > 0 && sender != null) {
            MessageUtils.send(sender, "  <color:#FFB3C6>Found " + fixedOwnership + " ownership inconsistencies</color>");
        }
        
        return fixedOwnership;
    }
    
    
    private int identifyMissingGroups(CommandSender sender, List<String> errors) {
        if (sender != null) {
            MessageUtils.send(sender, "<color:#06FFA5>Step 4: Identifying regions missing from groups...</color>");
        }
        
        int missingGroups = 0;
        Set<String> groupedRegions = new HashSet<>();
        
        
        for (CellGroup group : cellGroupManager.getAllGroups().values()) {
            groupedRegions.addAll(group.getRegions());
        }
        
        
        for (Region region : arm.getRegionManager()) {
            if (region == null || region.getRegion() == null) {
                continue;
            }
            
            String regionId = region.getRegion().getId();
            
            
            if (groupedRegions.contains(regionId)) {
                continue;
            }
            
            
            String suggestedGroup = suggestGroupForRegion(regionId);
            if (suggestedGroup != null) {
                missingGroups++;
                errors.add("Region '" + regionId + "' not in any group (suggested: " + suggestedGroup + ")");
                plugin.debug("Region '" + regionId + "' could belong to group: " + suggestedGroup);
            }
        }
        
        if (missingGroups > 0 && sender != null) {
            MessageUtils.send(sender, "  <color:#FFB3C6>Found " + missingGroups + " regions that could be grouped</color>");
        }
        
        return missingGroups;
    }
    
    
    private String suggestGroupForRegion(String regionId) {
        String lowerRegionId = regionId.toLowerCase();
        
        
        if (lowerRegionId.matches("^[a-j]cell\\d+$")) {
            char wardLetter = lowerRegionId.charAt(0);
            return wardLetter + "cells";
        }
        

        
        
        for (CellGroup group : cellGroupManager.getAllGroups().values()) {
            String groupName = group.getName().toLowerCase();
            if (lowerRegionId.startsWith(groupName.replace("cells", ""))) {
                return group.getName();
            }
        }
        
        return null;
    }
    
    
    private boolean isValidDoorLocation(String doorKey) {
        Location location = parseLocationFromKey(doorKey);
        if (location == null) {
            return false;
        }
        
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        
        return doorManager.isValidDoor(location.getBlock().getType());
    }
    
    
    private Location parseLocationFromKey(String doorKey) {
        try {
            String[] parts = doorKey.split(":");
            if (parts.length != 4) {
                return null;
            }
            
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                return null;
            }
            
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
    
    
    private void updateSyncStats(int validRegions, int invalidRegions, int orphanedDoors, 
                                int fixedOwnership, int missingGroups, int syncedDoorOwnerships, long syncTime) {
        lastSyncTime = System.currentTimeMillis();
        lastSyncStats.put("validRegions", validRegions);
        lastSyncStats.put("invalidRegions", invalidRegions);
        lastSyncStats.put("orphanedDoors", orphanedDoors);
        lastSyncStats.put("fixedOwnership", fixedOwnership);
        lastSyncStats.put("missingGroups", missingGroups);
        lastSyncStats.put("syncedDoorOwnerships", syncedDoorOwnerships);
        lastSyncStats.put("syncTime", syncTime);
    }
    
    
    private void reportSyncResults(CommandSender sender, SyncResult result) {
        MessageUtils.send(sender, "<color:#9D4EDD>=== Synchronization Results ===</color>");
        MessageUtils.send(sender, "");
        
        
        MessageUtils.send(sender, "<color:#51CF66>✓ Valid regions:</color> <color:#FFB3C6>" + result.getValidRegions() + "</color>");
        
        if (result.getInvalidRegions() > 0) {
            MessageUtils.send(sender, "<color:#FF6B6B>✗ Invalid regions removed:</color> <color:#FFB3C6>" + result.getInvalidRegions() + "</color>");
        }
        
        
        if (result.getOrphanedDoors() > 0) {
            MessageUtils.send(sender, "<color:#FF6B6B>✗ Orphaned doors cleaned:</color> <color:#FFB3C6>" + result.getOrphanedDoors() + "</color>");
        } else {
            MessageUtils.send(sender, "<color:#51CF66>✓ No orphaned doors found</color>");
        }
        
        
        if (result.getSyncedDoorOwnerships() > 0) {
            MessageUtils.send(sender, "<color:#51CF66>✓ Door ownerships synced:</color> <color:#FFB3C6>" + result.getSyncedDoorOwnerships() + "</color>");
        } else {
            MessageUtils.send(sender, "<color:#ADB5BD>• No doors to sync</color>");
        }
        
        
        if (result.getFixedOwnership() > 0) {
            MessageUtils.send(sender, "<color:#FF6B6B>✗ Ownership issues fixed:</color> <color:#FFB3C6>" + result.getFixedOwnership() + "</color>");
        } else {
            MessageUtils.send(sender, "<color:#51CF66>✓ No ownership issues found</color>");
        }
        
        
        if (result.getMissingGroups() > 0) {
            MessageUtils.send(sender, "<color:#FF6B6B>! Regions needing groups:</color> <color:#FFB3C6>" + result.getMissingGroups() + "</color>");
        } else {
            MessageUtils.send(sender, "<color:#51CF66>✓ All regions properly grouped</color>");
        }
        
        
        if (result.hasErrors()) {
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, "<color:#FF6B6B>Errors encountered:</color>");
            for (String error : result.getErrors()) {
                MessageUtils.send(sender, "<color:#FF6B6B>• " + error + "</color>");
            }
        }
        
        
        MessageUtils.send(sender, "");
        if (result.hasIssues()) {
            MessageUtils.send(sender, "<color:#FF6B6B>⚠ Synchronization completed with issues</color>");
            MessageUtils.send(sender, "<color:#ADB5BD>Check the results above and consider running sync again</color>");
        } else {
            MessageUtils.send(sender, "<color:#51CF66>✓ Synchronization completed successfully!</color>");
            MessageUtils.send(sender, "<color:#ADB5BD>All systems are properly synchronized</color>");
        }
        
        MessageUtils.send(sender, "<color:#ADB5BD>Completed in " + result.getSyncTime() + "ms</color>");
    }
    
    
    public Map<String, Object> getLastSyncStats() {
        return new HashMap<>(lastSyncStats);
    }
    
    
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    
    public CompletableFuture<SyncResult> performQuickCheck() {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<String> errors = new ArrayList<>();
            
            int validRegions = 0;
            int invalidRegions = 0;
            
            
            for (CellGroup group : cellGroupManager.getAllGroups().values()) {
                for (String regionId : group.getRegions()) {
                    Region region = plugin.findRegionById(regionId);
                    if (region == null) {
                        invalidRegions++;
                        errors.add("Invalid region: " + regionId);
                    } else {
                        validRegions++;
                    }
                }
            }
            
            long syncTime = System.currentTimeMillis() - startTime;
            
            return new SyncResult(validRegions, invalidRegions, 0, 0, 0, 0, errors, syncTime);
        });
    }
} 
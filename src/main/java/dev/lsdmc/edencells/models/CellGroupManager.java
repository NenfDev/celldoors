package dev.lsdmc.edencells.models;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.PermissionRegistry;
import net.alex9849.arm.AdvancedRegionMarket;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;


public final class CellGroupManager {
    
    private final EdenCells plugin;
    private final ConcurrentMap<String, CellGroup> groups = new ConcurrentHashMap<>();
    private int globalCellLimit = -1; 
    
    private File groupsFile;
    private FileConfiguration groupsConfig;
    
    public CellGroupManager(EdenCells plugin) {
        this.plugin = plugin;
        initializeGroupsFile();
        loadGroups();
    }
    
    
    private void initializeGroupsFile() {
        groupsFile = new File(plugin.getDataFolder(), "cell-groups.yml");
        
        
        if (!groupsFile.exists()) {
            plugin.saveResource("cell-groups.yml", false);
        }
        
        groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
        
        
        migrateFromMainConfig();
    }
    
    
    private void migrateFromMainConfig() {
        ConfigurationSection mainConfigGroups = plugin.getConfig().getConfigurationSection("cell-groups");
        
        if (mainConfigGroups != null && !mainConfigGroups.getKeys(false).isEmpty()) {
            plugin.getLogger().info("Migrating cell groups from config.yml to cell-groups.yml...");
            
            
            groupsConfig.set("groups", mainConfigGroups);
            
            
            int globalLimit = plugin.getConfig().getInt("cell-limits.global", -1);
            groupsConfig.set("limits.global", globalLimit);
            
            
            groupsConfig.set("version", 1);
            
            
            saveGroups();
            
            
            plugin.getConfig().set("cell-groups", null);
            plugin.getConfig().set("cell-limits", null);
            plugin.saveConfig();
            
            plugin.getLogger().info("Migration completed! Cell groups moved to cell-groups.yml");
        }
    }
    
      
    public void loadGroups() {
        groups.clear();
        
        try {
            PermissionRegistry.clearAll();
        } catch (Exception ignored) {}
        
        try {
            
            groupsConfig = YamlConfiguration.loadConfiguration(groupsFile);
            
            
            globalCellLimit = groupsConfig.getInt("limits.global", -1);
            
            ConfigurationSection groupsSection = groupsConfig.getConfigurationSection("groups");
            
            if (groupsSection != null) {
                for (String groupName : groupsSection.getKeys(false)) {
                    try {
                        ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                        if (groupSection == null) continue;
                        
                        
                        String displayName = groupSection.getString("display-name", groupName);
                        
                        
                        List<String> regionsList = groupSection.getStringList("regions");
                        Set<String> regions = new LinkedHashSet<>(regionsList);
                        
                        
                        ConcurrentMap<String, Object> options = new ConcurrentHashMap<>();
                        
                        
                        if (groupSection.contains("cell-limit")) {
                            options.put("cellLimit", groupSection.getInt("cell-limit", -1));
                        }
                        
                        
                        if (groupSection.contains("teleport-cost")) {
                            options.put("teleportCost", groupSection.getDouble("teleport-cost", -1));
                        }
                        
                        
                        if (groupSection.contains("priority")) {
                            options.put("priority", groupSection.getInt("priority", 0));
                        }

                        
                        if (groupSection.contains("teleport-access")) {
                            options.put("teleportAccess", groupSection.getString("teleport-access", "all"));
                        }

                        
                        if (groupSection.contains("permission")) {
                            String perm = groupSection.getString("permission");
                            options.put("permission", perm);
                            
                            if (perm != null && !perm.isBlank()) {
                                PermissionRegistry.register(perm, "Access to cell group '" + groupName + "'");
                            }
                        }
                        
                        
                        CellGroup group = new CellGroup(groupName, displayName, regions, options);
                        groups.put(groupName, group);
                        
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load cell group '" + groupName + "': " + e.getMessage());
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + groups.size() + " cell groups from cell-groups.yml");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load cell groups: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public synchronized void saveGroups() {
        try {
            
            groupsConfig.set("limits.global", globalCellLimit);
            
            
            groupsConfig.set("groups", null);
            
            
            for (Map.Entry<String, CellGroup> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                CellGroup group = entry.getValue();
                
                String path = "groups." + groupName;
                groupsConfig.set(path + ".display-name", group.getDisplayName());
                
                
                groupsConfig.set(path + ".regions", group.getRegions().stream().collect(Collectors.toList()));
                
                
                if (group.getCellLimit() != -1) {
                    groupsConfig.set(path + ".cell-limit", group.getCellLimit());
                }
                if (group.getTeleportCost() != -1) {
                    groupsConfig.set(path + ".teleport-cost", group.getTeleportCost());
                }
                if (group.getPriority() != 0) {
                    groupsConfig.set(path + ".priority", group.getPriority());
                }
                if (!"all".equalsIgnoreCase(group.getTeleportAccess())) {
                    groupsConfig.set(path + ".teleport-access", group.getTeleportAccess());
                }

                if (group.getRequiredPermission() != null) {
                    groupsConfig.set(path + ".permission", group.getRequiredPermission());
                }
            }
            
            
            groupsConfig.set("version", 1);
            
            
            groupsConfig.save(groupsFile);
            
            plugin.debug("Saved " + groups.size() + " cell groups to cell-groups.yml");
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save cell groups: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public void reloadGroups() {
        loadGroups();
    }
    
    
    public CellGroup getGroup(String name) {
        return name != null ? groups.get(name.trim()) : null;
    }
    
    
    public CellGroup getGroupByRegion(String regionId) {
        if (regionId == null) return null;
        
        String trimmed = regionId.trim();
        
        CellGroup best = null;
        int bestPriority = Integer.MIN_VALUE;
        for (CellGroup group : groups.values()) {
            if (group.containsRegion(trimmed)) {
                int prio = group.getPriority();
                if (best == null || prio > bestPriority) {
                    best = group;
                    bestPriority = prio;
                }
            }
        }
        return best;
    }

    
    public java.util.List<CellGroup> getGroupsByRegion(String regionId) {
        if (regionId == null) return java.util.List.of();
        String trimmed = regionId.trim();
        java.util.List<CellGroup> result = new java.util.ArrayList<>();
        for (CellGroup group : groups.values()) {
            if (group.containsRegion(trimmed)) {
                result.add(group);
            }
        }
        
        result.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return result;
    }
    
    
    public Map<String, CellGroup> getAllGroups() {
        return new HashMap<>(groups);
    }

    
    public boolean updateGroupDisplayName(String groupId, String newDisplayName) {
        if (groupId == null || newDisplayName == null) return false;
        String id = groupId.trim();
        String display = newDisplayName.trim();
        CellGroup existing = groups.get(id);
        if (existing == null) return false;
        try {
            CellGroup updated = new CellGroup(existing.getName(), display);
            
            for (String regionId : existing.getRegions()) {
                updated.addRegion(regionId);
            }
            
            updated.setCellLimit(existing.getCellLimit());
            updated.setTeleportCost(existing.getTeleportCost());
            updated.setPriority(existing.getPriority());
            updated.setTeleportAccess(existing.getTeleportAccess());
            String perm = existing.getRequiredPermission();
            if (perm != null) {
                updated.setRequiredPermission(perm);
            }
            groups.put(id, updated);
            return true;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to update display name for group '" + id + "': " + e.getMessage());
            return false;
        }
    }
    
    
    public CellGroup createGroup(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        
        String trimmed = name.trim();
        if (groups.containsKey(trimmed)) {
            return null;
        }
        
        try {
            CellGroup group = new CellGroup(trimmed);
            groups.put(trimmed, group);
            return group;
        } catch (IllegalArgumentException e) {
            throw e; 
        }
    }
    
    
    public boolean deleteGroup(String name) {
        if (name == null) return false;
        
        String trimmed = name.trim();
        CellGroup existing = groups.get(trimmed);
        if (existing != null) {
            String perm = existing.getRequiredPermission();
            if (perm != null && !perm.isBlank()) {
                PermissionRegistry.unregister(perm);
            }
        }
        CellGroup removed = groups.remove(trimmed);
        return removed != null;
    }
    
    
    public boolean canPlayerAcquireInGroup(Player player, CellGroup group) {
        if (player == null || group == null) {
            return false;
        }
        
        
        String permission = group.getRequiredPermission();
        if (permission != null && !permission.trim().isEmpty()) {
            
            if (plugin.getConfigManager().forceGroupPermissions()) {
                String prefix = plugin.getConfigManager().getLuckPermsGroupPrefix();
                String required = permission.startsWith(prefix) ? permission : prefix + permission;
                
                CellGroup shadow = new CellGroup(group.getName(), group.getDisplayName(), group.getRegions(), group.getOptions());
                shadow.setRequiredPermission(required);
                if (!dev.lsdmc.edencells.utils.PermissionManager.hasGroupAccess(player, shadow)) {
                    return false;
                }
            } else {
                if (!dev.lsdmc.edencells.utils.PermissionManager.hasGroupAccess(player, group)) {
                    return false;
                }
            }
        }
        
        
        int groupLimit = group.getCellLimit();
        if (groupLimit > 0) {
            int currentCount = getPlayerCellCountInGroup(player.getUniqueId(), group);
            if (currentCount >= groupLimit) {
                return false;
            }
        }
        
        
        if (globalCellLimit > 0) {
            int totalCount = getPlayerTotalCellCount(player.getUniqueId());
            if (totalCount >= globalCellLimit) {
                return false;
            }
        }
        
        return true;
    }
    
    
    public int getPlayerCellCountInGroup(UUID playerUuid, CellGroup group) {
        if (playerUuid == null || group == null) {
            return 0;
        }
        
        AdvancedRegionMarket arm = AdvancedRegionMarket.getInstance();
        if (arm == null) return 0;
        
        return (int) arm.getRegionManager().getRegionsByOwner(playerUuid)
            .stream()
            .filter(region -> group.containsRegion(region.getRegion().getId()))
            .count();
    }
    
    
    public int getPlayerTotalCellCount(UUID playerUuid) {
        if (playerUuid == null) return 0;
        
        AdvancedRegionMarket arm = AdvancedRegionMarket.getInstance();
        if (arm == null) return 0;
        
        return arm.getRegionManager().getRegionsByOwner(playerUuid).size();
    }
    
    
    public List<CellGroup> getAccessibleGroups(Player player) {
        if (player == null) {
            return List.of();
        }
        
        return groups.values().stream()
            .filter(group -> {
                String permission = group.getRequiredPermission();
                return permission == null || player.hasPermission(permission);
            })
            .collect(Collectors.toList());
    }
    
    
    public double getTeleportCostForRegion(String regionId) {
        CellGroup group = getGroupByRegion(regionId);
        if (group != null) {
            return group.getTeleportCost();
        }
        return -1;
    }
    
    
    public void setGlobalCellLimit(int limit) {
        if (limit < -1) {
            throw new IllegalArgumentException("Global cell limit cannot be less than -1");
        }
        this.globalCellLimit = limit;
    }
    
    
    public int getGlobalCellLimit() {
        return globalCellLimit;
    }
    
    
    public Map<String, String> getPlayerLimitInfo(Player player) {
        if (player == null) {
            return Map.of();
        }
        
        Map<String, String> info = new HashMap<>();
        
        
        int totalCells = getPlayerTotalCellCount(player.getUniqueId());
        if (globalCellLimit > 0) {
            info.put("global", totalCells + "/" + globalCellLimit);
        } else {
            info.put("global", totalCells + "/∞");
        }
        
        
        for (CellGroup group : groups.values()) {
            int groupLimit = group.getCellLimit();
            if (groupLimit > 0) {
                int groupCells = getPlayerCellCountInGroup(player.getUniqueId(), group);
                info.put(group.getName(), groupCells + "/" + groupLimit);
            }
        }
        
        return info;
    }
    
    
    public File getGroupsFile() {
        return groupsFile;
    }
    
    
    public FileConfiguration getGroupsConfig() {
        return groupsConfig;
    }
    
    
    public boolean isValidGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        try {
            
            new CellGroup(name.trim());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}



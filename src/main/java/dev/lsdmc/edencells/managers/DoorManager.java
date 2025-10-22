package dev.lsdmc.edencells.managers;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.Constants;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class DoorManager {
    
    private final EdenCells plugin;
    private final CellManager cellManager;
    private final SecurityManager security;
    
    
    private final Map<String, String> doorLinks = new ConcurrentHashMap<>();
    private final Set<Material> validDoorMaterials = new HashSet<>();
    private File doorsFile;
    
    
    private final Map<UUID, Long> doorCooldowns = new ConcurrentHashMap<>();
    private static final long DOOR_COOLDOWN_MS = 500; 
    
    
    private boolean playSounds;
    private Sound openSound;
    private Sound closeSound;
    private float soundVolume;
    private float soundPitch;
    
    public DoorManager(EdenCells plugin, CellManager cellManager, SecurityManager security) {
        this.plugin = plugin;
        this.cellManager = cellManager;
        this.security = security;
        this.doorsFile = new File(plugin.getDataFolder(), "doors.yml");
        
        loadConfig();
        loadDoors();
    }

    
    public void reload() {
        loadConfig();
    }
    
    
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        
        playSounds = plugin.getConfigManager().areDoorSoundsEnabled();
        String openSoundName = plugin.getConfigManager().getDoorOpenSound();
        String closeSoundName = plugin.getConfigManager().getDoorCloseSound();
        soundVolume = plugin.getConfigManager().getDoorSoundVolume();
        soundPitch = plugin.getConfigManager().getDoorSoundPitch();
        
        
        try {
            openSound = Sound.valueOf(openSoundName.toUpperCase().replace(".", "_"));
        } catch (IllegalArgumentException e) {
            openSound = Sound.BLOCK_IRON_DOOR_OPEN;
            plugin.getLogger().warning("Invalid open sound: " + openSoundName + ", using default");
        }
        
        try {
            closeSound = Sound.valueOf(closeSoundName.toUpperCase().replace(".", "_"));
        } catch (IllegalArgumentException e) {
            closeSound = Sound.BLOCK_IRON_DOOR_CLOSE;
            plugin.getLogger().warning("Invalid close sound: " + closeSoundName + ", using default");
        }
        
        
        validDoorMaterials.clear();
        List<String> materials = plugin.getConfigManager().getValidDoorMaterials();
        if (materials.isEmpty()) {
            
            validDoorMaterials.add(Material.IRON_DOOR);
            validDoorMaterials.add(Material.OAK_DOOR);
            validDoorMaterials.add(Material.SPRUCE_DOOR);
            validDoorMaterials.add(Material.BIRCH_DOOR);
            validDoorMaterials.add(Material.JUNGLE_DOOR);
            validDoorMaterials.add(Material.ACACIA_DOOR);
            validDoorMaterials.add(Material.DARK_OAK_DOOR);
            validDoorMaterials.add(Material.CRIMSON_DOOR);
            validDoorMaterials.add(Material.WARPED_DOOR);
            validDoorMaterials.add(Material.IRON_TRAPDOOR);
        } else {
            for (String mat : materials) {
                try {
                    validDoorMaterials.add(Material.valueOf(mat.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid door material: " + mat);
                }
            }
        }
    }
    
    
    public void linkDoor(Location location, String regionId) {
        String key = locationToKey(location);
        doorLinks.put(key, regionId);
        saveDoors();
        plugin.debug("Linked door at " + key + " to region " + regionId);
    }
    
    
    public void unlinkDoor(Location location) {
        String key = locationToKey(location);
        String regionId = doorLinks.remove(key);
        if (regionId != null) {
            saveDoors();
            plugin.debug("Unlinked door at " + key + " from region " + regionId);
        }
    }
    
    
    public String getLinkedRegion(Location location) {
        
        Block bottom = getBottomDoorBlock(location.getBlock());
        if (bottom != null) {
            String key = locationToKey(bottom.getLocation());
            return doorLinks.get(key);
        }
        return null;
    }
    
    
    public boolean isDoorLinked(Location location) {
        return getLinkedRegion(location) != null;
    }
    
    
    public boolean canAccessDoor(Player player, Location location) {
        String regionId = getLinkedRegion(location);
        if (regionId == null) {
            return true; 
        }
        
        
        Region region = plugin.findRegionById(regionId);
        if (region == null) {
            
            plugin.debug("Door linked to non-existent region: " + regionId);
            return false;
        }
        
        
        return cellManager.isOwner(player, region) || cellManager.hasAccess(player, region);
    }
    
    
    public boolean isOnDoorCooldown(Player player) {
        Long lastInteraction = doorCooldowns.get(player.getUniqueId());
        if (lastInteraction == null) {
            return false;
        }
        
        long timeSince = System.currentTimeMillis() - lastInteraction;
        if (timeSince < DOOR_COOLDOWN_MS) {
            return true;
        }
        
        
        doorCooldowns.remove(player.getUniqueId());
        return false;
    }
    
    
    private void setDoorCooldown(Player player) {
        doorCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    
    public void toggleDoor(Block block) {
        if (!isValidDoor(block.getType())) {
            return;
        }
        
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Openable openable)) {
            return;
        }
        
        
        boolean wasOpen = openable.isOpen();
        openable.setOpen(!wasOpen);
        block.setBlockData(openable);
        
        
        Block otherHalf = findDoubleDoor(block);
        if (otherHalf != null) {
            BlockData otherData = otherHalf.getBlockData();
            if (otherData instanceof Openable otherOpenable) {
                otherOpenable.setOpen(!wasOpen);
                otherHalf.setBlockData(otherOpenable);
            }
        }
        
        
        if (playSounds) {
            Sound sound = wasOpen ? closeSound : openSound;
            block.getWorld().playSound(block.getLocation(), sound, soundVolume, soundPitch);
        }
        
        plugin.debug("Toggled door at " + locationToKey(block.getLocation()) + " to " + (!wasOpen ? "open" : "closed"));
    }
    
    
    public boolean toggleDoorForPlayer(Block block, Player player) {
        if (isOnDoorCooldown(player)) {
            return false;
        }
        
        toggleDoor(block);
        setDoorCooldown(player);
        return true;
    }
    
    
    private Block findDoubleDoor(Block door) {
        BlockData data = door.getBlockData();
        if (!(data instanceof Door doorData)) {
            return null;
        }
        
        BlockFace facing = doorData.getFacing();
        BlockFace hinge = doorData.getHinge() == Door.Hinge.LEFT ? facing.getOppositeFace() : facing;
        
        
        Block[] adjacent = {
            door.getRelative(hinge.getOppositeFace()),
            door.getRelative(hinge)
        };
        
        for (Block check : adjacent) {
            if (check.getType() == door.getType()) {
                BlockData checkData = check.getBlockData();
                if (checkData instanceof Door checkDoor) {
                    
                    if (checkDoor.getFacing() == facing && 
                        checkDoor.getHalf() == doorData.getHalf()) {
                        return check;
                    }
                }
            }
        }
        
        return null;
    }
    
    
    public Block getBottomDoorBlock(Block block) {
        if (!isValidDoor(block.getType())) {
            return null;
        }
        
        BlockData data = block.getBlockData();
        if (data instanceof Door doorData) {
            if (doorData.getHalf() == Bisected.Half.TOP) {
                return block.getRelative(BlockFace.DOWN);
            }
        }
        
        return block;
    }
    
    
    public boolean isValidDoor(Material material) {
        return validDoorMaterials.contains(material);
    }
    
    
    public boolean isValidDoorMaterial(Material material) {
        return isValidDoor(material);
    }
    
    
    public String getLinkedCell(Location location) {
        String regionId = getLinkedRegion(location);
        if (regionId != null) {
            return regionId + ":" + location.getWorld().getName();
        }
        return null;
    }
    
    
    public void loadDoors() {
        if (!doorsFile.exists()) {
            plugin.getLogger().info("No doors.yml file found, starting fresh");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(doorsFile);
        doorLinks.clear();
        
        for (String key : config.getKeys(false)) {
            String regionId = config.getString(key);
            if (regionId != null) {
                doorLinks.put(key, regionId);
            }
        }
        
        plugin.getLogger().info("Loaded " + doorLinks.size() + " door links");
    }
    
    
    public void saveDoors() {
        FileConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<String, String> entry : doorLinks.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        
        try {
            config.save(doorsFile);
            plugin.debug("Saved " + doorLinks.size() + " door links");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save door links: " + e.getMessage());
        }
    }
    
    
    private String locationToKey(Location location) {
        return String.format("%s:%d:%d:%d",
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }
    
    
    public Map<String, String> getAllDoorLinks() {
        return new HashMap<>(doorLinks);
    }
    
    
    public int getDoorCount() {
        return doorLinks.size();
    }
    
    
    public int cleanupInvalidLinks() {
        int removed = 0;
        Iterator<Map.Entry<String, String>> iter = doorLinks.entrySet().iterator();
        
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            String regionId = entry.getValue();
            
            
            Region region = plugin.findRegionById(regionId);
            if (region == null) {
                iter.remove();
                removed++;
                plugin.getLogger().info("Removed invalid door link to non-existent region: " + regionId);
            }
        }
        
        if (removed > 0) {
            saveDoors();
        }
        
        return removed;
    }
    
    
    public void syncDoorOwnershipForRegion(String regionId) {
        plugin.debug("Syncing door ownership for region: " + regionId);
        
        
        List<Location> linkedDoors = findDoorsLinkedToRegion(regionId);
        
        if (linkedDoors.isEmpty()) {
            plugin.debug("No doors linked to region: " + regionId);
            return;
        }
        
        
        Region region = plugin.findRegionById(regionId);
        if (region == null) {
            plugin.debug("Region no longer exists, unlinking doors: " + regionId);
            
            for (Location doorLocation : linkedDoors) {
                unlinkDoor(doorLocation);
            }
            return;
        }
        
        plugin.debug("Found " + linkedDoors.size() + " doors linked to region " + regionId);
        
        
        if (!cellManager.isSold(region)) {
            plugin.debug("Region " + regionId + " is not sold, doors remain linked but no access");
            return;
        }
        
        
        plugin.debug("Door ownership synced for region: " + regionId);
    }
    
    
    public List<Location> findDoorsLinkedToRegion(String regionId) {
        List<Location> linkedDoors = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : doorLinks.entrySet()) {
            if (regionId.equals(entry.getValue())) {
                Location doorLocation = parseLocationFromKey(entry.getKey());
                if (doorLocation != null) {
                    linkedDoors.add(doorLocation);
                }
            }
        }
        
        return linkedDoors;
    }
    
    
    public int syncAllDoorOwnerships() {
        plugin.debug("Starting full door ownership sync");
        
        int syncedDoors = 0;
        Map<String, List<Location>> regionDoors = new HashMap<>();
        
        
        for (Map.Entry<String, String> entry : doorLinks.entrySet()) {
            String regionId = entry.getValue();
            Location doorLocation = parseLocationFromKey(entry.getKey());
            
            if (doorLocation != null) {
                regionDoors.computeIfAbsent(regionId, k -> new ArrayList<>()).add(doorLocation);
            }
        }
        
        
        for (String regionId : regionDoors.keySet()) {
            syncDoorOwnershipForRegion(regionId);
            syncedDoors += regionDoors.get(regionId).size();
        }
        
        plugin.debug("Synced " + syncedDoors + " doors across " + regionDoors.size() + " regions");
        return syncedDoors;
    }
    
    
    private Location parseLocationFromKey(String doorKey) {
        try {
            String[] parts = doorKey.split(":");
            if (parts.length != 4) {
                return null;
            }
            
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return null;
            }
            
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
} 
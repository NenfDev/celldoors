package dev.lsdmc.edencells.models;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;


public final class CellGroup {
    
    
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_DISPLAY_NAME_LENGTH = 64;
    private static final int MAX_REGIONS = 10000; 
    
    
    private final String name;
    private final String displayName;
    
    
    private final Set<String> regions = Collections.synchronizedSet(new LinkedHashSet<>());
    private final ConcurrentMap<String, Object> options = new ConcurrentHashMap<>();
    
    
    private volatile int cachedSize = -1;
    private volatile long lastModified = System.currentTimeMillis();
    
    
    private static final int DEFAULT_CELL_LIMIT = -1; 
    private static final double DEFAULT_TELEPORT_COST = -1; 
    
    
    public CellGroup(String name) {
        this(name, name);
    }
    
    
    public CellGroup(String name, String displayName) {
        this.name = validateName(name);
        this.displayName = validateDisplayName(displayName);
    }
    
    
    CellGroup(String name, String displayName, Set<String> regions, ConcurrentMap<String, Object> options) {
        this.name = validateName(name);
        this.displayName = validateDisplayName(displayName);
        
        
        if (regions != null) {
            for (String region : regions) {
                if (validateRegionId(region)) {
                    this.regions.add(region);
                }
            }
        }
        
        
        if (options != null) {
            this.options.putAll(options);
        }
        
        invalidateCache();
    }
    
    
    private static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Group name too long (max " + MAX_NAME_LENGTH + " characters)");
        }
        
        if (!VALID_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Group name contains invalid characters. Use only letters, numbers, underscores, and hyphens.");
        }
        
        return trimmed;
    }
    
    
    private static String validateDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        
        String trimmed = displayName.trim();
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Display name too long (max " + MAX_DISPLAY_NAME_LENGTH + " characters)");
        }
        
        return trimmed;
    }
    
    
    private static boolean validateRegionId(String regionId) {
        return regionId != null && 
               !regionId.trim().isEmpty() && 
               regionId.trim().length() <= MAX_NAME_LENGTH &&
               VALID_NAME_PATTERN.matcher(regionId.trim()).matches();
    }
    
    
    private void invalidateCache() {
        cachedSize = -1;
        lastModified = System.currentTimeMillis();
    }
    
    
    public String getName() {
        return name;
    }
    
    
    public String getDisplayName() {
        return displayName;
    }
    
    
    public Set<String> getRegions() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(regions));
    }
    
    
    public boolean addRegion(String regionId) {
        if (!validateRegionId(regionId)) {
            return false;
        }
        
        String trimmed = regionId.trim();
        
        
        if (regions.size() >= MAX_REGIONS) {
            throw new IllegalStateException("Too many regions in group (max " + MAX_REGIONS + ")");
        }
        
        boolean added = regions.add(trimmed);
        if (added) {
            invalidateCache();
        }
        return added;
    }
    
    
    public boolean removeRegion(String regionId) {
        if (regionId == null) {
            return false;
        }
        
        boolean removed = regions.remove(regionId.trim());
        if (removed) {
            invalidateCache();
        }
        return removed;
    }
    
    
    public boolean containsRegion(String regionId) {
        return regionId != null && regions.contains(regionId.trim());
    }
    
    
    public int size() {
        if (cachedSize == -1) {
            synchronized (regions) {
                if (cachedSize == -1) { 
                    cachedSize = regions.size();
                }
            }
        }
        return cachedSize;
    }
    
    
    public int getCellLimit() {
        Object limit = options.get("cellLimit");
        if (limit instanceof Number) {
            return ((Number) limit).intValue();
        }
        return DEFAULT_CELL_LIMIT;
    }
    
    
    public void setCellLimit(int limit) {
        if (limit < -1) {
            throw new IllegalArgumentException("Cell limit cannot be less than -1");
        }
        
        options.put("cellLimit", limit);
        lastModified = System.currentTimeMillis();
    }
    
    
    public double getTeleportCost() {
        Object cost = options.get("teleportCost");
        if (cost instanceof Number) {
            return ((Number) cost).doubleValue();
        }
        return DEFAULT_TELEPORT_COST;
    }
    
    
    public void setTeleportCost(double cost) {
        if (cost < -1 || Double.isNaN(cost) || Double.isInfinite(cost)) {
            throw new IllegalArgumentException("Invalid teleport cost");
        }
        
        options.put("teleportCost", cost);
        lastModified = System.currentTimeMillis();
    }
    
    
    public int getPriority() {
        Object value = options.get("priority");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    
    public void setPriority(int priority) {
        options.put("priority", priority);
        lastModified = System.currentTimeMillis();
    }

    
    public String getTeleportAccess() {
        Object value = options.get("teleportAccess");
        if (value instanceof String s) {
            return s;
        }
        return "all";
    }

    
    public void setTeleportAccess(String access) {
        if (access == null || access.trim().isEmpty()) {
            options.remove("teleportAccess");
        } else {
            options.put("teleportAccess", access.trim().toLowerCase());
        }
        lastModified = System.currentTimeMillis();
    }


    
    
    public String getRequiredPermission() {
        Object perm = options.get("permission");
        if (perm instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return null;
    }
    
    
    public void setRequiredPermission(String permission) {
        if (permission == null || permission.trim().isEmpty()) {
            options.remove("permission");
        } else {
            
            String trimmed = permission.trim();
            if (trimmed.length() > 128) {
                throw new IllegalArgumentException("Permission string too long");
            }
            options.put("permission", trimmed);
        }
        lastModified = System.currentTimeMillis();
    }
    
    
    public ConcurrentMap<String, Object> getOptions() {
        return new ConcurrentHashMap<>(options);
    }
    
    
    public Object getOption(String key) {
        return key != null ? options.get(key) : null;
    }
    
    
    public void setOption(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Option key cannot be null or empty");
        }
        
        String trimmedKey = key.trim();
        if (trimmedKey.length() > 64) {
            throw new IllegalArgumentException("Option key too long");
        }
        
        if (value == null) {
            options.remove(trimmedKey);
        } else {
            options.put(trimmedKey, value);
        }
        lastModified = System.currentTimeMillis();
    }
    
    
    public long getLastModified() {
        return lastModified;
    }
    
    
    public boolean isEmpty() {
        return regions.isEmpty();
    }
    
    
    public void clearRegions() {
        regions.clear();
        invalidateCache();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CellGroup)) return false;
        
        CellGroup other = (CellGroup) obj;
        return Objects.equals(name, other.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return String.format("CellGroup{name='%s', displayName='%s', regions=%d, options=%d}", 
            name, displayName, size(), options.size());
    }
}



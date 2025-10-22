package dev.lsdmc.edencells.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.Constants;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public final class SecurityManager {
    
    private final EdenCells plugin;
    private final Pattern regionIdPattern;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    
    private final Map<String, Cache<UUID, Integer>> rateLimiters = new ConcurrentHashMap<>();
    
    
    private final Cache<UUID, Long> teleportCooldowns;
    
    public SecurityManager(EdenCells plugin) {
        this.plugin = plugin;
        
        this.regionIdPattern = Pattern.compile(plugin.getConfigManager().getRegionIdPattern());
        
        
        rateLimiters.put("purchase", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.cell-purchase", Constants.RateLimits.PURCHASE_PER_MINUTE)));
        rateLimiters.put("member_add", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.member-add", Constants.RateLimits.MEMBER_ADD_PER_MINUTE)));
        rateLimiters.put("member_remove", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.member-remove", 5)));
        rateLimiters.put("door_interact", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.door-interact", Constants.RateLimits.DOOR_INTERACT_PER_MINUTE)));
        rateLimiters.put("gui_open", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.gui-open", Constants.RateLimits.GUI_OPEN_PER_MINUTE)));
        rateLimiters.put("npc_interact", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.npc-interact", 10)));
        rateLimiters.put("bulk_add", createRateLimiter(
            plugin.getConfig().getInt("security.rate-limits.bulk-add", Constants.RateLimits.BULK_ADD_PER_MINUTE)));
        
        
        int cooldownSeconds = plugin.getConfigManager().getTeleportationCooldownSeconds();
        this.teleportCooldowns = Caffeine.newBuilder()
            .expireAfterWrite(cooldownSeconds, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build();
    }
    
    
    private Cache<UUID, Integer> createRateLimiter(int maxPerMinute) {
        return Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1000) 
            .build();
    }
    
    
    public boolean isValidRegionId(String regionId) {
        if (regionId == null || regionId.trim().isEmpty()) {
            return false;
        }
        
        
        if (regionId.length() > plugin.getConfigManager().getMaxRegionIdLength()) {
            return false;
        }
        
        
        return regionIdPattern.matcher(regionId.trim()).matches();
    }
    
    
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = username.trim();
        
        
        if (trimmed.length() > Constants.Validation.MAX_USERNAME_LENGTH) {
            return false;
        }
        
        
        return trimmed.matches("^[a-zA-Z0-9_]+$");
    }
    
    
    public boolean isValidEconomyAmount(double amount) {
        return !Double.isNaN(amount) && 
               !Double.isInfinite(amount) && 
               amount >= 0 && 
               amount <= plugin.getConfigManager().getMaxTransaction();
    }
    
    
    public boolean isRateLimited(Player player, String action) {
        if (player == null || action == null) {
            return true; 
        }
        
        
        if (player.hasPermission(Constants.Permissions.BYPASS) || 
            player.hasPermission(Constants.Permissions.BYPASS_RATE_LIMIT)) {
            return false;
        }
        
        
        String specificBypass = getBypassPermissionForAction(action);
        if (specificBypass != null && player.hasPermission(specificBypass)) {
            return false;
        }
        
        Cache<UUID, Integer> limiter = rateLimiters.get(action);
        if (limiter == null) {
            return false; 
        }
        
        UUID playerId = player.getUniqueId();
        int currentCount = limiter.get(playerId, key -> 0);
        int maxAllowed = getMaxForAction(action);
        
        if (currentCount >= maxAllowed) {
            return true;
        }
        
        
        limiter.put(playerId, currentCount + 1);
        return false;
    }
    
    
    private String getBypassPermissionForAction(String action) {
        return switch (action) {
            case "purchase" -> Constants.Permissions.BYPASS_RATE_LIMIT_PURCHASE;
            case "member_add", "member_remove" -> Constants.Permissions.BYPASS_RATE_LIMIT_MEMBER;
            case "door_interact" -> Constants.Permissions.BYPASS_RATE_LIMIT_DOOR;
            case "gui_open" -> Constants.Permissions.BYPASS_RATE_LIMIT_GUI;
            case "npc_interact" -> Constants.Permissions.BYPASS_RATE_LIMIT_NPC;
            default -> null;
        };
    }
    
    
    private int getMaxForAction(String action) {
        return switch (action) {
            case "purchase" -> plugin.getConfig().getInt("security.rate-limits.cell-purchase", Constants.RateLimits.PURCHASE_PER_MINUTE);
            case "member_add" -> plugin.getConfig().getInt("security.rate-limits.member-add", Constants.RateLimits.MEMBER_ADD_PER_MINUTE);
            case "member_remove" -> plugin.getConfig().getInt("security.rate-limits.member-remove", 5);
            case "door_interact" -> plugin.getConfig().getInt("security.rate-limits.door-interact", Constants.RateLimits.DOOR_INTERACT_PER_MINUTE);
            case "gui_open" -> plugin.getConfig().getInt("security.rate-limits.gui-open", Constants.RateLimits.GUI_OPEN_PER_MINUTE);
            case "npc_interact" -> plugin.getConfig().getInt("security.rate-limits.npc-interact", 10);
            case "bulk_add" -> plugin.getConfig().getInt("security.rate-limits.bulk-add", Constants.RateLimits.BULK_ADD_PER_MINUTE);
            default -> 10; 
        };
    }
    
    
    public boolean isOnTeleportCooldown(Player player) {
        if (player == null) return true;
        
        
        if (player.hasPermission(Constants.Permissions.BYPASS) || 
            player.hasPermission(Constants.Permissions.BYPASS_COOLDOWN)) {
            return false;
        }
        
        return teleportCooldowns.getIfPresent(player.getUniqueId()) != null;
    }
    
    
    public void setTeleportCooldown(Player player) {
        if (player == null) return;
        
        teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    
    public int getRemainingTeleportCooldown(Player player) {
        if (player == null) return 0;
        
        Long cooldownTime = teleportCooldowns.getIfPresent(player.getUniqueId());
        if (cooldownTime == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - cooldownTime;
        int cooldownSeconds = plugin.getConfigManager().getTeleportationCooldownSeconds();
        long remaining = (cooldownSeconds * 1000L) - elapsed;
        
        return Math.max(0, (int) (remaining / 1000));
    }
    
    
    public void auditLog(Player player, String action, String target, String details) {
        if (player == null || action == null) {
            return;
        }
        
        
        if (!plugin.getConfig().getBoolean("security.audit.enabled", true)) {
            return;
        }
        
        
        List<String> logActions = plugin.getConfig().getStringList("security.audit.log-actions");
        if (!logActions.isEmpty() && !logActions.contains(action.toLowerCase())) {
            return;
        }
        
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String logFileName = plugin.getConfig().getString("security.audit.log-file", Constants.Storage.AUDIT_LOG_FILE);
                File auditFile = new File(plugin.getDataFolder(), logFileName);
                
                
                if (!auditFile.getParentFile().exists()) {
                    auditFile.getParentFile().mkdirs();
                }
                
                String timestamp = LocalDateTime.now().format(dateFormat);
                String playerName = sanitizeInput(player.getName());
                String actionSafe = sanitizeInput(action);
                String targetSafe = target != null ? sanitizeInput(target) : "N/A";
                String detailsSafe = details != null ? sanitizeInput(details) : "N/A";
                
                String logEntry = String.format("[%s] Player: %s | Action: %s | Target: %s | Details: %s%n",
                    timestamp, playerName, actionSafe, targetSafe, detailsSafe);
                
                
                Files.write(auditFile.toPath(), logEntry.getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write audit log: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().severe("Unexpected error in audit logging: " + e.getMessage());
            }
        });
    }
    
    
    public String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        
        return input.replaceAll("[\\r\\n\\t]", " ")
                   .replaceAll("[<>\"'&]", "")
                   .trim();
    }
    
    
    public boolean hasPermission(Permissible permissible, String permission) {
        if (permissible == null || permission == null) {
            return false;
        }
        
        return permissible.hasPermission(permission);
    }
    
    
    public void cleanupRateLimits() {
        
        rateLimiters.values().forEach(cache -> cache.cleanUp());
        teleportCooldowns.cleanUp();
    }
    
    
    public void clearRateLimits(Player player) {
        if (player == null) return;
        
        UUID playerId = player.getUniqueId();
        
        
        for (Cache<UUID, Integer> limiter : rateLimiters.values()) {
            limiter.invalidate(playerId);
        }
        
        
        teleportCooldowns.invalidate(playerId);
    }
    
    
    public String getRateLimitStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Rate Limiter Statistics:\n");
        
        for (var entry : rateLimiters.entrySet()) {
            long size = entry.getValue().estimatedSize();
            stats.append(String.format("- %s: %d active entries\n", entry.getKey(), size));
        }
        
        stats.append(String.format("- teleport_cooldowns: %d active entries\n", 
            teleportCooldowns.estimatedSize()));
        
        return stats.toString();
    }
} 
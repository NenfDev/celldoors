package dev.lsdmc.edencells.utils;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;


public final class PermissionManager {

    private PermissionManager() {} 

    
    public static boolean hasPermission(Player player, String permission) {
        if (player == null || permission == null) {
            return false;
        }

        return player.hasPermission(permission);
    }

    
    public static boolean hasPermission(Permissible permissible, String permission) {
        if (permissible == null || permission == null) {
            return false;
        }

        return permissible.hasPermission(permission);
    }

    
    public static boolean isAdmin(Player player) {
        return hasPermission(player, Constants.Permissions.ADMIN);
    }

    
    public static boolean canBypass(Player player) {
        return hasPermission(player, Constants.Permissions.BYPASS);
    }

    
    public static boolean hasAnyPermission(Player player, String... permissions) {
        if (player == null || permissions == null) {
            return false;
        }

        for (String permission : permissions) {
            if (hasPermission(player, permission)) {
                return true;
            }
        }

        return false;
    }

    
    public static boolean hasAllPermissions(Player player, String... permissions) {
        if (player == null || permissions == null) {
            return false;
        }

        for (String permission : permissions) {
            if (!hasPermission(player, permission)) {
                return false;
            }
        }

        return true;
    }

    
    public static boolean hasNpcAccess(Player player, String groupId) {
        if (player == null || groupId == null) {
            return false;
        }

        
        if (hasPermission(player, Constants.Permissions.NPC_TELEPORT)) {
            return true;
        }

        
        String groupSpecificPerm = Constants.Permissions.NPC_TELEPORT_PREFIX + groupId.toLowerCase();
        if (hasPermission(player, groupSpecificPerm)) {
            return true;
        }

        
        EdenCells plugin = EdenCells.getInstance();
        if (plugin != null && plugin.getCellGroupManager() != null) {
            CellGroup group = plugin.getCellGroupManager().getGroup(groupId);
            if (group != null && group.getRequiredPermission() != null) {
                
                String required = group.getRequiredPermission().trim();
                if (required.toLowerCase().startsWith("group:")) {
                    String lpGroup = required.substring("group:".length()).trim();
                    return isInLuckPermsGroup(player, lpGroup);
                }
                return hasPermission(player, required);
            }
        }

        return false;
    }

    
    private static boolean isInLuckPermsGroup(Player player, String groupName) {
        try {
            
            if (EdenCells.getInstance().getServer().getPluginManager().getPlugin("LuckPerms") == null) {
                return false;
            }
            String target = groupName.toLowerCase();
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = providerClass.getMethod("get").invoke(null);

            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            Class<?> contextManagerClass = Class.forName("net.luckperms.api.context.ContextManager");
            Class<?> queryOptionsClass = Class.forName("net.luckperms.api.query.QueryOptions");
            Class<?> groupClass = Class.forName("net.luckperms.api.model.group.Group");

            Object userManager = luckPermsClass.getMethod("getUserManager").invoke(api);
            Object user = userManagerClass.getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) return false;

            Object contextManager = luckPermsClass.getMethod("getContextManager").invoke(api);
            Object queryOptions = contextManagerClass.getMethod("getQueryOptions", userClass).invoke(contextManager, user);
            @SuppressWarnings("unchecked")
            java.util.Collection<?> groups = (java.util.Collection<?>) userClass
                .getMethod("getInheritedGroups", queryOptionsClass)
                .invoke(user, queryOptions);

            for (Object g : groups) {
                String name = (String) groupClass.getMethod("getName").invoke(g);
                if (name != null && name.equalsIgnoreCase(target)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    
    public static boolean hasGroupAccess(Player player, dev.lsdmc.edencells.models.CellGroup group) {
        if (player == null || group == null) return true;
        String required = group.getRequiredPermission();
        if (required == null || required.trim().isEmpty()) return true;
        String trimmed = required.trim();
        if (trimmed.toLowerCase().startsWith("group:")) {
            String lpGroup = trimmed.substring("group:".length()).trim();
            return isInLuckPermsGroup(player, lpGroup);
        }
        return hasPermission(player, trimmed);
    }

    
    public static boolean hasBypassPayment(Player player) {
        return hasPermission(player, Constants.Permissions.BYPASS_PAYMENT);
    }

    
    public static boolean hasBypassCooldown(Player player) {
        return hasPermission(player, Constants.Permissions.BYPASS_COOLDOWN);
    }

    
    public static boolean checkPermission(Player player, String permission) {
        if (hasPermission(player, permission)) {
            return true;
        }

        MessageUtils.sendNoPermission(player);
        return false;
    }

    
    public static boolean checkPermission(Player player, String permission, String message) {
        if (hasPermission(player, permission)) {
            return true;
        }

        MessageUtils.sendError(player, message);
        return false;
    }
}
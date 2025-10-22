package dev.lsdmc.edencells.utils;

import dev.lsdmc.edencells.EdenCells;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public final class PermissionRegistry {

    private static final Set<String> registered = Collections.synchronizedSet(new HashSet<>());

    private PermissionRegistry() {}

    
    public static void register(String permissionNode, String description) {
        if (permissionNode == null || permissionNode.isBlank()) {
            return;
        }

        String node = permissionNode.trim();
        if (registered.contains(node)) {
            return;
        }

        try {
            Permission perm = new Permission(node, description != null ? description : "", PermissionDefault.FALSE);
            Bukkit.getPluginManager().addPermission(perm);
            registered.add(node);
            EdenCells.getInstance().debug("Registered dynamic permission: " + node);
        } catch (Exception e) {
            EdenCells.getInstance().getLogger().warning("Failed to register permission '" + node + "': " + e.getMessage());
        }
    }

    
    public static void unregister(String permissionNode) {
        if (permissionNode == null || permissionNode.isBlank()) {
            return;
        }

        String node = permissionNode.trim();
        try {
            Permission perm = Bukkit.getPluginManager().getPermission(node);
            if (perm != null) {
                Bukkit.getPluginManager().removePermission(perm);
            }
        } catch (Exception e) {
            EdenCells.getInstance().getLogger().warning("Failed to unregister permission '" + node + "': " + e.getMessage());
        } finally {
            registered.remove(node);
        }
    }

    
    public static void clearAll() {
        synchronized (registered) {
            for (String node : new HashSet<>(registered)) {
                try {
                    Permission perm = Bukkit.getPluginManager().getPermission(node);
                    if (perm != null) {
                        Bukkit.getPluginManager().removePermission(perm);
                    }
                } catch (Exception ignored) {
                }
            }
            registered.clear();
        }
    }
}



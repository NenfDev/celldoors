package dev.lsdmc.edencells.utils;

import dev.lsdmc.edencells.EdenCells;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;


public final class ConfigManager {
    
    private final EdenCells plugin;
    private FileConfiguration config;
    
    public ConfigManager(EdenCells plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("ConfigManager reloaded - config sections: " + 
            (config != null ? config.getKeys(false) : "NULL"));
    }
    
    
    public void debugConfig() {
        plugin.getLogger().info("=== ConfigManager Debug ===");
        plugin.getLogger().info("Config instance: " + (config != null ? "NOT NULL" : "NULL"));
        if (config != null) {
            plugin.getLogger().info("Config sections: " + config.getKeys(false));
            if (config.contains("teleportation")) {
                plugin.getLogger().info("Teleportation section: " + config.getConfigurationSection("teleportation").getKeys(false));
                if (config.contains("teleportation.messages")) {
                    plugin.getLogger().info("Messages section: " + config.getConfigurationSection("teleportation.messages").getKeys(false));
                    String testMessage = config.getString("teleportation.messages.no_permission", "NOT_FOUND");
                    plugin.getLogger().info("Test message 'no_permission': " + testMessage);
                } else {
                    plugin.getLogger().warning("teleportation.messages section not found!");
                }
            } else {
                plugin.getLogger().warning("teleportation section not found!");
            }
        }
        plugin.getLogger().info("=== End ConfigManager Debug ===");
    }
    
    
    
    public boolean isDebugEnabled() {
        return config.getBoolean("general.debug", false);
    }
    
    public String getLocale() {
        return config.getString("general.locale", "en_US");
    }
    
    
    
    public String getPrefix() {
        return config.getString("messages.prefix", Constants.Messages.PREFIX);
    }
    
    public String getPrimaryColor() {
        return config.getString("messages.colors.primary", "#9D4EDD");
    }
    
    public String getSecondaryColor() {
        return config.getString("messages.colors.secondary", "#06FFA5");
    }
    
    public String getAccentColor() {
        return config.getString("messages.colors.accent", "#FFB3C6");
    }
    
    public String getErrorColor() {
        return config.getString("messages.colors.error", "#FF6B6B");
    }
    
    public String getSuccessColor() {
        return config.getString("messages.colors.success", "#51CF66");
    }
    
    public String getNeutralColor() {
        return config.getString("messages.colors.neutral", "#ADB5BD");
    }
    
    
    
    public int getMaxCellsPerPlayer() {
        return config.getInt("cells.max-per-player", 10);
    }
    
    public int getDefaultMaxMembers() {
        return config.getInt("cells.default-max-members", 5);
    }
    
    public List<String> getCellSignKeywords() {
        return config.getStringList("cells.sign-keywords");
    }
    
    
    
    public int getGuiItemsPerPage() {
        return config.getInt("cells.gui.items-per-page", 45);
    }
    
    public String getGuiDefaultSort() {
        return config.getString("cells.gui.default-sort", "name");
    }
    
    public boolean showPricesInGui() {
        return config.getBoolean("cells.gui.show-prices", true);
    }
    
    public boolean showExpirationInGui() {
        return config.getBoolean("cells.gui.show-expiration", true);
    }
    
    public boolean requireGuiConfirmation() {
        return config.getBoolean("cells.gui.require-confirmation", true);
    }
    
    public boolean playGuiClickSounds() {
        return config.getBoolean("cells.gui.click-sounds", true);
    }
    
    
    
    public String getCurrencySymbol() {
        return config.getString("economy.currency-symbol", "$");
    }
    
    public String getCurrencySuffix() {
        return config.getString("economy.currency-suffix", "");
    }
    
    public double getMemberAddCost() {
        return config.getDouble("economy.members.add-cost", 100.0);
    }
    
    public double getMemberRemoveCost() {
        return config.getDouble("economy.members.remove-cost", 50.0);
    }
    
    public double getSellFeePercent() {
        return config.getDouble("economy.fees.sell-fee-percent", 0.1);
    }
    
    public double getListingFee() {
        return config.getDouble("economy.fees.listing-fee", 0.0);
    }
    
    
    
    
    
    
    
    public boolean isTeleportationEnabled() {
        return config.getBoolean("teleportation.enabled", true);
    }
    
    public double getTeleportationDefaultCost() {
        return config.getDouble("teleportation.default-cost", 500.0);
    }
    
    public int getTeleportationCooldownSeconds() {
        return config.getInt("teleportation.cooldown-seconds", 1);
    }
    
    public List<String> getTeleportationFreeGroups() {
        return config.getStringList("teleportation.free-groups");
    }
    
    public String getTeleportationSoundSuccess() {
        return config.getString("teleportation.sounds.success", "ENTITY_ENDERMAN_TELEPORT");
    }
    
    public String getTeleportationSoundDenied() {
        return config.getString("teleportation.sounds.denied", "ENTITY_VILLAGER_NO");
    }
    
    public String getTeleportationSoundError() {
        return config.getString("teleportation.sounds.error", "BLOCK_NOTE_BLOCK_BASS");
    }
    
    public String getTeleportationMessage(String key) {
        return config.getString("teleportation.messages." + key, "Missing message: " + key);
    }
    
    
    public double getTeleportBaseCost() {
        return getTeleportationDefaultCost();
    }
    
    public boolean requireTeleportPayment() {
        return config.getBoolean("teleportation.require-payment", true);
    }
    
    public int getTeleportCooldown() {
        return getTeleportationCooldownSeconds();
    }
    
    public List<String> getTeleportFreeGroups() {
        return getTeleportationFreeGroups();
    }
    
    public boolean playTeleportSound() {
        return config.getBoolean("teleportation.effects.play-sound", true);
    }
    
    public String getTeleportSound() {
        return config.getString("teleportation.effects.sound", "entity.enderman.teleport");
    }
    
    public boolean showTeleportParticles() {
        return config.getBoolean("teleportation.effects.show-particles", true);
    }
    
    public String getTeleportParticle() {
        return config.getString("teleportation.effects.particle", "PORTAL");
    }
    
    
    
    public List<String> getValidDoorMaterials() {
        return config.getStringList("doors.valid-materials");
    }
    
    public boolean areDoorSoundsEnabled() {
        return config.getBoolean("doors.sounds.enabled", true);
    }
    
    public String getDoorOpenSound() {
        return config.getString("doors.sounds.open-sound", "block.iron_door.open");
    }
    
    public String getDoorCloseSound() {
        return config.getString("doors.sounds.close-sound", "block.iron_door.close");
    }
    
    public float getDoorSoundVolume() {
        return (float) config.getDouble("doors.sounds.volume", 1.0);
    }
    
    public float getDoorSoundPitch() {
        return (float) config.getDouble("doors.sounds.pitch", 1.0);
    }
    
    public int getDoorAutoCloseDelay() {
        return config.getInt("doors.auto-close-delay", 0);
    }
    
    
    
    public int getRateLimit(String action) {
        return config.getInt("security.rate-limits." + action, 10);
    }
    
    public int getMaxRegionIdLength() {
        return config.getInt("security.validation.max-region-id-length", 32);
    }
    
    public String getRegionIdPattern() {
        return config.getString("security.validation.region-id-pattern", "^[a-zA-Z0-9_-]+$");
    }
    
    public double getMaxTransaction() {
        return config.getDouble("security.validation.max-transaction", 1000000.0);
    }
    
    public boolean isAuditEnabled() {
        return config.getBoolean("security.audit.enabled", true);
    }
    
    public String getAuditLogFile() {
        return config.getString("security.audit.log-file", "logs/audit.log");
    }
    
    public List<String> getAuditLogActions() {
        return config.getStringList("security.audit.log-actions");
    }
    
    
    
    public boolean useArmEconomy() {
        return config.getBoolean("integrations.arm.use-arm-economy", true);
    }
    
    public boolean syncArmData() {
        return config.getBoolean("integrations.arm.sync-data", true);
    }
    
    public boolean checkArmPermissions() {
        return config.getBoolean("integrations.arm.check-permissions", true);
    }
    
    public boolean isCitizensEnabled() {
        return config.getBoolean("integrations.citizens.enabled", true);
    }
    
    public String getCitizensNpcFormat() {
        return config.getString("integrations.citizens.npc-name-format", "&b&lCell Teleporter");
    }
    
    public String getCitizensNpcSkin() {
        return config.getString("integrations.citizens.npc-skin", "Notch");
    }
    
    public boolean checkWorldGuardRegions() {
        return config.getBoolean("integrations.worldguard.check-regions", true);
    }
    
    public boolean respectWorldGuardFlags() {
        return config.getBoolean("integrations.worldguard.respect-flags", true);
    }

    
    public boolean forceGroupPermissions() {
        return config.getBoolean("integrations.luckperms.force-group-permissions", false);
    }

    public String getLuckPermsGroupPrefix() {
        return config.getString("integrations.luckperms.group-prefix", "group:");
    }
    
    
    
    public int getPlayerCacheTime() {
        return config.getInt("performance.cache.player-cache-time", 5);
    }
    
    public int getRegionCacheTime() {
        return config.getInt("performance.cache.region-cache-time", 10);
    }
    
    public int getMaxCacheSize() {
        return config.getInt("performance.cache.max-cache-size", 1000);
    }
    
    public boolean useAsyncSaves() {
        return config.getBoolean("performance.async.async-saves", true);
    }
    
    public boolean useAsyncEconomy() {
        return config.getBoolean("performance.async.async-economy", true);
    }
    
    public int getThreadPoolSize() {
        return config.getInt("performance.async.thread-pool-size", 4);
    }
    
    
    
    public int getAutoSaveInterval() {
        return config.getInt("maintenance.auto-save-interval", 5);
    }
    
    public boolean isBackupEnabled() {
        return config.getBoolean("maintenance.backup.enabled", false);
    }
    
    public int getBackupInterval() {
        return config.getInt("maintenance.backup.interval", 24);
    }
    
    public int getMaxBackups() {
        return config.getInt("maintenance.backup.max-backups", 7);
    }
    
    public String getBackupFolder() {
        return config.getString("maintenance.backup.folder", "backups");
    }
    
    public boolean removeExpiredRentals() {
        return config.getBoolean("maintenance.cleanup.remove-expired-rentals", true);
    }
    
    public int getAuditLogRetention() {
        return config.getInt("maintenance.cleanup.audit-log-retention", 30);
    }
    
    public boolean cleanOrphanedDoors() {
        return config.getBoolean("maintenance.cleanup.clean-orphaned-doors", true);
    }
    
    
    
    public int getConfigVersion() {
        return config.getInt("config-version", 1);
    }
}

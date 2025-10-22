package dev.lsdmc.edencells;

import dev.lsdmc.edencells.commands.CellCommands;
import dev.lsdmc.edencells.commands.CellGroupCommands;
import dev.lsdmc.edencells.commands.ConfigCommands;
import dev.lsdmc.edencells.commands.DoorCommands;
import dev.lsdmc.edencells.commands.MainCommand;
import dev.lsdmc.edencells.commands.TeleportNPCCommands;
import dev.lsdmc.edencells.commands.SecurityCommands;
import dev.lsdmc.edencells.managers.DoorManager;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.managers.TeleportNPCManager;
import dev.lsdmc.edencells.managers.SyncManager;
import dev.lsdmc.edencells.gui.CellGUIManager;
import dev.lsdmc.edencells.listeners.CellSignListener;
import dev.lsdmc.edencells.listeners.DoorInteractionListener;
import dev.lsdmc.edencells.listeners.GUIListener;
import dev.lsdmc.edencells.models.CellGroupManager;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.ConfigManager;
import dev.lsdmc.edencells.utils.Constants;

import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.event.Listener;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;



public class EdenCells extends JavaPlugin implements Listener {
    private static EdenCells instance;
    
    private Economy economy;
    private AdvancedRegionMarket arm;
    private SecurityManager securityManager;
    private CellManager cellManager;
    private DoorManager doorManager;
    private TeleportNPCManager teleportNPCManager;
    private CellGUIManager guiManager;
    private CellSignListener cellSignListener;
    private GUIListener guiListener;
    private CellGroupManager cellGroupManager;
    private ConfigManager configManager;
    private SyncManager syncManager;
    
    @Override
    public void onEnable() {
        try {
            instance = this;
            
            
            Constants.Keys.init(this);
            
            
            if (!getServer().getPluginManager().isPluginEnabled("AdvancedRegionMarket")) {
                getLogger().severe("AdvancedRegionMarket not found! Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            
            this.arm = AdvancedRegionMarket.getInstance();
            if (this.arm == null) {
                getLogger().severe("Failed to get AdvancedRegionMarket instance!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            
            if (!setupEconomy()) {
                getLogger().warning("Economy setup failed, some features may be limited.");
            } else {
                
                boolean useArmEconomy = getConfig().getBoolean("integrations.arm.use-arm-economy", true);
                if (useArmEconomy) {
                    getLogger().info("ARM economy integration enabled - ARM will handle all economy transactions");
                } else {
                    getLogger().info("Using Vault economy system for transactions");
                }
            }
            
            
            saveDefaultConfig();
            
            
            this.configManager = new ConfigManager(this);
            
            this.configManager.reload();
            
            
            this.securityManager = new SecurityManager(this);
            this.cellManager = new CellManager(this, arm, economy, securityManager);
            this.cellGroupManager = new CellGroupManager(this);
            this.doorManager = new DoorManager(this, cellManager, securityManager);
            this.teleportNPCManager = new TeleportNPCManager(this, cellManager, economy, securityManager);
            this.teleportNPCManager.enable();
            this.syncManager = new SyncManager(this);
            
            
            if (getServer().getPluginManager().isPluginEnabled("Citizens")) {
                try {
                    net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(
                        net.citizensnpcs.api.trait.TraitInfo.create(dev.lsdmc.edencells.npc.TeleportNPC.class)
                    );
                    getLogger().info("Registered TeleportNPC trait with Citizens");
                } catch (Exception e) {
                    getLogger().warning("Failed to register Citizens trait: " + e.getMessage());
                }
            }
            
            
            try {
                this.guiManager = new CellGUIManager(this);
            } catch (Exception e) {
                getLogger().warning("Failed to initialize GUI manager: " + e.getMessage());
                
                this.guiManager = null;
            }
            
            
            try {
                this.cellSignListener = new CellSignListener(this);
                this.guiListener = new GUIListener(this, cellManager, securityManager);
                
                dev.lsdmc.edencells.listeners.AdminGUIListener adminGUIListener = new dev.lsdmc.edencells.listeners.AdminGUIListener(this);
                dev.lsdmc.edencells.listeners.AdminGroupPromptListener adminGroupPromptListener = new dev.lsdmc.edencells.listeners.AdminGroupPromptListener(this);
                dev.lsdmc.edencells.listeners.AdminPlayerPrompt adminPlayerPrompt = new dev.lsdmc.edencells.listeners.AdminPlayerPrompt(this);
                
                
                DoorInteractionListener doorInteractionListener = new DoorInteractionListener(this, doorManager, securityManager);
                
                
                getServer().getPluginManager().registerEvents(cellSignListener, this);
                getServer().getPluginManager().registerEvents(guiListener, this);
                getServer().getPluginManager().registerEvents(adminGUIListener, this);
                getServer().getPluginManager().registerEvents(adminGroupPromptListener, this);
                getServer().getPluginManager().registerEvents(adminPlayerPrompt, this);
                getServer().getPluginManager().registerEvents(doorInteractionListener, this);
            } catch (Exception e) {
                getLogger().severe("Failed to initialize listeners: " + e.getMessage());
                throw e;
            }
            
            
            try {
                new MainCommand(this);
                
                
                CellCommands cellCommands = new CellCommands(this);
                getCommand("cell").setExecutor(cellCommands);
                getCommand("cell").setTabCompleter(cellCommands);
                
                
                CellGroupCommands cellGroupCommands = new CellGroupCommands(this);
                getCommand("cellgroup").setExecutor(cellGroupCommands);
                getCommand("cellgroup").setTabCompleter(cellGroupCommands);
                
                
                DoorCommands doorCommands = new DoorCommands(this, doorManager);
                getCommand("door").setExecutor(doorCommands);
                getCommand("door").setTabCompleter(doorCommands);
                
                
                TeleportNPCCommands teleportNPCCommands = new TeleportNPCCommands(this, teleportNPCManager);
                getCommand("teleportnpc").setExecutor(teleportNPCCommands);
                getCommand("teleportnpc").setTabCompleter(teleportNPCCommands);
                
                
                ConfigCommands configCommands = new ConfigCommands(this);
                getCommand("econfig").setExecutor(configCommands);
                getCommand("econfig").setTabCompleter(configCommands);
                
                
                SecurityCommands securityCommands = new SecurityCommands(this);
                getCommand("esecurity").setExecutor(securityCommands);
                getCommand("esecurity").setTabCompleter(securityCommands);

                
                new dev.lsdmc.edencells.commands.AdminCommands(this);
            } catch (Exception e) {
                getLogger().severe("Failed to register commands: " + e.getMessage());
                throw e;
            }
            
            
            try {
                
                getCellGroupManager().getAllGroups().values().forEach(group -> {
                    String perm = group.getRequiredPermission();
                    if (perm != null && !perm.isBlank()) {
                        dev.lsdmc.edencells.utils.PermissionRegistry.register(perm, "Access to cell group '" + group.getName() + "'");
                    }
                });
            } catch (Exception e) {
                getLogger().warning("Failed to register dynamic permissions: " + e.getMessage());
            }

            getLogger().info("EdenCells has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Critical error during plugin initialization: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling EdenCells...");
        
        try {
            
            if (cellGroupManager != null) {
                cellGroupManager.saveGroups();
                getLogger().info("Saved cell group configurations");
            }
            
            if (doorManager != null) {
                doorManager.saveDoors();
                getLogger().info("Saved door configurations");
            }
            
            if (teleportNPCManager != null) {
                teleportNPCManager.disable();
                getLogger().info("Saved teleport NPC configurations");
            }
            
            
            if (guiManager != null) {
                dev.lsdmc.edencells.gui.CellGUI.cleanupSessions();
                getLogger().info("Cleaned up GUI sessions");
            }
            
            getLogger().info("EdenCells disabled successfully");
            
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found, economy features disabled.");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Economy provider not found, economy features disabled.");
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    
    public static EdenCells getInstance() {
        return instance;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public AdvancedRegionMarket getARM() {
        return arm;
    }
    
    public SecurityManager getSecurityManager() {
        return securityManager;
    }
    
    public CellManager getCellManager() {
        return cellManager;
    }
    
    public DoorManager getDoorManager() {
        return doorManager;
    }
    
    public TeleportNPCManager getTeleportNPCManager() {
        return teleportNPCManager;
    }
    
    public CellGUIManager getGuiManager() {
        return guiManager;
    }
    
    
    public CellGroupManager getCellGroupManager() {
        return cellGroupManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public SyncManager getSyncManager() {
        return syncManager;
    }
    
    public double getMemberAddCost() {
        return getConfig().getDouble("economy.members.add-cost", 100.0);
    }
    
    public double getMemberRemoveCost() {
        return getConfig().getDouble("economy.members.remove-cost", 50.0);
    }
    
    
    public void reload() {
        reloadConfig();
        
        if (configManager != null) {
            configManager.reload();
        }
        
    }
    
    
    public Region findRegionById(String regionId) {
        if (regionId == null || regionId.trim().isEmpty() || arm == null) {
            return null;
        }
        
        try {
            for (Region r : arm.getRegionManager()) {
                if (r != null && r.getRegion() != null && 
                    r.getRegion().getId().equalsIgnoreCase(regionId.trim())) {
                    return r;
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error finding region '" + regionId + "': " + e.getMessage());
        }
        return null;
    }
    
    public String formatCurrency(double amount) {
        if (economy == null) {
            return String.format("$%.2f", amount);
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            getLogger().warning("Error formatting currency: " + e.getMessage());
            return String.format("$%.2f", amount);
        }
    }
    
    public void debug(String message) {
        if (message == null) return;
        
        try {
            if (getConfig().getBoolean(Constants.Config.DEBUG, false)) {
                getLogger().info("[DEBUG] " + message);
            }
        } catch (Exception e) {
            
            getLogger().info("[DEBUG] " + message);
        }
    }
}
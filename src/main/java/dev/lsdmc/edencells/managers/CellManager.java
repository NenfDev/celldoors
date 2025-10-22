package dev.lsdmc.edencells.managers;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.models.CellGroupManager;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.AdvancedRegionMarket;
import net.alex9849.arm.regions.Region;
import net.alex9849.arm.regions.RentRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


public final class CellManager {
    
    private final EdenCells plugin;
    private final AdvancedRegionMarket arm;
    private final Economy economy;
    private final SecurityManager security;
    
    public CellManager(EdenCells plugin, AdvancedRegionMarket arm, Economy economy, SecurityManager security) {
        this.plugin = plugin;
        this.arm = arm;
        this.economy = economy;
        this.security = security;
        
        
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (arm == null) {
            throw new IllegalArgumentException("AdvancedRegionMarket cannot be null");
        }
        if (security == null) {
            throw new IllegalArgumentException("SecurityManager cannot be null");
        }
    }
    
    
    public Region getCell(String cellId, World world) {
        if (cellId == null || cellId.trim().isEmpty()) {
            plugin.debug("getCell called with null or empty cellId");
            return null;
        }
        
        if (world == null) {
            plugin.debug("getCell called with null world");
            return null;
        }
        
        try {
            String trimmedId = cellId.trim();
            for (Region region : arm.getRegionManager()) {
                if (region != null && 
                    region.getRegion() != null && 
                    region.getRegion().getId().equalsIgnoreCase(trimmedId) &&
                    region.getRegionworld().equals(world)) {
                return region;
            }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting cell '" + cellId + "': " + e.getMessage());
        }
        
        return null;
    }
    
    
    public List<Region> getPlayerCells(OfflinePlayer player) {
        if (player == null) {
            return Collections.emptyList();
        }
        
        try {
            return arm.getRegionManager()
                .getRegionsByOwner(player.getUniqueId())
            .stream()
                .filter(Objects::nonNull)
            .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting cells for player " + player.getName() + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    
    public List<Region> getPlayerCellsInRegion(OfflinePlayer player, String worldName, String regionName) {
        if (player == null || worldName == null || regionName == null) {
            return Collections.emptyList();
        }
        
        try {
            return getPlayerCells(player).stream()
                .filter(region -> region != null && 
                         region.getRegion() != null &&
                         region.getRegionworld().getName().equalsIgnoreCase(worldName.trim()) &&
                         region.getRegion().getId().toLowerCase().contains(regionName.toLowerCase().trim()))
            .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player cells in region: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    
    public boolean hasAccess(Player player, Region cell) {
        if (player == null || cell == null) {
            return false;
        }
        
        try {
            UUID playerId = player.getUniqueId();
            
            
            if (player.hasPermission(Constants.Permissions.BYPASS) ||
                player.hasPermission(Constants.Permissions.ADMIN)) {
                return true;
            }

            
            if (cell.getRegion().hasOwner(playerId)) {
                return true;
            }
            
            
            if (cell.getRegion().hasMember(playerId)) {
                return true;
            }
            
            
            if (player.hasPermission(Constants.Permissions.BYPASS)) {
                return true;
            }
        
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking access for player " + player.getName() + ": " + e.getMessage());
        }
        
        return false;
    }
    
    
    public boolean isOwner(OfflinePlayer player, Region cell) {
        if (player == null || cell == null) {
            return false;
        }
        
        try {
            
            if (player instanceof org.bukkit.entity.Player online && (
                    online.hasPermission(Constants.Permissions.BYPASS) ||
                    online.hasPermission(Constants.Permissions.ADMIN))) {
                return true;
            }
            return cell.getRegion().hasOwner(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking ownership: " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean isSold(Region cell) {
        if (cell == null) {
            return false;
        }
        
        try {
            return cell.isSold();
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if cell is sold: " + e.getMessage());
        return false;
        }
    }
    
    
    public double getPrice(Region cell) {
        if (cell == null) {
            return 0.0;
        }
        
        try {
            if (cell instanceof RentRegion rentRegion) {
            return rentRegion.getPricePerPeriod();
            }
            return cell.getPricePerPeriod(); 
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting cell price: " + e.getMessage());
            return 0.0;
        }
    }
    
    
    public boolean addMember(Region cell, Player player, String targetName) {
        
        if (cell == null || player == null || targetName == null || targetName.trim().isEmpty()) {
            if (player != null) {
                MessageUtils.sendError(player, "Invalid parameters for adding member.");
            }
            return false;
        }
        
        
        if (!security.isValidUsername(targetName)) {
            MessageUtils.sendError(player, "Invalid username format.");
            return false;
        }
        
        if (security.isRateLimited(player, "member_add")) {
            MessageUtils.sendError(player, "You're adding members too quickly! Please wait.");
            return false;
        }
        
        try {
            
            if (!isOwner(player, cell)) {
                MessageUtils.sendError(player, "You don't own this cell!");
                return false;
            }
            
            
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName.trim());
            if (target == null) {
                MessageUtils.sendError(player, "Player '" + targetName + "' not found!");
                return false;
            }
            
            
            if (cell.getRegion().hasMember(target.getUniqueId())) {
                MessageUtils.sendError(player, targetName + " is already a member!");
            return false;
        }
        
            
            if (cell.getRegion().hasOwner(target.getUniqueId())) {
                MessageUtils.sendError(player, "Cannot add the owner as a member!");
            return false;
        }
        
            
            int maxMembers = cell.getMaxMembers();
            if (maxMembers >= 0 && cell.getRegion().getMembers().size() >= maxMembers) {
                MessageUtils.sendError(player, "Member limit of " + maxMembers + " reached!");
                return false;
            }
            
            
            double cost = plugin.getMemberAddCost();
            if (cost > 0.0 && economy != null) {
                if (!economy.has(player, cost)) {
                    MessageUtils.sendError(player, "Insufficient funds! Cost: " + plugin.formatCurrency(cost));
                    return false;
                }
            }
            
            
            try {
                cell.getRegion().addMember(target.getUniqueId());
                cell.queueSave();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add member " + targetName + " to cell " + cell.getRegion().getId() + ": " + e.getMessage());
                MessageUtils.sendError(player, "Failed to add member to the cell!");
                return false;
            }
            
            
            if (cost > 0.0 && economy != null) {
                if (!economy.withdrawPlayer(player, cost).transactionSuccess()) {
                    
                    try {
                        cell.getRegion().removeMember(target.getUniqueId());
                        cell.queueSave();
                    } catch (Exception rollbackException) {
                        plugin.getLogger().severe("Failed to rollback member addition after payment failure for " + 
                            targetName + " in cell " + cell.getRegion().getId() + ". Manual correction needed!");
                    }
                    MessageUtils.sendError(player, "Payment failed! Member addition has been cancelled.");
                    return false;
                }
                
                MessageUtils.sendInfo(player, "Charged " + plugin.formatCurrency(cost) + " for adding member.");
            }
            
            
            security.auditLog(player, "ADD_MEMBER", cell.getRegion().getId(), 
                "Added " + targetName + " as member");
            
            
            MessageUtils.sendSuccess(player, "Added " + targetName + " as a member!");
            
            if (target.isOnline()) {
                Player targetPlayer = (Player) target;
                MessageUtils.sendSuccess(targetPlayer, 
                    "You have been added as a member to cell " + cell.getRegion().getId() + " by " + player.getName());
            }
        
        return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error adding member: " + e.getMessage());
            MessageUtils.sendError(player, "Failed to add member due to an error.");
            return false;
        }
    }
    
    
    public boolean removeMember(Region cell, Player player, String targetName) {
        
        if (cell == null || player == null || targetName == null || targetName.trim().isEmpty()) {
            if (player != null) {
                MessageUtils.sendError(player, "Invalid parameters for removing member.");
            }
            return false;
        }
        
        
        if (!security.isValidUsername(targetName)) {
            MessageUtils.sendError(player, "Invalid username format.");
            return false;
        }
        
        try {
            
        if (!isOwner(player, cell)) {
            MessageUtils.sendError(player, "You don't own this cell!");
            return false;
        }
        
            
            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName.trim());
            if (target == null) {
                MessageUtils.sendError(player, "Player '" + targetName + "' not found!");
            return false;
        }
        
            
            if (!cell.getRegion().hasMember(target.getUniqueId())) {
                MessageUtils.sendError(player, targetName + " is not a member!");
                return false;
            }
            
            
            double cost = plugin.getMemberRemoveCost();
            if (cost > 0.0 && economy != null) {
                if (!economy.has(player, cost)) {
                    MessageUtils.sendError(player, "Insufficient funds! Cost: " + plugin.formatCurrency(cost));
                    return false;
                }
            }
            
            
            try {
                cell.getRegion().removeMember(target.getUniqueId());
                cell.queueSave();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove member " + targetName + " from cell " + cell.getRegion().getId() + ": " + e.getMessage());
                MessageUtils.sendError(player, "Failed to remove member from the cell!");
                return false;
            }
            
            
            if (cost > 0.0 && economy != null) {
                if (!economy.withdrawPlayer(player, cost).transactionSuccess()) {
                    
                    try {
                        cell.getRegion().addMember(target.getUniqueId());
                        cell.queueSave();
                    } catch (Exception rollbackException) {
                        plugin.getLogger().severe("Failed to rollback member removal after payment failure for " + 
                            targetName + " in cell " + cell.getRegion().getId() + ". Manual correction needed!");
                    }
                    MessageUtils.sendError(player, "Payment failed! Member removal has been cancelled.");
                    return false;
                }
                
                MessageUtils.sendInfo(player, "Charged " + plugin.formatCurrency(cost) + " for removing member.");
            }
            
            
            security.auditLog(player, "REMOVE_MEMBER", cell.getRegion().getId(), 
                "Removed " + targetName + " as member");
            
            
            MessageUtils.sendSuccess(player, "Removed " + targetName + " as a member!");
            
            if (target.isOnline()) {
                Player targetPlayer = (Player) target;
                MessageUtils.sendInfo(targetPlayer, 
                    "You have been removed as a member from cell " + cell.getRegion().getId() + " by " + player.getName());
            }
        
        return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing member: " + e.getMessage());
            MessageUtils.sendError(player, "Failed to remove member due to an error.");
            return false;
        }
    }
    
    
    public List<Region> getAvailableCells(World world) {
        if (world == null) {
            return Collections.emptyList();
        }
        
        try {
            List<Region> availableCells = new ArrayList<>();
            for (Region region : arm.getRegionManager()) {
                if (region != null && 
                    region.getRegion() != null && 
                    region.getRegionworld().equals(world) &&
                    !region.isSold()) {
                    availableCells.add(region);
                }
            }
            return availableCells;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting available cells: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    
    public Map<String, String> getCellInfo(Region cell) {
        Map<String, String> info = new HashMap<>();
        
        if (cell == null) {
            info.put("error", "Cell is null");
            return info;
        }
        
        try {
            
            info.put("id", cell.getRegion() != null ? cell.getRegion().getId() : "Unknown");
            info.put("world", cell.getRegionworld() != null ? 
                     cell.getRegionworld().getName() : "Unknown");
            info.put("sold", isSold(cell) ? "Occupied" : "Available");
            info.put("price", plugin.formatCurrency(getPrice(cell)));
            
            
            if (cell instanceof RentRegion) {
                info.put("type", "Rental");
            } else {
                info.put("type", "Purchase");
            }
            
            
            UUID ownerId = cell.getOwner();
            if (ownerId != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
                info.put("owner", owner.getName() != null ? owner.getName() : "Unknown");
            } else {
                info.put("owner", "Available");
            }
            
            
            var members = cell.getRegion().getMembers();
            if (members != null && !members.isEmpty()) {
                List<String> memberNames = members.stream()
                    .map(Bukkit::getOfflinePlayer)
                    .filter(Objects::nonNull)
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                
                info.put("members", String.join(", ", memberNames));
                info.put("memberCount", String.valueOf(memberNames.size()));
            } else {
                info.put("members", "None");
                info.put("memberCount", "0");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting cell info: " + e.getMessage());
            info.put("error", "Failed to get cell information");
        }
        
        return info;
    }
    
    
    public boolean purchaseCell(Player player, Region cell) {
        if (cell == null || player == null) {
            plugin.debug("purchaseCell: null cell or player");
            return false;
        }
        
        try {
            
            if (isSold(cell)) {
                MessageUtils.sendError(player, "This cell is already owned!");
                return false;
            }
            
            
            if (!canPlayerAcquireCell(player, cell)) {
                return false; 
            }
            
            double price = getPrice(cell);
            if (price < 0) {
                MessageUtils.sendError(player, "This cell is not for sale!");
                return false;
            }
            
            
            if (security.isRateLimited(player, "purchase")) {
                MessageUtils.sendError(player, "You're purchasing too quickly! Please wait.");
                return false;
            }
            
            
            
            boolean success = false;
            String errorMessage = null;
            
            try {
                
                cell.buy(player);
                success = true;
                
                String action = cell instanceof RentRegion ? "rented" : "purchased";
                MessageUtils.sendSuccess(player, "Successfully %s cell '%s' for %s!", 
                    action, cell.getRegion().getId(), plugin.formatCurrency(price));
                    
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null) {
                    if (message.toLowerCase().contains("money") || message.toLowerCase().contains("fund") || 
                        message.toLowerCase().contains("balance") || message.toLowerCase().contains("afford")) {
                        errorMessage = "Insufficient funds! You need " + plugin.formatCurrency(price);
                    } else {
                        errorMessage = "Failed to purchase cell: " + message;
                    }
                } else {
                    errorMessage = "Failed to purchase cell! Please try again.";
                }
                
                plugin.getLogger().warning("ARM purchase failed for " + player.getName() + 
                    " on cell " + cell.getRegion().getId() + ": " + e.getMessage());
                success = false;
            }
            
            if (success) {
                
                plugin.getDoorManager().syncDoorOwnershipForRegion(cell.getRegion().getId());
                
                
                security.auditLog(player, "CELL_PURCHASE", cell.getRegion().getId(), 
                    "Price: " + plugin.formatCurrency(price) + " (ARM handled payment)");
                
                plugin.debug("Player " + player.getName() + " successfully purchased cell " + 
                    cell.getRegion().getId() + " through ARM's economy system");
                
                return true;
            } else {
                MessageUtils.sendError(player, errorMessage != null ? errorMessage : 
                    "Failed to purchase cell! Please try again.");
                return false;
            }
            
        } catch (Exception e) {
            MessageUtils.sendError(player, "An error occurred while purchasing the cell!");
            plugin.getLogger().warning("Error in purchaseCell for " + player.getName() + 
                " and cell " + cell.getRegion().getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean canPlayerAcquireCell(Player player, Region cell) {
        CellGroupManager groupManager = plugin.getCellGroupManager();
        String regionId = cell.getRegion().getId();
        
        
        java.util.List<CellGroup> groups = groupManager.getGroupsByRegion(regionId);
        if (groups.isEmpty()) {
            return true; 
        }
        
        for (CellGroup group : groups) {
            if (groupManager.canPlayerAcquireInGroup(player, group)) {
                return true; 
            }
        }
        
        
        CellGroup primary = groupManager.getGroupByRegion(regionId);
        if (primary != null) {
            String permission = primary.getRequiredPermission();
            if (permission != null && !player.hasPermission(permission)) {
                MessageUtils.sendError(player, "You don't have permission to acquire cells in this group!");
                return false;
            }
            int groupLimit = primary.getCellLimit();
            if (groupLimit > 0) {
                int currentCount = groupManager.getPlayerCellCountInGroup(player.getUniqueId(), primary);
                if (currentCount >= groupLimit) {
                    MessageUtils.sendError(player, "You've reached the limit of %d cells in the %s group!", 
                        groupLimit, primary.getDisplayName());
                    return false;
                }
            }
        }
        
        int globalLimit = groupManager.getGlobalCellLimit();
        if (globalLimit > 0) {
            int totalCount = groupManager.getPlayerTotalCellCount(player.getUniqueId());
            if (totalCount >= globalLimit) {
                MessageUtils.sendError(player, "You've reached the global limit of %d cells!", globalLimit);
                return false;
            }
        }
        
        return false;
    }

    
    public boolean sellCell(Player player, Region cell) {
        if (cell == null || player == null) {
            plugin.debug("sellCell: null cell or player");
            return false;
        }
        
        try {
            
            if (!isOwner(player, cell)) {
                MessageUtils.sendError(player, "You don't own this cell!");
                return false;
            }
            
            
            try {
                
                cell.unsell(Region.ActionReason.MANUALLY_BY_ADMIN, true, false);
                
                String action = cell instanceof RentRegion ? "cancelled rental for" : "sold";
                MessageUtils.sendSuccess(player, "Successfully %s cell '%s'!", 
                    action, cell.getRegion().getId());
                    
                
                plugin.getDoorManager().syncDoorOwnershipForRegion(cell.getRegion().getId());
                
                
                String auditAction = cell instanceof RentRegion ? "CELL_RENTAL_CANCEL" : "CELL_SELL";
                String details = cell instanceof RentRegion ? "Rental cancelled" : "Sold back to market";
                security.auditLog(player, auditAction, cell.getRegion().getId(), details);
                
                plugin.debug("Player " + player.getName() + " " + action + " cell " + 
                    cell.getRegion().getId());
                
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().warning("ARM unsell failed: " + e.getMessage());
                MessageUtils.sendError(player, "Failed to sell/cancel cell!");
                return false;
            }
            
        } catch (Exception e) {
            MessageUtils.sendError(player, "An error occurred while selling the cell!");
            plugin.getLogger().warning("Error in sellCell for " + player.getName() + 
                " and cell " + cell.getRegion().getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    
    public boolean extendRental(Player player, Region cell, int periods) {
        if (player == null || cell == null || periods <= 0) {
            if (player != null) {
                MessageUtils.sendError(player, "Invalid extension parameters.");
            }
            return false;
        }
        
        if (!(cell instanceof RentRegion rentRegion)) {
            MessageUtils.sendError(player, "This cell is not a rental!");
            return false;
        }
        
        try {
            
            if (!isOwner(player, cell)) {
                MessageUtils.sendError(player, "You don't own this rental!");
                return false;
            }
            
            
            if (security.isRateLimited(player, "purchase")) {
                MessageUtils.sendError(player, "You're extending too quickly! Please wait.");
                return false;
            }
            
            
            double balanceBefore = economy != null ? economy.getBalance(player) : 0.0;
            
            
            boolean extensionSuccess = false;
            String errorMessage = null;
            int successfulExtensions = 0;
            
            try {
                
                
                for (int i = 0; i < periods; i++) {
                    try {
                        rentRegion.extend(player);
                        successfulExtensions++;
                        plugin.debug("Extended rental period " + (i + 1) + "/" + periods + " for " + player.getName());
                    } catch (Exception periodException) {
                        
                        String msg = periodException.getMessage();
                        if (msg != null && (msg.toLowerCase().contains("max") || 
                                          msg.toLowerCase().contains("limit") ||
                                          msg.toLowerCase().contains("duration") ||
                                          msg.toLowerCase().contains("cannot extend"))) {
                            if (successfulExtensions > 0) {
                                
                                extensionSuccess = true;
                                plugin.debug("Hit extension limit after " + successfulExtensions + " periods - this is expected behavior");
                            } else {
                                errorMessage = "Rental is already at maximum duration and cannot be extended further.";
                            }
                            break;
                        } else if (msg != null && (msg.toLowerCase().contains("money") || 
                                                 msg.toLowerCase().contains("fund") ||
                                                 msg.toLowerCase().contains("balance") || 
                                                 msg.toLowerCase().contains("afford"))) {
                            
                            if (successfulExtensions > 0) {
                                extensionSuccess = true;
                                errorMessage = "Extended " + successfulExtensions + " period(s) before running out of funds.";
                            } else {
                                errorMessage = "Insufficient funds for rental extension!";
                            }
                            break;
                        } else {
                            
                            throw periodException;
                        }
                    }
                }
                
                if (successfulExtensions == periods) {
                    extensionSuccess = true;
                }
                
            } catch (Exception e) {
                String message = e.getMessage();
                if (message != null) {
                    if (message.toLowerCase().contains("money") || message.toLowerCase().contains("fund") || 
                        message.toLowerCase().contains("balance") || message.toLowerCase().contains("afford")) {
                        errorMessage = "Insufficient funds for rental extension!";
                    } else if (message.toLowerCase().contains("max") || message.toLowerCase().contains("limit") ||
                              message.toLowerCase().contains("duration")) {
                        errorMessage = "Rental is already at maximum duration!";
                    } else {
                        errorMessage = "Failed to extend rental: " + message;
                    }
                } else {
                    errorMessage = "Failed to extend rental. Please try again.";
                }
                
                plugin.getLogger().warning("ARM rental extension failed for " + player.getName() + 
                    " on cell " + cell.getRegion().getId() + ": " + e.getMessage());
            }
            
            if (extensionSuccess && successfulExtensions > 0) {
                
                double balanceAfter = economy != null ? economy.getBalance(player) : 0.0;
                double actualCostPaid = balanceBefore - balanceAfter;
                
                
                security.auditLog(player, "EXTEND_RENTAL", cell.getRegion().getId(), 
                    "Periods: " + successfulExtensions + ", Total Cost: " + plugin.formatCurrency(actualCostPaid) + 
                    " (ARM handled payment with prorated pricing)");
                
                
                String successMsg;
                String periodsText = successfulExtensions == 1 ? "1 period" : successfulExtensions + " periods";
                
                if (actualCostPaid > 0.0) {
                    successMsg = String.format("Successfully extended rental for %s! Total paid: %s", 
                        periodsText, plugin.formatCurrency(actualCostPaid));
                } else {
                    
                    successMsg = String.format("Successfully extended rental for %s! (Free extension)", periodsText);
                }
                
                
                if (successfulExtensions < periods) {
                    successMsg += String.format(" (Extended %d of %d requested periods - reached maximum duration)", 
                        successfulExtensions, periods);
                }
                
                MessageUtils.sendSuccess(player, successMsg);
                
                
                try {
                    long newPayedTill = rentRegion.getPayedTill();
                    long newTimeRemaining = newPayedTill - System.currentTimeMillis();
                    MessageUtils.sendInfo(player, "Rental now expires: " + formatTimeLeft(newTimeRemaining));
                } catch (Exception e) {
                    plugin.debug("Could not get new expiration time: " + e.getMessage());
                }
                
                return true;
            } else {
                MessageUtils.sendError(player, errorMessage != null ? errorMessage : 
                    "Failed to extend rental! Please try again.");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error extending rental: " + e.getMessage());
            MessageUtils.sendError(player, "Failed to extend rental due to an error.");
            return false;
        }
    }
    
    
    public int getMaxReasonableExtensions(Region cell, Player player) {
        if (!(cell instanceof RentRegion rentRegion) || player == null) {
            return 0;
        }
        
        try {
            
            int maxExtensionsAllowed = plugin.getConfig().getInt("teleportation.rental.max-gui-extensions", 25);
            
            
            int actualMaxExtensions = testActualExtensionLimit(rentRegion, player, maxExtensionsAllowed);
            
            
            int finalMax = Math.min(maxExtensionsAllowed, actualMaxExtensions);
            
            plugin.debug("Max extensions for cell " + cell.getRegion().getId() + 
                        ": config=" + maxExtensionsAllowed + ", ARM=" + actualMaxExtensions + 
                        ", final=" + finalMax);
            
            return finalMax;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating reasonable extensions: " + e.getMessage());
            return 5; 
        }
    }
    
    
    private int testActualExtensionLimit(RentRegion rentRegion, Player player, int maxTest) {
        try {
            
            long currentPayedTill = rentRegion.getPayedTill();
            long now = System.currentTimeMillis();
            long currentTimeRemaining = currentPayedTill - now;
            
            
            if (currentTimeRemaining <= 0) {
                return Math.min(maxTest, 25); 
            }
            
            
            long currentDurationDays = currentTimeRemaining / (24 * 60 * 60 * 1000);
            
            
            
            
            
            if (currentDurationDays >= 60) {
                return Math.min(5, maxTest); 
            } else if (currentDurationDays >= 30) {
                return Math.min(10, maxTest); 
            } else if (currentDurationDays >= 14) {
                return Math.min(15, maxTest); 
            } else {
                return Math.min(maxTest, 25); 
            }
            
        } catch (Exception e) {
            plugin.debug("Error testing extension limit: " + e.getMessage());
            return Math.min(10, maxTest); 
        }
    }
    
    
    public List<Integer> getValidExtensionAmounts(Region cell, Player player) {
        List<Integer> validAmounts = new ArrayList<>();
        
        if (!(cell instanceof RentRegion)) {
            return validAmounts;
        }
        
        int maxReasonable = getMaxReasonableExtensions(cell, player);
        
        
        if (maxReasonable > 0) {
            validAmounts.add(1);
        }
        
        
        if (maxReasonable >= 5) {
            validAmounts.add(5);
        }
        if (maxReasonable >= 10) {
            validAmounts.add(10);
        }
        if (maxReasonable >= 25) {
            validAmounts.add(25);
        }
        
        
        if (maxReasonable > 1 && maxReasonable <= 25 && !validAmounts.contains(maxReasonable)) {
            validAmounts.add(maxReasonable);
            validAmounts.sort(Integer::compareTo); 
        }
        
        plugin.debug("Valid extension amounts for cell " + cell.getRegion().getId() + 
                    ": " + validAmounts + " (max reasonable: " + maxReasonable + ")");
        
        return validAmounts;
    }
    
    
    public Map<String, String> getRentalInfo(Region cell) {
        Map<String, String> info = new HashMap<>();
        
        if (!(cell instanceof RentRegion rentRegion)) {
            info.put("error", "Not a rental cell");
            return info;
        }
        
        try {
            info.put("periodPrice", plugin.formatCurrency(rentRegion.getPricePerPeriod()));
            
            
            try {
                long payedTillTimestamp = rentRegion.getPayedTill();
                long remainingTime = payedTillTimestamp - System.currentTimeMillis();
                info.put("timeLeft", formatTimeLeft(remainingTime));
            } catch (Exception e) {
                plugin.getLogger().warning("Error getting remaining rental time: " + e.getMessage());
                info.put("timeLeft", "Unknown");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting rental info: " + e.getMessage());
            info.put("error", "Failed to get rental information");
        }
        
        return info;
    }
    
    
    private String formatTimeLeft(long milliseconds) {
        if (milliseconds <= 0) {
            return "Expired";
        }
        
        try {
            Duration duration = Duration.ofMillis(milliseconds);
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
        
        if (days > 0) {
                return String.format("%dd %dh %dm", days, hours, minutes);
            } else if (hours > 0) {
                return String.format("%dh %dm", hours, minutes);
        } else {
                return String.format("%dm", minutes);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting time: " + e.getMessage());
            return "Unknown";
        }
    }
} 
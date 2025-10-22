package dev.lsdmc.edencells.gui;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.security.SecurityManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.regions.Region;
import net.alex9849.arm.regions.RentRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class CellGUI {
    
    private final EdenCells plugin;
    private final CellManager cellManager;
    private final SecurityManager security;
    
    
    public static final Map<UUID, GUISession> openGUIs = new ConcurrentHashMap<>();
    
    
    private static final long SESSION_TIMEOUT = 300000; 
    
    public record GUISession(
        Inventory inventory,
        String type,
        Object data,
        long openTime
    ) {}
    
    public CellGUI(EdenCells plugin, CellManager cellManager, SecurityManager security) {
        this.plugin = plugin;
        this.cellManager = cellManager;
        this.security = security;
        
        
        if (plugin != null) {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
                CellGUI::cleanupSessions, 200L, 1200L); 
        }
    }
    
    
    public void openCellGUI(Player player, Region cell) {
        if (player == null || cell == null) {
            plugin.getLogger().warning("Attempted to open GUI with null player or cell");
            return;
        }
        
        if (!player.isOnline()) {
            plugin.getLogger().warning("Attempted to open GUI for offline player: " + player.getName());
            return;
        }
        
        if (!cellManager.isSold(cell)) {
            
            openPurchaseGUI(player, cell);
        } else if (cellManager.isOwner(player, cell)) {
            
            openManagementGUI(player, cell);
        } else {
            
            openViewerGUI(player, cell);
        }
    }
    
    
    private void openPurchaseGUI(Player player, Region cell) {
        if (player == null || cell == null) return;
        
        Map<String, String> info = cellManager.getCellInfo(cell);
        String cellId = info.get("id");
        
        Inventory gui = Bukkit.createInventory(null, 45, MessageUtils.fromMiniMessage(
            "<color:#9D4EDD>Purchase Cell: <color:#FFB3C6>" + cellId + "</color></color>"));
        
        
        ItemStack infoItem = createCellInfoItem(info, Material.EMERALD_BLOCK);
        gui.setItem(13, infoItem);
        
        
        double price = cellManager.getPrice(cell);
        boolean canAfford = plugin.getEconomy() != null && plugin.getEconomy().has(player, price);
        
        Material buttonMaterial = canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        ItemStack purchaseButton = new ItemStack(buttonMaterial);
        ItemMeta purchaseMeta = purchaseButton.getItemMeta();
        if (purchaseMeta == null) return; 
        
        String actionText = cell instanceof RentRegion ? "Rent Cell" : "Purchase Cell";
        if (!canAfford) {
            actionText = "Cannot Afford";
        }
        purchaseMeta.displayName(Component.text(actionText, 
            canAfford ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD));
        
        List<Component> purchaseLore = new ArrayList<>();
        purchaseLore.add(Component.text("Price: " + info.get("price"), NamedTextColor.AQUA));
        purchaseLore.add(Component.text("Type: " + info.get("type"), NamedTextColor.DARK_AQUA));
        purchaseLore.add(Component.empty());
        
        if (canAfford) {
            purchaseLore.add(Component.text("Click to " + (cell instanceof RentRegion ? "rent" : "purchase") + "!", 
                NamedTextColor.YELLOW));
        } else {
            purchaseLore.add(Component.text("⚠ Insufficient Funds", NamedTextColor.RED, TextDecoration.BOLD));
        }
        
        purchaseMeta.lore(purchaseLore);
        purchaseButton.setItemMeta(purchaseMeta);
        gui.setItem(22, purchaseButton);
        
        
        if (plugin.getEconomy() != null) {
            ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta balanceMeta = balanceItem.getItemMeta();
            if (balanceMeta != null) {
                balanceMeta.displayName(Component.text("Your Balance", NamedTextColor.GOLD, TextDecoration.BOLD));
                balanceMeta.lore(List.of(
                    Component.text("$" + String.format("%.2f", plugin.getEconomy().getBalance(player)), 
                        canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)
                ));
                balanceItem.setItemMeta(balanceMeta);
                gui.setItem(31, balanceItem);
            }
        }
        
        addCloseButton(gui, 40);
        fillBorders(gui, Material.GRAY_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), new GUISession(gui, "purchase", cell, System.currentTimeMillis()));
    }
    
    
    private void openManagementGUI(Player player, Region cell) {
        if (player == null || cell == null) return;
        
        Map<String, String> info = cellManager.getCellInfo(cell);
        String cellId = info.get("id");
        
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.fromMiniMessage(
            "<color:#9D4EDD>Manage Cell: <color:#FFB3C6>" + cellId + "</color></color>"));
        
        
        ItemStack infoItem = createCellInfoItem(info, Material.DIAMOND_BLOCK);
        gui.setItem(13, infoItem);
        
        
        if (cell instanceof RentRegion) {
            Map<String, String> rentalInfo = cellManager.getRentalInfo(cell);
            ItemStack rentalItem = new ItemStack(Material.CLOCK);
            ItemMeta rentalMeta = rentalItem.getItemMeta();
            if (rentalMeta != null) {
                rentalMeta.displayName(Component.text("Rental Information", NamedTextColor.AQUA, TextDecoration.BOLD));
                
                List<Component> rentalLore = new ArrayList<>();
                rentalLore.add(Component.text("Time Left: " + rentalInfo.getOrDefault("timeLeft", "Unknown"), NamedTextColor.YELLOW));
                rentalLore.add(Component.text("Period Price: " + rentalInfo.getOrDefault("periodPrice", "Unknown"), NamedTextColor.GREEN));
                
                rentalMeta.lore(rentalLore);
                rentalItem.setItemMeta(rentalMeta);
                gui.setItem(11, rentalItem);
                
                
                ItemStack extendButton = new ItemStack(Material.EMERALD);
                ItemMeta extendMeta = extendButton.getItemMeta();
                if (extendMeta != null) {
                    extendMeta.displayName(Component.text("Extend Rental", NamedTextColor.GREEN, TextDecoration.BOLD));
                    extendMeta.lore(List.of(
                        Component.text("Extend your rental period", NamedTextColor.GRAY),
                        Component.text("Click to choose extension amount", NamedTextColor.YELLOW)
                    ));
                    extendButton.setItemMeta(extendMeta);
                    gui.setItem(20, extendButton);
                }
            }
        }
        
        
        ItemStack memberButton = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta memberMeta = memberButton.getItemMeta();
        if (memberMeta != null) {
            memberMeta.displayName(Component.text("View Members", NamedTextColor.BLUE, TextDecoration.BOLD));
            
            String memberCount = info.getOrDefault("memberCount", "0");
            List<Component> memberLore = new ArrayList<>();
            memberLore.add(Component.text("Current Members: " + memberCount, NamedTextColor.GRAY));
            memberLore.add(Component.empty());
            memberLore.add(Component.text("Click to view and manage members", NamedTextColor.YELLOW));
            
            memberMeta.lore(memberLore);
            memberButton.setItemMeta(memberMeta);
            gui.setItem(15, memberButton);
        }
        
        
        ItemStack sellButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta sellMeta = sellButton.getItemMeta();
        if (sellMeta != null) {
            String sellText = cell instanceof RentRegion ? "Unrent Cell" : "Sell Cell";
            sellMeta.displayName(Component.text(sellText, NamedTextColor.RED, TextDecoration.BOLD));
            sellMeta.lore(List.of(
                Component.text("⚠ This action cannot be undone!", NamedTextColor.RED),
                Component.text("You will lose access to this cell", NamedTextColor.GRAY),
                Component.text("Use Cell NPCs to teleport to your cells", NamedTextColor.GOLD)
            ));
            sellButton.setItemMeta(sellMeta);
            gui.setItem(24, sellButton);
        }
        
        addCloseButton(gui, 49);
        fillBorders(gui, Material.BLUE_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), new GUISession(gui, "management", cell, System.currentTimeMillis()));
    }
    
    
    private void openViewerGUI(Player player, Region cell) {
        if (player == null || cell == null) return;
        
        Map<String, String> info = cellManager.getCellInfo(cell);
        String cellId = info.get("id");
        
        Inventory gui = Bukkit.createInventory(null, 36, MessageUtils.fromMiniMessage(
            "<color:#9D4EDD>Cell Info: <color:#FFB3C6>" + cellId + "</color></color>"));
        
        
        ItemStack infoItem = createCellInfoItem(info, Material.IRON_BLOCK);
        gui.setItem(13, infoItem);
        
        
        String ownerName = info.get("owner");
        if (ownerName != null && !ownerName.equals("Unknown") && !ownerName.equals("Available")) {
            ItemStack ownerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) ownerHead.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
                skullMeta.displayName(Component.text("Owner: " + ownerName, NamedTextColor.GOLD, TextDecoration.BOLD));
                ownerHead.setItemMeta(skullMeta);
                gui.setItem(11, ownerHead);
            }
        }
        
        
        String members = info.get("members");
        if (members != null && !members.isEmpty() && !members.equals("None")) {
            ItemStack memberBook = new ItemStack(Material.BOOK);
            ItemMeta bookMeta = memberBook.getItemMeta();
            if (bookMeta != null) {
                bookMeta.displayName(Component.text("Members", NamedTextColor.AQUA, TextDecoration.BOLD));
                
                List<Component> memberLore = new ArrayList<>();
                for (String member : members.split(", ")) {
                    if (member != null && !member.trim().isEmpty()) {
                        memberLore.add(Component.text("• " + member.trim(), NamedTextColor.WHITE));
                    }
                }
                
                bookMeta.lore(memberLore);
                memberBook.setItemMeta(bookMeta);
                gui.setItem(15, memberBook);
            }
        }
        
        addCloseButton(gui, 31);
        fillBorders(gui, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), new GUISession(gui, "viewer", cell, System.currentTimeMillis()));
    }
    
    
    private ItemStack createCellInfoItem(Map<String, String> info, Material material) {
        if (info == null) {
            plugin.getLogger().warning("Attempted to create cell info item with null info");
            return new ItemStack(Material.BARRIER);
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item; 
        
        meta.displayName(Component.text("Cell: " + info.getOrDefault("id", "Unknown"), 
            NamedTextColor.GOLD, TextDecoration.BOLD));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("World: " + info.getOrDefault("world", "Unknown"), NamedTextColor.GRAY));
        lore.add(Component.text("Type: " + info.getOrDefault("type", "Unknown"), NamedTextColor.DARK_AQUA));
        lore.add(Component.text("Price: " + info.getOrDefault("price", "Unknown"), NamedTextColor.AQUA));
        lore.add(Component.text("Status: " + info.getOrDefault("sold", "Unknown"), NamedTextColor.YELLOW));
        
        String owner = info.get("owner");
        if (owner != null && !owner.equals("Available") && !owner.equals("Unknown")) {
            lore.add(Component.text("Owner: " + owner, NamedTextColor.GREEN));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    
    private void addCloseButton(Inventory gui, int slot) {
        if (gui == null) return;
        
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.displayName(Component.text("Close", NamedTextColor.RED, TextDecoration.BOLD));
            closeButton.setItemMeta(closeMeta);
            gui.setItem(slot, closeButton);
        }
    }
    
    
    private void fillBorders(Inventory gui, Material material) {
        if (gui == null) return;
        
        ItemStack filler = new ItemStack(material);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
            
            int size = gui.getSize();
            
            
            for (int i = 0; i < 9; i++) {
                if (gui.getItem(i) == null) gui.setItem(i, filler);
                if (gui.getItem(size - 9 + i) == null) gui.setItem(size - 9 + i, filler);
            }
            
            
            for (int i = 1; i < (size / 9) - 1; i++) {
                if (gui.getItem(i * 9) == null) gui.setItem(i * 9, filler);
                if (gui.getItem(i * 9 + 8) == null) gui.setItem(i * 9 + 8, filler);
            }
        }
    }
    
    
    public static GUISession getSession(Player player) {
        if (player == null) return null;
        return openGUIs.get(player.getUniqueId());
    }
    
    
    public static void closeSession(Player player) {
        if (player == null) return;
        openGUIs.remove(player.getUniqueId());
    }
    
    
    public static void cleanupSessions() {
        long now = System.currentTimeMillis();
        openGUIs.entrySet().removeIf(entry -> {
            if (entry == null || entry.getValue() == null) {
                return true;
            }
            
            
            if (now - entry.getValue().openTime() > SESSION_TIMEOUT) {
                return true;
            }
            
            
            var player = Bukkit.getPlayer(entry.getKey());
            return player == null || !player.isOnline();
        });
    }
    
    
    public void openPeriodSelectionGUI(Player player, Region cell) {
        if (player == null || cell == null) return;
        
        if (!(cell instanceof RentRegion)) {
            MessageUtils.sendError(player, "This cell is not a rental!");
            return;
        }
        
        Map<String, String> info = cellManager.getCellInfo(cell);
        String cellId = info.get("id");
        
        Inventory gui = Bukkit.createInventory(null, 36, MessageUtils.fromMiniMessage(
            "<color:#9D4EDD>Extend Rental: <color:#FFB3C6>" + cellId + "</color></color>"));
        
        
        ItemStack infoItem = createCellInfoItem(info, Material.CLOCK);
        gui.setItem(4, infoItem);
        
        
        List<Integer> validAmounts = cellManager.getValidExtensionAmounts(cell, player);
        createDynamicPeriodButtons(gui, validAmounts, cell);
        
        
        Map<String, String> rentalInfo = cellManager.getRentalInfo(cell);
        ItemStack currentInfo = new ItemStack(Material.PAPER);
        ItemMeta currentMeta = currentInfo.getItemMeta();
        if (currentMeta != null) {
            currentMeta.displayName(Component.text("Current Rental Info", NamedTextColor.AQUA, TextDecoration.BOLD));
            
            List<Component> currentLore = new ArrayList<>();
            currentLore.add(Component.text("Time Left: " + rentalInfo.getOrDefault("timeLeft", "Unknown"), NamedTextColor.YELLOW));
            currentLore.add(Component.text("Period Price: " + rentalInfo.getOrDefault("periodPrice", "Unknown"), NamedTextColor.GREEN));
            currentLore.add(Component.empty());
            currentLore.add(Component.text("Select periods to extend above", NamedTextColor.GRAY));
            
            currentMeta.lore(currentLore);
            currentInfo.setItemMeta(currentMeta);
            gui.setItem(22, currentInfo);
        }
        
        
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back", NamedTextColor.YELLOW, TextDecoration.BOLD));
            backMeta.lore(List.of(Component.text("Return to cell management", NamedTextColor.GRAY)));
            backButton.setItemMeta(backMeta);
            gui.setItem(27, backButton);
        }
        
        addCloseButton(gui, 31);
        fillBorders(gui, Material.BLUE_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), new GUISession(gui, "period_selection", cell, System.currentTimeMillis()));
    }
    
    
    private void createDynamicPeriodButtons(Inventory gui, List<Integer> validAmounts, Region cell) {
        if (validAmounts.isEmpty()) {
            
            ItemStack noExtensionItem = new ItemStack(Material.BARRIER);
            ItemMeta noExtensionMeta = noExtensionItem.getItemMeta();
            if (noExtensionMeta != null) {
                noExtensionMeta.displayName(Component.text("No Extensions Available", NamedTextColor.RED, TextDecoration.BOLD));
                noExtensionMeta.lore(List.of(
                    Component.text("This rental cannot be extended further", NamedTextColor.GRAY),
                    Component.text("It may already be at maximum duration", NamedTextColor.DARK_GRAY)
                ));
                noExtensionItem.setItemMeta(noExtensionMeta);
                gui.setItem(13, noExtensionItem);
            }
            return;
        }
        
        
        int[] slots = calculateButtonSlots(validAmounts.size());
        Material[] materials = {Material.LIME_CONCRETE, Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE};
        
        for (int i = 0; i < validAmounts.size() && i < slots.length; i++) {
            int periods = validAmounts.get(i);
            String displayText = periods == 1 ? "1 Period" : periods + " Periods";
            Material material = materials[Math.min(i, materials.length - 1)];
            
            createPeriodButton(gui, slots[i], periods, displayText, material, cell);
        }
    }
    
    
    private int[] calculateButtonSlots(int buttonCount) {
        switch (buttonCount) {
            case 1 -> {
                return new int[]{13}; 
            }
            case 2 -> {
                return new int[]{11, 15}; 
            }
            case 3 -> {
                return new int[]{10, 13, 16}; 
            }
            case 4 -> {
                return new int[]{10, 12, 14, 16}; 
            }
            default -> {
                return new int[]{10, 12, 14, 16}; 
            }
        }
    }
    
    
    private void createPeriodButton(Inventory gui, int slot, int periods, String displayText, Material material, Region cell) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(displayText, NamedTextColor.WHITE, TextDecoration.BOLD));
            
            List<Component> lore = new ArrayList<>();
            
            
            String periodsText = periods == 1 ? "1 period" : periods + " periods";
            lore.add(Component.text("Extend rental by " + periodsText, NamedTextColor.GRAY));
            
            
            if (cell instanceof RentRegion rentRegion) {
                try {
                    double periodPrice = rentRegion.getPricePerPeriod();
                    double estimatedTotal = periodPrice * periods;
                    
                    lore.add(Component.empty());
                    if (estimatedTotal > 0) {
                        lore.add(Component.text("Estimated cost: " + plugin.formatCurrency(estimatedTotal), NamedTextColor.AQUA));
                        lore.add(Component.text("(Actual cost may vary due to prorated pricing)", NamedTextColor.DARK_GRAY));
                    } else {
                        lore.add(Component.text("Free extension", NamedTextColor.GREEN));
                    }
                } catch (Exception e) {
                    lore.add(Component.text("Cost: Varies (ARM pricing)", NamedTextColor.GRAY));
                }
            }
            
            lore.add(Component.empty());
            lore.add(Component.text("Click to extend", NamedTextColor.YELLOW));
            lore.add(Component.text("Note: You'll only be charged for", NamedTextColor.DARK_GRAY));
            lore.add(Component.text("periods you can actually extend", NamedTextColor.DARK_GRAY));
            
            meta.lore(lore);
            button.setItemMeta(meta);
            gui.setItem(slot, button);
        }
    }
    
    
    public void openMembersGUI(Player player, Region cell) {
        if (player == null || cell == null) return;
        
        
        if (!cellManager.isOwner(player, cell)) {
            MessageUtils.sendError(player, "You don't own this cell!");
            return;
        }
        
        Map<String, String> info = cellManager.getCellInfo(cell);
        String cellId = info.get("id");
        
        Inventory gui = Bukkit.createInventory(null, 54, MessageUtils.fromMiniMessage(
            "<color:#9D4EDD>Members: <color:#FFB3C6>" + cellId + "</color></color>"));
        
        
        ItemStack infoItem = createCellInfoItem(info, Material.EMERALD_BLOCK);
        gui.setItem(4, infoItem);
        
        
        var members = cell.getRegion().getMembers();
        List<OfflinePlayer> memberPlayers = new ArrayList<>();
        if (members != null && !members.isEmpty()) {
            for (UUID memberId : members) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
                if (member != null) {
                    memberPlayers.add(member);
                }
            }
        }
        
        
        int slot = 9;
        for (OfflinePlayer member : memberPlayers) {
            if (slot >= 45) break; 
            
            ItemStack memberHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) memberHead.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(member);
                String memberName = member.getName() != null ? member.getName() : "Unknown";
                skullMeta.displayName(Component.text(memberName, NamedTextColor.GREEN, TextDecoration.BOLD));
                
                List<Component> memberLore = new ArrayList<>();
                memberLore.add(Component.text("Member of this cell", NamedTextColor.GRAY));
                memberLore.add(Component.empty());
                if (member.isOnline()) {
                    memberLore.add(Component.text("● Online", NamedTextColor.GREEN));
                } else {
                    memberLore.add(Component.text("● Offline", NamedTextColor.RED));
                }
                memberLore.add(Component.empty());
                memberLore.add(Component.text("Right-click to remove member", NamedTextColor.RED));
                
                skullMeta.lore(memberLore);
                memberHead.setItemMeta(skullMeta);
                gui.setItem(slot, memberHead);
            }
            slot++;
        }
        
        
        if (memberPlayers.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = emptyItem.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.displayName(Component.text("No Members", NamedTextColor.RED, TextDecoration.BOLD));
                emptyMeta.lore(List.of(
                    Component.text("This cell has no members yet", NamedTextColor.GRAY),
                    Component.text("Use the Add Member button below", NamedTextColor.DARK_GRAY)
                ));
                emptyItem.setItemMeta(emptyMeta);
                gui.setItem(22, emptyItem);
            }
        }
        
        
        ItemStack addButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta addMeta = addButton.getItemMeta();
        if (addMeta != null) {
            addMeta.displayName(Component.text("Add Member", NamedTextColor.GREEN, TextDecoration.BOLD));
            addMeta.lore(List.of(
                Component.text("Click to add a new member", NamedTextColor.GRAY),
                Component.text("Cost: " + plugin.formatCurrency(plugin.getMemberAddCost()), NamedTextColor.GOLD)
            ));
            addButton.setItemMeta(addMeta);
            gui.setItem(45, addButton);
        }
        
        
        ItemStack limitItem = new ItemStack(Material.PAPER);
        ItemMeta limitMeta = limitItem.getItemMeta();
        if (limitMeta != null) {
            int maxMembers = cell.getMaxMembers();
            int currentMembers = memberPlayers.size();
            
            limitMeta.displayName(Component.text("Member Limit", NamedTextColor.AQUA, TextDecoration.BOLD));
            List<Component> limitLore = new ArrayList<>();
            limitLore.add(Component.text("Current: " + currentMembers, NamedTextColor.WHITE));
            if (maxMembers >= 0) {
                limitLore.add(Component.text("Maximum: " + maxMembers, NamedTextColor.WHITE));
                if (currentMembers >= maxMembers) {
                    limitLore.add(Component.text("⚠ Limit reached!", NamedTextColor.RED));
                } else {
                    limitLore.add(Component.text("Available slots: " + (maxMembers - currentMembers), NamedTextColor.GREEN));
                }
            } else {
                limitLore.add(Component.text("Maximum: Unlimited", NamedTextColor.WHITE));
            }
            
            limitMeta.lore(limitLore);
            limitItem.setItemMeta(limitMeta);
            gui.setItem(49, limitItem);
        }
        
        
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back", NamedTextColor.YELLOW, TextDecoration.BOLD));
            backMeta.lore(List.of(Component.text("Return to cell management", NamedTextColor.GRAY)));
            backButton.setItemMeta(backMeta);
            gui.setItem(46, backButton);
        }
        
        addCloseButton(gui, 53);
        fillBorders(gui, Material.BLUE_STAINED_GLASS_PANE);
        
        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), new GUISession(gui, "members", cell, System.currentTimeMillis()));
    }
    
    
    public void openVacantCellGUI(Player player, Region cell) {
        openCellGUI(player, cell);
    }
    
    public void openOccupiedCellGUI(Player player, Region cell) {
        openCellGUI(player, cell);
    }
} 
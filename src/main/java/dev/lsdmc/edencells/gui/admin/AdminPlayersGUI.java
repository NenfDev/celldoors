package dev.lsdmc.edencells.gui.admin;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public final class AdminPlayersGUI {

    private final EdenCells plugin;

    public AdminPlayersGUI(EdenCells plugin) {
        this.plugin = plugin;
    }

    public void open(Player admin, int page) {
        
        Set<UUID> ownerIds = new LinkedHashSet<>();
        try {
            for (Region region : plugin.getARM().getRegionManager()) {
                if (region == null) continue;
                UUID owner = region.getOwner();
                if (owner != null) ownerIds.add(owner);
            }
        } catch (Exception ignored) {}

        List<OfflinePlayer> players = ownerIds.stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(Objects::nonNull)
                .filter(p -> p.getName() != null && !p.getName().isBlank())
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getName()).orElse("").toLowerCase()))
                .toList();

        int perPage = 36; 
        int totalPages = Math.max(1, (players.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(new AdminGuiHolder("players"), 54,
                MessageUtils.fromMiniMessage("<color:#9D4EDD>Players</color>"));

        
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        bm.displayName(Component.text("Back", NamedTextColor.RED));
        back.setItemMeta(bm);
        inv.setItem(46, back);

        int start = page * perPage;
        int end = Math.min(start + perPage, players.size());
        int gridSlot = 9;
        for (int i = start; i < end; i++) {
            OfflinePlayer p = players.get(i);
            inv.setItem(gridSlot++, playerHeadItem(p));
        }

        
        inv.setItem(45, navItem("Prev", page > 0));
        inv.setItem(49, pageInfo(page + 1, totalPages));
        inv.setItem(53, navItem("Next", page < totalPages - 1));

        admin.openInventory(inv);
    }

    private ItemStack playerHeadItem(OfflinePlayer player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        
        String rawName = Optional.ofNullable(player.getName()).orElse("");
        String displayName = rawName.isEmpty() ? "Unknown" : rawName;
        try {
            if (player.getUniqueId() != null) {
                PlayerProfile profile = org.bukkit.Bukkit.createProfile(player.getUniqueId());
                meta.setPlayerProfile(profile);
            } else if (!rawName.isEmpty() && rawName.length() <= 16) {
                PlayerProfile profile = org.bukkit.Bukkit.createProfile(null, rawName);
                meta.setPlayerProfile(profile);
            }
        } catch (Throwable ignored) {
            
        }

        meta.displayName(Component.text(displayName, NamedTextColor.GOLD));

        
        CellManager cm = plugin.getCellManager();
        List<Region> owned = cm.getPlayerCells(player);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Cells: " + owned.size(), NamedTextColor.YELLOW));
        if (!owned.isEmpty()) {
            String ids = owned.stream()
                    .limit(3)
                    .map(r -> r.getRegion().getId())
                    .collect(Collectors.joining(", "));
            lore.add(Component.text(ids + (owned.size() > 3 ? ", ..." : ""), NamedTextColor.GRAY));
        }
        meta.lore(lore);

        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "admin_player_uuid"), PersistentDataType.STRING, player.getUniqueId().toString());
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack navItem(String label, boolean enabled) {
        ItemStack paper = new ItemStack(enabled ? Material.ARROW : Material.GRAY_DYE);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text(label, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        paper.setItemMeta(meta);
        return paper;
    }

    private ItemStack pageInfo(int current, int total) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text("Page " + current + "/" + total, NamedTextColor.AQUA));
        paper.setItemMeta(meta);
        return paper;
    }
}



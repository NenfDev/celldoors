package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.CellManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.alex9849.arm.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class AdminPlayerPrompt implements Listener {

    private static final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();
    private final EdenCells plugin;

    public AdminPlayerPrompt(EdenCells plugin) { this.plugin = plugin; }

    private enum Action { SET_OWNER, ADD_MEMBER, REMOVE_MEMBER }
    private record Prompt(Action action, UUID targetUuid, String regionId) {}

    public static void beginSetOwnerPrompt(Player admin, OfflinePlayer target) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.SET_OWNER, target.getUniqueId(), null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter region ID to set owner to </color><color:#FFB3C6>" + target.getName() + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginMemberAddPrompt(Player admin, OfflinePlayer target, String regionId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.ADD_MEMBER, target.getUniqueId(), regionId));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter player name to add as member</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginMemberRemovePrompt(Player admin, OfflinePlayer target, String regionId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.REMOVE_MEMBER, target.getUniqueId(), regionId));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter player name to remove from members</color> or <color:#FF6B6B>cancel</color>");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player admin = event.getPlayer();
        Prompt prompt = prompts.get(admin.getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) { prompts.remove(admin.getUniqueId()); MessageUtils.send(admin, "<color:#FF6B6B>Cancelled.</color>"); return; }
        Bukkit.getScheduler().runTask(plugin, () -> handle(admin, prompt, msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { prompts.remove(e.getPlayer().getUniqueId()); }

    private void handle(Player admin, Prompt prompt, String input) {
        try {
            OfflinePlayer target = plugin.getServer().getOfflinePlayer(prompt.targetUuid);
            if (target == null) { MessageUtils.sendError(admin, "Target not found."); return; }
            CellManager cm = plugin.getCellManager();
            switch (prompt.action) {
                case SET_OWNER -> {
                    Region region = plugin.findRegionById(input);
                    if (region == null) { MessageUtils.sendError(admin, "Region not found."); return; }
                    try {
                        
                        java.util.UUID newOwner = prompt.targetUuid;
                        
                        org.bukkit.OfflinePlayer offline = plugin.getServer().getOfflinePlayer(newOwner);
                        region.setOwner(offline);
                        region.queueSave();
                        plugin.getDoorManager().syncDoorOwnershipForRegion(region.getRegion().getId());
                        MessageUtils.sendSuccess(admin, "Owner set to " + target.getName());
                    } catch (Exception ex) {
                        MessageUtils.sendError(admin, "Failed to set owner: " + ex.getMessage());
                    }
                }
                case ADD_MEMBER -> {
                    Region region = plugin.findRegionById(prompt.regionId);
                    if (region == null) { MessageUtils.sendError(admin, "Region not found."); return; }
                    boolean ok = cm.addMember(region, admin, input);
                    if (ok) MessageUtils.sendSuccess(admin, "Member added.");
                }
                case REMOVE_MEMBER -> {
                    Region region = plugin.findRegionById(prompt.regionId);
                    if (region == null) { MessageUtils.sendError(admin, "Region not found."); return; }
                    boolean ok = cm.removeMember(region, admin, input);
                    if (ok) MessageUtils.sendSuccess(admin, "Member removed.");
                }
            }
        } finally {
            prompts.remove(admin.getUniqueId());
        }
    }
}



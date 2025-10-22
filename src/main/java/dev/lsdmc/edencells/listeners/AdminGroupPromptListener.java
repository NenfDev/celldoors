package dev.lsdmc.edencells.listeners;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.models.CellGroupManager;
import dev.lsdmc.edencells.utils.MessageUtils;
import dev.lsdmc.edencells.utils.PermissionRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class AdminGroupPromptListener implements Listener {

    private static final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();
    private final EdenCells plugin;

    public AdminGroupPromptListener(EdenCells plugin) {
        this.plugin = plugin;
    }

    private enum Action {
        CREATE_GROUP_ID,
        GLOBAL_LIMIT,
        GROUP_LIMIT,
        GROUP_COST,
        GROUP_ACCESS,
        GROUP_PERMISSION,
        REGION_ADD,
        REGION_REMOVE,
        GROUP_PRIORITY,
        GROUP_DISPLAY
    }

    private record Prompt(Action action, String groupId, Integer page) {}

    
    public static void beginCreateGroupPrompt(Player admin) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.CREATE_GROUP_ID, null, 0));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter new group ID</color> <color:#ADB5BD>(letters, numbers, _-)</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGlobalLimitPrompt(Player admin, int page) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GLOBAL_LIMIT, null, page));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter global limit</color> <color:#ADB5BD>(-1 for unlimited)</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupLimitPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_LIMIT, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter cell limit</color> <color:#ADB5BD>(-1 for unlimited)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupCostPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_COST, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter teleport cost</color> <color:#ADB5BD>(-1 for default)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupAccessPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_ACCESS, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter access</color> <color:#ADB5BD>(all | owner | permission)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupPermissionPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_PERMISSION, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter permission</color> <color:#ADB5BD>(or 'none' to clear)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginRegionAddPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.REGION_ADD, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Add regions</color> <color:#ADB5BD>(space/comma separated)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginRegionRemovePrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.REGION_REMOVE, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Remove regions</color> <color:#ADB5BD>(space/comma separated)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupPriorityPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_PRIORITY, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter priority</color> <color:#ADB5BD>(integer)</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    public static void beginGroupDisplayPrompt(Player admin, String groupId) {
        prompts.put(admin.getUniqueId(), new Prompt(Action.GROUP_DISPLAY, groupId, null));
        MessageUtils.send(admin, "<color:#9D4EDD>Enter display name</color> for <color:#FFB3C6>" + groupId + "</color> or <color:#FF6B6B>cancel</color>");
    }

    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = prompts.get(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String raw = event.getMessage().trim();
        if (raw.equalsIgnoreCase("cancel")) {
            prompts.remove(player.getUniqueId());
            MessageUtils.send(player, "<color:#FF6B6B>Action cancelled.</color>");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, prompt, raw));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        prompts.remove(event.getPlayer().getUniqueId());
    }

    private void handleInput(Player admin, Prompt prompt, String input) {
        CellGroupManager gm = plugin.getCellGroupManager();
        try {
            switch (prompt.action) {
                case CREATE_GROUP_ID -> {
                    if (!gm.isValidGroupName(input)) {
                        MessageUtils.sendError(admin, "Invalid group ID.");
                        return;
                    }
                    if (gm.getGroup(input) != null) {
                        MessageUtils.sendError(admin, "Group already exists.");
                        return;
                    }
                    CellGroup created = gm.createGroup(input);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Created group '" + input + "'. Now set a display name.");
                    beginGroupDisplayPrompt(admin, created.getName());
                }
                case GLOBAL_LIMIT -> {
                    int val = Integer.parseInt(input);
                    gm.setGlobalCellLimit(val);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Global limit updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupsGUI(plugin).open(admin, Math.max(0, prompt.page == null ? 0 : prompt.page));
                }
                case GROUP_LIMIT -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    int val = Integer.parseInt(input);
                    g.setCellLimit(val);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Cell limit updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case GROUP_COST -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    double val = Double.parseDouble(input);
                    g.setTeleportCost(val);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Teleport cost updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case GROUP_ACCESS -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    String v = input.toLowerCase();
                    if (!v.equals("all") && !v.equals("owner") && !v.equals("permission")) {
                        MessageUtils.sendError(admin, "Invalid access. Use all/owner/permission.");
                        return;
                    }
                    g.setTeleportAccess(v);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Teleport access updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case GROUP_PERMISSION -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    String old = g.getRequiredPermission();
                    String perm = input.equalsIgnoreCase("none") ? null : input.trim();
                    g.setRequiredPermission(perm);
                    
                    try {
                        if (old != null && (perm == null || !old.equals(perm))) {
                            PermissionRegistry.unregister(old);
                        }
                        if (perm != null) {
                            PermissionRegistry.register(perm, "Access to cell group '" + g.getName() + "'");
                        }
                    } catch (Exception ignored) {}
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Permission updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case REGION_ADD -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    int added = 0;
                    for (String token : splitIds(input)) {
                        if (g.addRegion(token)) added++;
                    }
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Added " + added + " region(s).");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case REGION_REMOVE -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    int removed = 0;
                    for (String token : splitIds(input)) {
                        if (g.removeRegion(token)) removed++;
                    }
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Removed " + removed + " region(s).");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case GROUP_PRIORITY -> {
                    CellGroup g = gm.getGroup(prompt.groupId);
                    if (g == null) { MessageUtils.sendError(admin, "Group not found."); return; }
                    int val = Integer.parseInt(input);
                    g.setPriority(val);
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Priority updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, g.getName());
                }
                case GROUP_DISPLAY -> {
                    String id = prompt.groupId;
                    boolean ok = gm.updateGroupDisplayName(id, input);
                    if (!ok) {
                        MessageUtils.sendError(admin, "Failed to update display name.");
                        return;
                    }
                    gm.saveGroups();
                    MessageUtils.sendSuccess(admin, "Display name updated.");
                    new dev.lsdmc.edencells.gui.admin.AdminGroupEditGUI(plugin).open(admin, id);
                }
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendError(admin, "Invalid number format.");
        } catch (Exception e) {
            plugin.getLogger().warning("Prompt handling error: " + e.getMessage());
            MessageUtils.sendError(admin, "An error occurred.");
        } finally {
            prompts.remove(admin.getUniqueId());
        }
    }

    private List<String> splitIds(String input) {
        String norm = input.replace(',', ' ');
        String[] parts = norm.split("\\s+");
        List<String> ids = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) ids.add(p.trim());
        return ids;
    }
}



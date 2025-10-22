package dev.lsdmc.edencells.commands;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.managers.TeleportNPCManager;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public final class TeleportNPCCommands implements CommandExecutor, TabCompleter {

    private final EdenCells plugin;
    private final TeleportNPCManager teleportNPCManager;

    public TeleportNPCCommands(EdenCells plugin, TeleportNPCManager teleportNPCManager) {
        this.plugin = plugin;
        this.teleportNPCManager = teleportNPCManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, Constants.Messages.PREFIX + Constants.Messages.ERROR_COLOR +
                    "This command can only be used by players!");
            return true;
        }

        if (!player.hasPermission(Constants.Permissions.ADMIN_TELEPORT_NPC)) {
            MessageUtils.sendNoPermission(player);
            return true;
        }

        if (!CitizensAPI.hasImplementation()) {
            MessageUtils.sendError(player, "Citizens is not installed or enabled!");
            MessageUtils.sendInfo(player, "Please install Citizens to use teleport NPCs.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "create" -> handleCreate(player, args);
            case "remove" -> handleRemove(player, args);
            case "setgroup" -> handleSetGroup(player, args);
            case "list" -> handleList(player);
            case "reload" -> handleReload(player);
            case "debug" -> handleDebug(player);
            default -> {
                sendHelp(player);
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendError(player, "Usage: /teleportnpc create <name> <cellgroup>");
            return true;
        }

        
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        String groupId = args[args.length - 1];

        
        if (plugin.getCellGroupManager().getGroup(groupId) == null) {
            MessageUtils.sendError(player, "Cell group '%s' does not exist!", groupId);
            MessageUtils.sendInfo(player, "Available groups: %s",
                    String.join(", ", plugin.getCellGroupManager().getAllGroups().keySet()));
            return true;
        }

        
        var npc = teleportNPCManager.createTeleportNPC(name, player.getLocation(), groupId);

        if (npc == null) {
            MessageUtils.sendError(player, "Failed to create NPC!");
            return true;
        }

        MessageUtils.send(player, "<color:#51CF66>Created teleport NPC '<color:#FFB3C6>" + name +
                "</color>' with ID <color:#FFB3C6>" + npc.getId() + "</color></color>");
        MessageUtils.send(player, "<color:#06FFA5>Linked to cell group: <color:#FFB3C6>" +
                groupId + "</color></color>");

        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(player, "Usage: /teleportnpc remove <id>");
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(player, "Invalid NPC ID!");
            return true;
        }

        if (teleportNPCManager.removeTeleportNPC(npcId)) {
            MessageUtils.send(player, "<color:#51CF66>Removed teleport NPC with ID <color:#FFB3C6>" +
                    npcId + "</color></color>");
        } else {
            MessageUtils.sendError(player, "NPC with ID %d not found!", npcId);
        }

        return true;
    }

    private boolean handleSetGroup(Player player, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendError(player, "Usage: /teleportnpc setgroup <id> <cellgroup>");
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendError(player, "Invalid NPC ID!");
            return true;
        }

        String groupId = args[2];

        
        if (plugin.getCellGroupManager().getGroup(groupId) == null) {
            MessageUtils.sendError(player, "Cell group '%s' does not exist!", groupId);
            MessageUtils.sendInfo(player, "Available groups: %s",
                    String.join(", ", plugin.getCellGroupManager().getAllGroups().keySet()));
            return true;
        }

        if (teleportNPCManager.setExistingNPCAsTeleport(npcId, groupId)) {
            MessageUtils.send(player, "<color:#51CF66>Set NPC '<color:#FFB3C6>" + npcId +
                    "</color>' as teleport NPC for group '<color:#FFB3C6>" + groupId + "</color>'</color>");
        } else {
            MessageUtils.sendError(player, "NPC with ID %d not found!", npcId);
        }

        return true;
    }

    private boolean handleList(Player player) {
        var npcs = CitizensAPI.getNPCRegistry().sorted();

        MessageUtils.sendInfo(player, "=== Teleport NPCs ===");

        int count = 0;
        for (var npc : npcs) {
            if (npc.hasTrait(dev.lsdmc.edencells.npc.TeleportNPC.class)) {
                var config = teleportNPCManager.getNPCConfig(npc.getId());
                if (config != null) {
                    MessageUtils.send(player, "• ID <color:#FFB3C6>" + npc.getId() +
                            "</color>: <color:#ADB5BD>" + npc.getName() +
                            "</color> - Group: <color:#FFB3C6>" + config.groupId() + "</color>");
                    count++;
                }
            }
        }

        if (count == 0) {
            MessageUtils.sendInfo(player, "No teleport NPCs found.");
        }

        return true;
    }

    private boolean handleReload(Player player) {
        teleportNPCManager.reloadTeleportNPCs();
        MessageUtils.send(player, "<color:#51CF66>Reloaded teleport NPCs!</color>");
        return true;
    }

    private boolean handleDebug(Player player) {
        MessageUtils.send(player, "<color:#9D4EDD>=== Teleport NPC Debug Info ===</color>");
        MessageUtils.send(player, "<color:#FFB3C6>Teleportation enabled:</color> " + 
                plugin.getConfigManager().isTeleportationEnabled());
        MessageUtils.send(player, "<color:#FFB3C6>Default cost:</color> " + 
                plugin.getConfigManager().getTeleportationDefaultCost());
        MessageUtils.send(player, "<color:#FFB3C6>Cooldown seconds:</color> " + 
                plugin.getConfigManager().getTeleportationCooldownSeconds());
        
        
        MessageUtils.send(player, "<color:#FFB3C6>Testing messages:</color>");
        MessageUtils.send(player, "  no_permission: " + 
                plugin.getConfigManager().getTeleportationMessage("no_permission"));
        MessageUtils.send(player, "  teleported: " + 
                plugin.getConfigManager().getTeleportationMessage("teleported"));
        MessageUtils.send(player, "  cooldown: " + 
                plugin.getConfigManager().getTeleportationMessage("cooldown"));
        MessageUtils.send(player, "  disabled: " + 
                plugin.getConfigManager().getTeleportationMessage("disabled"));
        
        return true;
    }

    private void sendHelp(Player player) {
        MessageUtils.send(player, "<color:#9D4EDD>=== Teleport NPC Commands ===</color>");
        MessageUtils.send(player, "<color:#FFB3C6>/teleportnpc create <name> <cellgroup></color> <color:#ADB5BD>- Create teleport NPC</color>");
        MessageUtils.send(player, "<color:#FFB3C6>/teleportnpc remove <id></color> <color:#ADB5BD>- Remove teleport NPC</color>");
        MessageUtils.send(player, "<color:#FFB3C6>/teleportnpc setgroup <id> <cellgroup></color> <color:#ADB5BD>- Set existing NPC as teleport</color>");
        MessageUtils.send(player, "<color:#FFB3C6>/teleportnpc list</color> <color:#ADB5BD>- List all teleport NPCs</color>");
        MessageUtils.send(player, "<color:#FFB3C6>/teleportnpc reload</color> <color:#ADB5BD>- Reload teleport NPCs</color>");
        MessageUtils.send(player, "");
        MessageUtils.send(player, "<color:#51CF66>How NPCs Work:</color>");
        MessageUtils.send(player, "  <color:#06FFA5>• Right-click:</color> <color:#ADB5BD>Teleport to your cell in the NPC's ward group</color>");
        MessageUtils.send(player, "  <color:#06FFA5>• Each NPC is linked to a specific cell group (ward)</color>");
        MessageUtils.send(player, "  <color:#06FFA5>• Players need the group permission to use the NPC</color>");
        MessageUtils.send(player, "");
        MessageUtils.send(player, "<color:#51CF66>Teleport Costs:</color>");

        
        List<String> freeGroups = plugin.getConfigManager().getTeleportationFreeGroups();
        if (!freeGroups.isEmpty()) {
            MessageUtils.send(player, "  <color:#FFB3C6>• Free Groups:</color> <color:#51CF66>" +
                    String.join(", ", freeGroups) + "</color>");
        }
        MessageUtils.send(player, "  <color:#FFB3C6>• Each group can have custom teleport costs</color>");
        MessageUtils.send(player, "  <color:#FFB3C6>• Configure costs in cell-groups.yml</color>");

        
        boolean hasFreeTeleport = hasFreeTeleportation(player);
        if (hasFreeTeleport) {
            MessageUtils.send(player, "");
            MessageUtils.send(player, "<color:#FFB3C6>👑 VIP Benefit:</color> <color:#51CF66>You teleport for free!</color>");
        }
    }

    
    private boolean hasFreeTeleportation(Player player) {
        
        if (player.hasPermission(Constants.Permissions.BYPASS_PAYMENT)) {
            return true;
        }

        
        List<String> freeGroups = plugin.getConfigManager().getTeleportationFreeGroups();
        for (String group : freeGroups) {
            if (group == null || group.trim().isEmpty()) {
                continue;
            }
            String sanitized = group.trim().toLowerCase();
            
            if (player.hasPermission(Constants.Permissions.GROUP_PREFIX + sanitized) ||
                    player.hasPermission("group." + sanitized) ||
                    player.hasPermission(sanitized)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "remove", "setgroup", "list", "reload", "debug")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        
        if (args.length == 2 && args[0].equalsIgnoreCase("setgroup")) {
            
            return Collections.emptyList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setgroup")) {
            
            String partial = args[2].toLowerCase();
            return plugin.getCellGroupManager().getAllGroups().keySet()
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        
        if (args.length >= 3 && args[0].equalsIgnoreCase("create")) {
            
            String partial = args[args.length - 1].toLowerCase();
            return plugin.getCellGroupManager().getAllGroups().keySet()
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
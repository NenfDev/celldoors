package dev.lsdmc.edencells.commands;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.models.CellGroup;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import dev.lsdmc.edencells.utils.PermissionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;


public final class ConfigCommands implements CommandExecutor, TabCompleter {
    
    private final EdenCells plugin;
    
    
    private final Map<String, List<String>> configSections = new HashMap<>();
    
    public ConfigCommands(EdenCells plugin) {
        this.plugin = plugin;
        initializeConfigSections();
    }
    
    private void initializeConfigSections() {
        
        configSections.put("general", Arrays.asList("debug", "locale"));
        
        
        configSections.put("messages", Arrays.asList("prefix", "colors"));
        
        
        configSections.put("cells", Arrays.asList("max-per-player", "default-max-members", "sign-keywords", "gui"));
        
        
        configSections.put("economy", Arrays.asList("currency-symbol", "currency-suffix", "members", "fees"));
        
        
        configSections.put("cell-groups", Arrays.asList("global-limit", "groups"));
        
        
        configSections.put("teleportation", Arrays.asList("base-cost", "require-payment", "cooldown", "effects"));
        
        
        configSections.put("doors", Arrays.asList("valid-materials", "sounds", "auto-close-delay"));
        
        
        configSections.put("security", Arrays.asList("rate-limits", "validation", "audit"));
        
        
        configSections.put("integrations", Arrays.asList("arm", "citizens", "worldguard"));
        
        
        configSections.put("performance", Arrays.asList("cache", "async"));
        
        
        configSections.put("maintenance", Arrays.asList("auto-save-interval", "backup", "cleanup"));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PermissionManager.hasPermission(sender, Constants.Permissions.ADMIN)) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }
        
        if (args.length == 0) {
            sendConfigHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "get":
                return handleGet(sender, args);
            case "set":
                return handleSet(sender, args);
            case "list":
                return handleList(sender, args);
            case "reload":
                return handleReload(sender);
            case "save":
                return handleSave(sender);
            case "help":
                sendConfigHelp(sender);
                return true;
            default:
                MessageUtils.sendError(sender, "Unknown subcommand: " + subCommand);
                sendConfigHelp(sender);
                return true;
        }
    }
    
    private boolean handleGet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendError(sender, "Usage: /econfig get <path>");
            return true;
        }
        
        String path = String.join(".", Arrays.copyOfRange(args, 1, args.length));
        
        
        if (path.startsWith("cell-groups.")) {
            return handleCellGroupsGet(sender, path);
        }
        
        if (!plugin.getConfig().contains(path)) {
            MessageUtils.sendError(sender, "Config path '%s' not found!", path);
            return true;
        }
        
        Object value = plugin.getConfig().get(path);
        String valueStr = formatConfigValue(value);
        
        MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + valueStr + "</color>");
        
        return true;
    }
    
    private boolean handleCellGroupsGet(CommandSender sender, String path) {
        String subPath = path.substring("cell-groups.".length());
        
        if (subPath.equals("global-limit")) {
            int globalLimit = plugin.getCellGroupManager().getGlobalCellLimit();
            MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + globalLimit + "</color>");
            return true;
        }
        
        if (subPath.startsWith("groups.")) {
            String groupPath = subPath.substring("groups.".length());
            String[] parts = groupPath.split("\\.", 2);
            String groupName = parts[0];
            
            CellGroup group = plugin.getCellGroupManager().getGroup(groupName);
            if (group == null) {
                MessageUtils.sendError(sender, "Cell group '%s' not found!", groupName);
                return true;
            }
            
            if (parts.length == 1) {
                
                MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color>");
                MessageUtils.send(sender, "  <color:#06FFA5>display-name:</color> <color:#51CF66>" + group.getDisplayName() + "</color>");
                MessageUtils.send(sender, "  <color:#06FFA5>regions:</color> <color:#51CF66>" + group.getRegions().size() + " regions</color>");
                if (group.getCellLimit() != -1) {
                    MessageUtils.send(sender, "  <color:#06FFA5>cell-limit:</color> <color:#51CF66>" + group.getCellLimit() + "</color>");
                }
                if (group.getTeleportCost() != -1) {
                    MessageUtils.send(sender, "  <color:#06FFA5>teleport-cost:</color> <color:#51CF66>" + group.getTeleportCost() + "</color>");
                }
                if (group.getRequiredPermission() != null) {
                    MessageUtils.send(sender, "  <color:#06FFA5>permission:</color> <color:#51CF66>" + group.getRequiredPermission() + "</color>");
                }

                return true;
            }
            
            String property = parts[1];
            switch (property) {
                case "display-name":
                    MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + group.getDisplayName() + "</color>");
                    break;
                case "cell-limit":
                    MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + group.getCellLimit() + "</color>");
                    break;
                case "teleport-cost":
                    MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + group.getTeleportCost() + "</color>");
                    break;
                case "permission":
                    String perm = group.getRequiredPermission();
                    MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + (perm != null ? perm : "null") + "</color>");
                    break;

                case "regions":
                    MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + formatConfigValue(group.getRegions()) + "</color>");
                    break;
                default:
                    MessageUtils.sendError(sender, "Unknown group property: " + property);
                    return true;
            }
            return true;
        }
        
        MessageUtils.sendError(sender, "Invalid cell-groups path: " + path);
        return true;
    }
    
    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendError(sender, "Usage: /econfig set <path> <value>");
            return true;
        }
        
        String path = args[1];
        String valueStr = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        
        if (path.startsWith("cell-groups.")) {
            return handleCellGroupsSet(sender, path, valueStr);
        }
        
        
        Object currentValue = plugin.getConfig().get(path);
        Object newValue = parseValue(valueStr, currentValue);
        
        if (newValue == null && !valueStr.equalsIgnoreCase("null")) {
            MessageUtils.sendError(sender, "Invalid value format for path: " + path);
            return true;
        }
        
        
        plugin.getConfig().set(path, newValue);
        plugin.saveConfig();
        
        
        applyConfigChange(path, newValue);
        
        MessageUtils.sendSuccess(sender, "Successfully updated configuration:");
        MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + formatConfigValue(newValue) + "</color>");
        
        return true;
    }
    
    private boolean handleCellGroupsSet(CommandSender sender, String path, String valueStr) {
        String subPath = path.substring("cell-groups.".length());
        
        if (subPath.equals("global-limit")) {
            try {
                int limit = Integer.parseInt(valueStr);
                if (limit < -1) {
                    MessageUtils.sendError(sender, "Global limit cannot be less than -1!");
                    return true;
                }
                
                plugin.getCellGroupManager().setGlobalCellLimit(limit);
                plugin.getCellGroupManager().saveGroups();
                
                MessageUtils.sendSuccess(sender, "Successfully updated global cell limit:");
                MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + limit + "</color>");
                return true;
            } catch (NumberFormatException e) {
                MessageUtils.sendError(sender, "Invalid number format for global limit!");
                return true;
            }
        }
        
        if (subPath.startsWith("groups.")) {
            String groupPath = subPath.substring("groups.".length());
            String[] parts = groupPath.split("\\.", 2);
            String groupName = parts[0];
            
            CellGroup group = plugin.getCellGroupManager().getGroup(groupName);
            if (group == null) {
                MessageUtils.sendError(sender, "Cell group '%s' not found!", groupName);
                return true;
            }
            
            if (parts.length < 2) {
                MessageUtils.sendError(sender, "Must specify a property to set for group!");
                return true;
            }
            
            String property = parts[1];
            try {
                switch (property) {
                    case "display-name":
                        
                        MessageUtils.sendError(sender, "Display name cannot be changed after creation!");
                        return true;
                    case "cell-limit":
                        int limit = Integer.parseInt(valueStr);
                        group.setCellLimit(limit);
                        break;
                    case "teleport-cost":
                        double cost = Double.parseDouble(valueStr);
                        group.setTeleportCost(cost);
                        break;
                    case "permission":
                        if (valueStr.equalsIgnoreCase("null") || valueStr.trim().isEmpty()) {
                            group.setRequiredPermission(null);
                        } else {
                            group.setRequiredPermission(valueStr);
                        }
                        break;

                    default:
                        MessageUtils.sendError(sender, "Unknown group property: " + property);
                        MessageUtils.sendInfo(sender, "Available properties: cell-limit, teleport-cost, permission");
                        return true;
                }
                
                plugin.getCellGroupManager().saveGroups();
                MessageUtils.sendSuccess(sender, "Successfully updated group property:");
                MessageUtils.send(sender, "<color:#FFB3C6>" + path + ":</color> <color:#51CF66>" + valueStr + "</color>");
                return true;
                
            } catch (NumberFormatException e) {
                MessageUtils.sendError(sender, "Invalid number format for property: " + property);
                return true;
            } catch (Exception e) {
                MessageUtils.sendError(sender, "Failed to set property: " + e.getMessage());
                return true;
            }
        }
        
        MessageUtils.sendError(sender, "Invalid cell-groups path: " + path);
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        String section = args.length > 1 ? args[1] : "";
        
        if (section.isEmpty()) {
            
            MessageUtils.send(sender, "<color:#9D4EDD>=== Configuration Sections ===</color>");
            MessageUtils.send(sender, "");
            for (String key : configSections.keySet()) {
                MessageUtils.send(sender, "<color:#06FFA5>• <color:#FFB3C6>" + key + "</color></color>");
            }
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, "<color:#ADB5BD>Use /econfig list <section> for details</color>");
        } else if (section.equals("cell-groups")) {
            
            MessageUtils.send(sender, "<color:#9D4EDD>=== Cell Groups Configuration ===</color>");
            MessageUtils.send(sender, "");
            
            
            int globalLimit = plugin.getCellGroupManager().getGlobalCellLimit();
            MessageUtils.send(sender, "<color:#FFB3C6>global-limit:</color> <color:#51CF66>" + globalLimit + "</color>");
            MessageUtils.send(sender, "");
            
            
            MessageUtils.send(sender, "<color:#FFB3C6>groups:</color>");
            for (CellGroup group : plugin.getCellGroupManager().getAllGroups().values()) {
                MessageUtils.send(sender, "  <color:#06FFA5>" + group.getName() + ":</color>");
                MessageUtils.send(sender, "    <color:#FFB3C6>display-name:</color> <color:#51CF66>" + group.getDisplayName() + "</color>");
                MessageUtils.send(sender, "    <color:#FFB3C6>regions:</color> <color:#51CF66>" + group.getRegions().size() + " regions</color>");
                if (group.getCellLimit() != -1) {
                    MessageUtils.send(sender, "    <color:#FFB3C6>cell-limit:</color> <color:#51CF66>" + group.getCellLimit() + "</color>");
                }
                if (group.getTeleportCost() != -1) {
                    MessageUtils.send(sender, "    <color:#FFB3C6>teleport-cost:</color> <color:#51CF66>" + group.getTeleportCost() + "</color>");
                }
                if (group.getRequiredPermission() != null) {
                    MessageUtils.send(sender, "    <color:#FFB3C6>permission:</color> <color:#51CF66>" + group.getRequiredPermission() + "</color>");
                }

                MessageUtils.send(sender, "");
            }
            
            MessageUtils.send(sender, "<color:#ADB5BD>Use /econfig set cell-groups.<path> <value> to modify</color>");
        } else {
            
            var options = plugin.getConfig().getConfigurationSection(section);
            if (options == null) {
                MessageUtils.sendError(sender, "Section '%s' not found!", section);
                return true;
            }
            
            MessageUtils.send(sender, "<color:#9D4EDD>=== " + section.substring(0, 1).toUpperCase() + section.substring(1) + " Configuration ===</color>");
            MessageUtils.send(sender, "");
            
            
            List<String> sortedKeys = new ArrayList<>();
            for (String key : options.getKeys(true)) {
                Object value = options.get(key);
                if (!(value instanceof org.bukkit.configuration.ConfigurationSection)) {
                    sortedKeys.add(key);
                }
            }
            Collections.sort(sortedKeys);
            
            
            for (String key : sortedKeys) {
                Object value = options.get(key);
                String fullPath = section + "." + key;
                String formattedValue = formatConfigValue(value);
                
                
                String displayKey = key.replace("-", " ").replace(".", " > ");
                
                MessageUtils.send(sender, "<color:#FFB3C6>" + displayKey + ":</color> <color:#51CF66>" + formattedValue + "</color>");
            }
            
            MessageUtils.send(sender, "");
            MessageUtils.send(sender, "<color:#ADB5BD>Use /econfig set <path> <value> to modify</color>");
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        try {
            
            plugin.reload();
            
            
            plugin.getCellGroupManager().reloadGroups();
            plugin.getDoorManager().reload();
            plugin.getTeleportNPCManager().reloadTeleportNPCs();
            
            MessageUtils.sendSuccess(sender, "Configuration reloaded successfully!");
            MessageUtils.sendInfo(sender, "Reloaded: config.yml, cell-groups.yml, doors.yml (runtime), and NPC configs");
            return true;
        } catch (Exception e) {
            MessageUtils.sendError(sender, "Reload failed: %s", e.getMessage());
            plugin.getLogger().warning("/econfig reload failed: " + e.getMessage());
            return true;
        }
    }
    
    private boolean handleSave(CommandSender sender) {
        plugin.saveConfig();
        plugin.getCellGroupManager().saveGroups();
        plugin.getDoorManager().saveDoors();
        
        MessageUtils.sendSuccess(sender, "All configurations saved successfully!");
        MessageUtils.sendInfo(sender, "Saved config.yml, cell-groups.yml, and doors.yml");
        
        return true;
    }
    
    private void sendConfigHelp(CommandSender sender) {
        MessageUtils.send(sender, "<color:#9D4EDD>=== Configuration Commands ===</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig get <path></color> <color:#06FFA5>- Get a config value</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig set <path> <value></color> <color:#06FFA5>- Set a config value</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig list [section]</color> <color:#06FFA5>- List config options</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig reload</color> <color:#06FFA5>- Reload configuration</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig save</color> <color:#06FFA5>- Save all configurations</color>");
        MessageUtils.send(sender, "<color:#FFB3C6>/econfig help</color> <color:#06FFA5>- Show this help</color>");
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "<color:#ADB5BD>Examples:</color>");
        MessageUtils.send(sender, "<color:#ADB5BD>  /econfig set teleportation.base-cost 100</color>");
        MessageUtils.send(sender, "<color:#ADB5BD>  /econfig get cell-groups.global-limit</color>");
        MessageUtils.send(sender, "<color:#ADB5BD>  /econfig set cell-groups.groups.jcells.cell-limit 1</color>");
        MessageUtils.send(sender, "<color:#ADB5BD>  /econfig list cell-groups</color>");
    }
    
    private String formatConfigValue(Object value) {
        if (value == null) return "null";
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            return "[" + list.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ")) + "]";
        }
        if (value instanceof String) {
            String str = value.toString();
            
            if (str.contains("&") || str.contains("§")) {
                return "\"" + str + "\"";
            }
            return str;
        }
        return value.toString();
    }
    
    private Object parseValue(String valueStr, Object currentValue) {
        if (valueStr.equalsIgnoreCase("null")) return null;
        if (valueStr.equalsIgnoreCase("true")) return true;
        if (valueStr.equalsIgnoreCase("false")) return false;
        
        
        if (currentValue instanceof Integer) {
            try {
                return Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        if (currentValue instanceof Double) {
            try {
                return Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        if (currentValue instanceof List) {
            
            return Arrays.stream(valueStr.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        }
        
        
        return valueStr;
    }
    
    private void applyConfigChange(String path, Object value) {
        
        switch (path) {
            case "general.debug":
                plugin.debug("Debug mode " + (((boolean) value) ? "enabled" : "disabled"));
                break;
            case "cell-groups.global-limit":
                plugin.getCellGroupManager().setGlobalCellLimit((int) value);
                break;
            
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!PermissionManager.hasPermission(sender, Constants.Permissions.ADMIN)) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            return Arrays.asList("get", "set", "list", "reload", "save", "help")
                .stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("get") || subCommand.equals("set")) {
                
                List<String> suggestions = new ArrayList<>();
                
                
                suggestions.addAll(configSections.keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList()));
                
                
                if ("cell-groups".startsWith(args[1].toLowerCase())) {
                    suggestions.add("cell-groups.global-limit");
                    for (CellGroup group : plugin.getCellGroupManager().getAllGroups().values()) {
                        suggestions.add("cell-groups.groups." + group.getName());
                    }
                }
                
                return suggestions;
            } else if (subCommand.equals("list")) {
                return configSections.keySet().stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            
            String path = args[1];
            
            
            if (path.startsWith("cell-groups.groups.")) {
                String[] parts = path.split("\\.");
                if (parts.length >= 3) {
                    return Arrays.asList("cell-limit", "teleport-cost", "permission");
                }
            } else if (path.equals("cell-groups.global-limit")) {
                return Arrays.asList("15", "10", "-1");
            }
            
            Object currentValue = plugin.getConfig().get(path);
            if (currentValue instanceof Boolean) {
                return Arrays.asList("true", "false");
            }
        }
        
        return Collections.emptyList();
    }
} 
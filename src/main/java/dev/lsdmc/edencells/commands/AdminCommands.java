package dev.lsdmc.edencells.commands;

import dev.lsdmc.edencells.EdenCells;
import dev.lsdmc.edencells.utils.Constants;
import dev.lsdmc.edencells.utils.MessageUtils;
import dev.lsdmc.edencells.utils.PermissionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class AdminCommands implements CommandExecutor, TabCompleter {

    private final EdenCells plugin;

    public AdminCommands(EdenCells plugin) {
        this.plugin = plugin;
        if (plugin.getCommand("ecadmin") != null) {
            plugin.getCommand("ecadmin").setExecutor(this);
            plugin.getCommand("ecadmin").setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendError(sender, "This command can only be used by players.");
            return true;
        }

        if (!PermissionManager.hasPermission(player, Constants.Permissions.ADMIN_DASHBOARD)) {
            MessageUtils.sendNoPermission(player);
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("dashboard")) {
            try {
                new dev.lsdmc.edencells.gui.admin.AdminDashboardGUI(plugin).open(player);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to open admin dashboard: " + e.getMessage());
                MessageUtils.sendError(player, "Failed to open admin dashboard.");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "players":
                try {
                    new dev.lsdmc.edencells.gui.admin.AdminPlayersGUI(plugin).open(player, 0);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to open admin players GUI: " + e.getMessage());
                    MessageUtils.sendError(player, "Failed to open players GUI.");
                }
                return true;
            default:
                MessageUtils.send(player, "<color:#06FFA5>/ecadmin dashboard</color> - Open admin panel");
                MessageUtils.send(player, "<color:#06FFA5>/ecadmin players</color> - Manage players & cells");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(Arrays.asList("dashboard", "players"), args[0]);
        }
        return Collections.emptyList();
    }

    private List<String> prefix(List<String> list, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(p)) out.add(s);
        }
        return out;
    }
}



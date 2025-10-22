package dev.lsdmc.edencells.utils;

import dev.lsdmc.edencells.EdenCells;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public final class MessageUtils {
    
    
    private static final TextColor PRIMARY_FALLBACK = TextColor.fromHexString("#9D4EDD");
    private static final TextColor SECONDARY_FALLBACK = TextColor.fromHexString("#06FFA5");
    private static final TextColor ACCENT_FALLBACK = TextColor.fromHexString("#FFB3C6");
    private static final TextColor ERROR_FALLBACK = TextColor.fromHexString("#FF6B6B");
    private static final TextColor SUCCESS_FALLBACK = TextColor.fromHexString("#51CF66");
    private static final TextColor NEUTRAL_FALLBACK = TextColor.fromHexString("#ADB5BD");

    private static TextColor colorFromHex(String hex, TextColor fallback) {
        try {
            TextColor c = TextColor.fromHexString(hex);
            return c != null ? c : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static TextColor primaryColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getPrimaryColor(), PRIMARY_FALLBACK);
    }

    private static TextColor secondaryColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getSecondaryColor(), SECONDARY_FALLBACK);
    }

    private static TextColor accentColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getAccentColor(), ACCENT_FALLBACK);
    }

    private static TextColor errorColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getErrorColor(), ERROR_FALLBACK);
    }

    private static TextColor successColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getSuccessColor(), SUCCESS_FALLBACK);
    }

    private static TextColor neutralColor() {
        return colorFromHex(EdenCells.getInstance().getConfigManager().getNeutralColor(), NEUTRAL_FALLBACK);
    }
    
    private MessageUtils() {} 
    
    
    public static Component formatTitle(String text) {
        return Component.text(text)
            .color(primaryColor())
            .decoration(TextDecoration.BOLD, true);
    }
    
    
    public static Component success(String text) {
        return Component.text(text).color(successColor());
    }
    
    
    public static Component error(String text) {
        return Component.text(text).color(errorColor());
    }
    
    
    public static Component warning(String text) {
        return Component.text(text).color(NamedTextColor.YELLOW);
    }
    
    
    public static Component info(String text) {
        return Component.text(text).color(secondaryColor());
    }
    
    
    public static Component heading(String text) {
        return Component.text(text)
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.BOLD, true);
    }
    
    
    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    
    public static String formatPrice(double amount, String currency) {
        return String.format("$%.2f %s", amount, currency);
    }
    
    
    public static Component divider() {
        return Component.text("──────────────────────────────────")
            .color(primaryColor());
    }
    
    
    public static Component noPermission(String permission) {
        return error("You don't have permission to do this!" + 
                    (permission.isEmpty() ? "" : " (" + permission + ")"));
    }
    
    
    public static Component actionMessage(String text, NamedTextColor color) {
        return Component.text(text)
            .color(color)
            .decoration(TextDecoration.ITALIC, true);
    }
    
    
    public static Component status(String label, boolean isPositive) {
        NamedTextColor color = isPositive ? NamedTextColor.GREEN : NamedTextColor.RED;
        String symbol = isPositive ? "✓" : "✗";
        return Component.text(symbol + " " + label).color(color);
    }
    
    
    public static Component progress(String label, int current, int max) {
        NamedTextColor color = (current >= max) ? NamedTextColor.RED : NamedTextColor.GREEN;
        return Component.text(label + ": " + current + "/" + max).color(color);
    }
    
    
    public static Component timeRemaining(String timeString) {
        return Component.text("Expires: " + timeString).color(NamedTextColor.YELLOW);
    }
    
    
    public static Component cost(double amount, String currency) {
        return Component.text("Cost: $" + String.format("%.2f", amount) + " " + currency)
            .color(NamedTextColor.GOLD);
    }
    
    
    public static Component balance(double amount, String currency, boolean sufficient) {
        NamedTextColor color = sufficient ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text("Balance: $" + String.format("%.2f", amount) + " " + currency)
            .color(color);
    }
    
    
    
    
    public static void send(CommandSender sender, String message) {
        sender.sendMessage(fromMiniMessage(message));
    }
    
    
    public static void sendError(Player player, String message) {
        player.sendMessage(error(message));
    }
    
    
    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(error(message));
    }
    
    
    public static void sendError(Player player, String message, Object... args) {
        player.sendMessage(error(String.format(message, args)));
    }
    
    
    public static void sendError(CommandSender sender, String message, Object... args) {
        sender.sendMessage(error(String.format(message, args)));
    }
    
    
    public static void sendSuccess(Player player, String message) {
        player.sendMessage(success(message));
    }
    
    
    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(success(message));
    }
    
    
    public static void sendSuccess(Player player, String message, Object... args) {
        player.sendMessage(success(String.format(message, args)));
    }
    
    
    public static void sendSuccess(CommandSender sender, String message, Object... args) {
        sender.sendMessage(success(String.format(message, args)));
    }
    
    
    public static void sendInfo(Player player, String message) {
        player.sendMessage(info(message));
    }
    
    
    public static void sendInfo(Player player, String message, Object... args) {
        player.sendMessage(info(String.format(message, args)));
    }
    
    
    public static void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(info(message));
    }
    
    
    public static void sendInfo(CommandSender sender, String message, Object... args) {
        sender.sendMessage(info(String.format(message, args)));
    }
    
    
    public static void sendNoPermission(Player player) {
        player.sendMessage(noPermission(""));
    }
    
    
    public static void sendNoPermission(CommandSender sender) {
        sender.sendMessage(noPermission(""));
    }
    
    
    public static Component fromMiniMessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }
    
    
    public static Component button(String text, NamedTextColor color) {
        return Component.text("[ " + text + " ]")
            .color(color)
            .decoration(TextDecoration.BOLD, true);
    }
    
    
    public static Component highlight(String text) {
        return Component.text(text)
            .color(accentColor())
            .decoration(TextDecoration.BOLD, true);
    }
    
    
    public static Component prefix() {
        try {
            
            String configuredPrefix = EdenCells.getInstance().getConfigManager().getPrefix();
            return fromMiniMessage(configuredPrefix);
        } catch (Exception e) {
            
            return Component.text("[")
                .color(neutralColor())
                .append(Component.text("EdenCells").color(primaryColor()))
                .append(Component.text("] ").color(neutralColor()));
        }
    }
    
    
    public static void sendPrefixed(CommandSender sender, Component message) {
        sender.sendMessage(prefix().append(message));
    }
}
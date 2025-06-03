package com.gilbertomorales.howlyvelocity.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Tag {

    public static Component colorize(String text) {
        if (text == null) {
            return Component.empty();
        }

        text = text.replace("§0", "<black>")
                  .replace("§1", "<dark_blue>")
                  .replace("§2", "<dark_green>")
                  .replace("§3", "<dark_aqua>")
                  .replace("§4", "<dark_red>")
                  .replace("§5", "<dark_purple>")
                  .replace("§6", "<gold>")
                  .replace("§7", "<gray>")
                  .replace("§8", "<dark_gray>")
                  .replace("§9", "<blue>")
                  .replace("§a", "<green>")
                  .replace("§b", "<aqua>")
                  .replace("§c", "<red>")
                  .replace("§d", "<light_purple>")
                  .replace("§e", "<yellow>")
                  .replace("§f", "<white>")
                  .replace("§k", "<obfuscated>")
                  .replace("§l", "<bold>")
                  .replace("§m", "<strikethrough>")
                  .replace("§n", "<underlined>")
                  .replace("§o", "<italic>")
                  .replace("§r", "<reset>");
        
        return Component.text(text);
    }

    public static Component success(String message) {
        return Component.text("✓ ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component error(String message) {
        return Component.text("✗ ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component warning(String message) {
        return Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component info(String message) {
        return Component.text("ℹ ", NamedTextColor.BLUE, TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component prefix(String prefix, String message) {
        return Component.text("[" + prefix + "] ", NamedTextColor.GRAY, TextDecoration.BOLD)
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    public static Component howlyPrefix(String message) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Howly", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }
}

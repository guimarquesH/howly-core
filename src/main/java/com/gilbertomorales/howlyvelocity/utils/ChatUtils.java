package com.gilbertomorales.howlyvelocity.utils;

import com.velocitypowered.api.proxy.Player;

public class ChatUtils {

    /**
     * Aplica cores no texto se o jogador tiver permissão
     */
    public static String applyColors(String message, Player player) {
        if (player.hasPermission("chat.cores")) {
            return message.replaceAll("&([0-9a-fk-or])", "§$1");
        }
        return message;
    }

    /**
     * Remove códigos de cor de uma mensagem
     */
    public static String stripColors(String message) {
        if (message == null) return "";

        return message.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("&[0-9a-fk-or]", "");
    }

    /**
     * Verifica se uma mensagem contém códigos de cor
     */
    public static boolean hasColorCodes(String message) {
        if (message == null) return false;

        return message.matches(".*[§&][0-9a-fk-or].*");
    }

    /**
     * Converte códigos de cor & para §
     */
    public static String colorize(String message) {
        return message.replaceAll("&([0-9a-fk-or])", "§$1");
    }
}

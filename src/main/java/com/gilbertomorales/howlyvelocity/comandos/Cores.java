package com.gilbertomorales.howlyvelocity.comandos;

public class Cores {
    
    public static String colorir(String texto) {
        if (texto == null) return "";
        
        return texto.replace("&0", "§0")
                   .replace("&1", "§1")
                   .replace("&2", "§2")
                   .replace("&3", "§3")
                   .replace("&4", "§4")
                   .replace("&5", "§5")
                   .replace("&6", "§6")
                   .replace("&7", "§7")
                   .replace("&8", "§8")
                   .replace("&9", "§9")
                   .replace("&a", "§a")
                   .replace("&b", "§b")
                   .replace("&c", "§c")
                   .replace("&d", "§d")
                   .replace("&e", "§e")
                   .replace("&f", "§f")
                   .replace("&k", "§k")
                   .replace("&l", "§l")
                   .replace("&m", "§m")
                   .replace("&n", "§n")
                   .replace("&o", "§o")
                   .replace("&r", "§r");
    }
}

package com.gilbertomorales.howlyvelocity.utils;

/**
 * Utilitário para cores ANSI em logs
 */
public class LogColor {
    // Cores
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // Cores brilhantes
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_PURPLE = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    
    // Estilos
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    
    /**
     * Formata uma mensagem com prefixo colorido
     * @param prefix O prefixo da mensagem
     * @param prefixColor A cor do prefixo
     * @param message A mensagem
     * @return A mensagem formatada
     */
    public static String format(String prefix, String prefixColor, String message) {
        return prefixColor + "[" + prefix + "] " + WHITE + message + RESET;
    }
    
    /**
     * Formata uma mensagem de sucesso
     * @param prefix O prefixo da mensagem
     * @param message A mensagem
     * @return A mensagem formatada
     */
    public static String success(String prefix, String message) {
        return format(prefix, GREEN, message);
    }
    
    /**
     * Formata uma mensagem de erro
     * @param prefix O prefixo da mensagem
     * @param message A mensagem
     * @return A mensagem formatada
     */
    public static String error(String prefix, String message) {
        return format(prefix, RED, message);
    }
    
    /**
     * Formata uma mensagem de aviso
     * @param prefix O prefixo da mensagem
     * @param message A mensagem
     * @return A mensagem formatada
     */
    public static String warning(String prefix, String message) {
        return format(prefix, YELLOW, message);
    }
    
    /**
     * Formata uma mensagem de informação
     * @param prefix O prefixo da mensagem
     * @param message A mensagem
     * @return A mensagem formatada
     */
    public static String info(String prefix, String message) {
        return format(prefix, BLUE, message);
    }
}

package com.gilbertomorales.howlyvelocity.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Converte milissegundos para uma string legível
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "Permanente";
        }

        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(" dia").append(days > 1 ? "s" : "");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(" hora").append(hours > 1 ? "s" : "");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(" minuto").append(minutes > 1 ? "s" : "");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(" segundo").append(seconds > 1 ? "s" : "");
        }

        return sb.length() > 0 ? sb.toString() : "Menos de 1 segundo";
    }

    /**
     * Converte timestamp para data formatada
     */
    public static String formatTimestamp(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(FORMATTER);
    }

    /**
     * Converte string de tempo para milissegundos
     * Formato: 1d2h3m4s (dias, horas, minutos, segundos)
     */
    public static long parseTimeString(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }

        timeString = timeString.toLowerCase().trim();
        
        if (timeString.equals("perm") || timeString.equals("permanente")) {
            return -1; // Permanente
        }

        long totalMillis = 0;
        StringBuilder currentNumber = new StringBuilder();

        for (char c : timeString.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else {
                if (currentNumber.length() > 0) {
                    long number = Long.parseLong(currentNumber.toString());
                    
                    switch (c) {
                        case 's' -> totalMillis += TimeUnit.SECONDS.toMillis(number);
                        case 'm' -> totalMillis += TimeUnit.MINUTES.toMillis(number);
                        case 'h' -> totalMillis += TimeUnit.HOURS.toMillis(number);
                        case 'd' -> totalMillis += TimeUnit.DAYS.toMillis(number);
                        case 'w' -> totalMillis += TimeUnit.DAYS.toMillis(number * 7);
                    }
                    
                    currentNumber.setLength(0);
                }
            }
        }

        return totalMillis;
    }

    /**
     * Verifica se um timestamp já expirou
     */
    public static boolean isExpired(long timestamp) {
        if (timestamp <= 0) {
            return false; // Permanente
        }
        return System.currentTimeMillis() > timestamp;
    }

    /**
     * Calcula o tempo restante até expirar
     */
    public static long getTimeRemaining(long expirationTime) {
        if (expirationTime <= 0) {
            return -1; // Permanente
        }
        
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}

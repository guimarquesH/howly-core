package com.gilbertomorales.howlyvelocity.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    /**
     * Converte uma duração em milissegundos para uma string legível
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0 segundos";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " dia" : " dias");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hora" : " horas");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");
        }
        if (seconds > 0 && days == 0) { // Só mostrar segundos se não tiver dias
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " segundo" : " segundos");
        }

        return sb.toString();
    }

    /**
     * Converte uma string de tempo para milissegundos
     * Formatos aceitos: 1s, 1m, 1h, 1d, 1w, 1M
     */
    public static Long parseDuration(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        // Padrão para capturar número + unidade
        Pattern pattern = Pattern.compile("(\\d+)([smhdwM])");
        Matcher matcher = pattern.matcher(timeString.toLowerCase());

        long totalMillis = 0;

        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s" -> totalMillis += TimeUnit.SECONDS.toMillis(value);
                case "m" -> totalMillis += TimeUnit.MINUTES.toMillis(value);
                case "h" -> totalMillis += TimeUnit.HOURS.toMillis(value);
                case "d" -> totalMillis += TimeUnit.DAYS.toMillis(value);
                case "w" -> totalMillis += TimeUnit.DAYS.toMillis(value * 7);
                case "M" -> totalMillis += TimeUnit.DAYS.toMillis(value * 30); // Aproximadamente 30 dias
                default -> {
                    return null;
                }
            }
        }

        return totalMillis > 0 ? totalMillis : null;
    }

    /**
     * Formata um timestamp para uma data legível
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Calcula o tempo decorrido desde um timestamp
     */
    public static String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;

        if (diff < 0) {
            return "no futuro";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        if (days > 0) {
            return days + (days == 1 ? " dia atrás" : " dias atrás");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hora atrás" : " horas atrás");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minuto atrás" : " minutos atrás");
        } else {
            return "agora mesmo";
        }
    }
}

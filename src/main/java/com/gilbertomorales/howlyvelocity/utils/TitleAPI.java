package com.gilbertomorales.howlyvelocity.utils;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;

public class TitleAPI {

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 1, 3, 1);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Component titleComponent = Tag.colorize(title);
        Component subtitleComponent = Tag.colorize(subtitle);
        
        Title titleObj = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofSeconds(fadeIn),
                Duration.ofSeconds(stay),
                Duration.ofSeconds(fadeOut)
            )
        );
        
        player.showTitle(titleObj);
    }

    public static void sendActionBar(Player player, String message) {
        Component component = Tag.colorize(message);
        player.sendActionBar(component);
    }

    public static void clearTitle(Player player) {
        player.clearTitle();
    }

    public static void resetTitle(Player player) {
        player.resetTitle();
    }
}

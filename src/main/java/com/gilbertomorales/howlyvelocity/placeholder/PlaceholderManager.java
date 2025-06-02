package com.gilbertomorales.howlyvelocity.placeholder;

import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class PlaceholderManager {

    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final Map<String, Function<Player, String>> placeholders = new HashMap<>();

    public PlaceholderManager(TagManager tagManager, MedalManager medalManager) {
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        
        // Registrar placeholders padrão
        registerDefaultPlaceholders();
    }

    private void registerDefaultPlaceholders() {
        // Placeholder para tag
        placeholders.put("howly.tag", player -> tagManager.getPlayerTag(player));
        
        // Placeholder para medalha
        placeholders.put("howly.medalha", player -> medalManager.getPlayerMedal(player));
    }

    public String replacePlaceholders(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        for (Map.Entry<String, Function<Player, String>> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                String value = entry.getValue().apply(player);
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }

    public void registerPlaceholder(String key, Function<Player, String> valueProvider) {
        placeholders.put(key, valueProvider);
    }

    public boolean hasPlaceholder(String key) {
        return placeholders.containsKey(key);
    }

    public String getPlaceholderValue(String key, Player player) {
        Function<Player, String> valueProvider = placeholders.get(key);
        return valueProvider != null ? valueProvider.apply(player) : null;
    }
}

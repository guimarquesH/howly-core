package com.gilbertomorales.howlyvelocity.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class TagManager {

    private final Path dataDirectory;
    private final File tagsFile;
    private final Gson gson;
    
    // Lista de tags disponíveis com suas permissões
    private final Map<String, TagInfo> availableTags = new LinkedHashMap<>();
    
    // Tags selecionadas pelos jogadores
    private final Map<UUID, String> playerTags = new HashMap<>();

    public TagManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.tagsFile = new File(dataDirectory.toFile(), "tags.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Inicializar tags padrão
        initDefaultTags();
    }
    
    private void initDefaultTags() {
        availableTags.put("nenhuma", new TagInfo("", "", "§7"));
        availableTags.put("dono", new TagInfo("§4§l[DONO]", "howly.dono", "§4"));
        availableTags.put("admin", new TagInfo("§c§l[ADMIN]", "howly.admin", "§c"));
        availableTags.put("mod", new TagInfo("§2§l[MOD]", "howly.moderador", "§2"));
        availableTags.put("helper", new TagInfo("§a§l[HELPER]", "howly.helper", "§a"));
        availableTags.put("vip", new TagInfo("§6§l[VIP]", "howly.vip", "§6"));
        availableTags.put("membro", new TagInfo("§7[MEMBRO]", "", "§7"));
    }

    public void loadTags() {
        try {
            if (!tagsFile.exists()) {
                saveTags();
                return;
            }

            try (Reader reader = new FileReader(tagsFile)) {
                Type tagMapType = new TypeToken<Map<String, TagInfo>>() {}.getType();
                Map<String, TagInfo> loadedTags = gson.fromJson(reader, tagMapType);
                
                if (loadedTags != null) {
                    availableTags.clear();
                    availableTags.putAll(loadedTags);
                }
            }
            
            // Carregar tags dos jogadores
            File playerTagsFile = new File(dataDirectory.toFile(), "player_tags.json");
            if (playerTagsFile.exists()) {
                try (Reader reader = new FileReader(playerTagsFile)) {
                    Type playerTagMapType = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> loadedPlayerTags = gson.fromJson(reader, playerTagMapType);
                    
                    if (loadedPlayerTags != null) {
                        playerTags.clear();
                        loadedPlayerTags.forEach((uuidStr, tag) -> {
                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                playerTags.put(uuid, tag);
                            } catch (IllegalArgumentException e) {
                                // Ignorar UUIDs inválidos
                            }
                        });
                    }
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveTags() {
        try {
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            try (Writer writer = new FileWriter(tagsFile)) {
                gson.toJson(availableTags, writer);
            }
            
            // Salvar tags dos jogadores
            File playerTagsFile = new File(dataDirectory.toFile(), "player_tags.json");
            try (Writer writer = new FileWriter(playerTagsFile)) {
                Map<String, String> saveMap = new HashMap<>();
                playerTags.forEach((uuid, tag) -> saveMap.put(uuid.toString(), tag));
                gson.toJson(saveMap, writer);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerTag(Player player) {
        // Verificar se o jogador tem uma tag selecionada
        String selectedTag = playerTags.get(player.getUniqueId());
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);
            
            // Verificar se o jogador tem permissão para usar esta tag
            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                return tagInfo.display;
            }
        }

        // Se não tiver tag selecionada ou não tiver permissão, usar tag padrão (membro)
        return "§7[MEMBRO]";
    }

    public String getFormattedPlayerName(Player player) {
        String tag = getPlayerTag(player);
        return tag.isEmpty() ? "§f" + player.getUsername() : tag + " §f" + player.getUsername();
    }

    public String getPlayerNameColor(Player player) {
        // Verificar se o jogador tem uma tag selecionada
        String selectedTag = playerTags.get(player.getUniqueId());
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);
            
            // Verificar se o jogador tem permissão para usar esta tag
            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                return tagInfo.nameColor;
            }
        }

        // Cor padrão se nada for encontrado
        return "§7";
    }

    public void setPlayerTag(UUID uuid, String tagId) {
        playerTags.put(uuid, tagId);
        saveTags(); // Salvar imediatamente
    }

    public void removePlayerTag(UUID uuid) {
        playerTags.remove(uuid);
        saveTags(); // Salvar imediatamente
    }

    public Map<String, TagInfo> getAvailableTags() {
        return Collections.unmodifiableMap(availableTags);
    }

    public List<String> getPlayerAvailableTags(Player player) {
        List<String> availableTags = new ArrayList<>();
        
        for (Map.Entry<String, TagInfo> entry : this.availableTags.entrySet()) {
            String tagId = entry.getKey();
            TagInfo tagInfo = entry.getValue();
            
            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                availableTags.add(tagId);
            }
        }
        
        return availableTags;
    }

    public boolean hasTag(String tagId) {
        return availableTags.containsKey(tagId);
    }

    public TagInfo getTagInfo(String tagId) {
        return availableTags.get(tagId);
    }

    public static class TagInfo {
        private final String display;
        private final String permission;
        private final String nameColor;

        public TagInfo(String display, String permission, String nameColor) {
            this.display = display;
            this.permission = permission;
            this.nameColor = nameColor;
        }

        public String getDisplay() {
            return display;
        }

        public String getPermission() {
            return permission;
        }

        public String getNameColor() {
            return nameColor;
        }
    }
}

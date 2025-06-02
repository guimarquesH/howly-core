package com.gilbertomorales.howlyvelocity.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.velocitypowered.api.proxy.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class MedalManager {

    private final Path dataDirectory;
    private final File medalsFile;
    private final File playerMedalsFile;
    private final Gson gson;
    
    // Lista de medalhas disponíveis com suas permissões
    private final Map<String, MedalInfo> availableMedals = new LinkedHashMap<>();
    
    // Medalhas selecionadas pelos jogadores
    private final Map<UUID, String> playerMedals = new HashMap<>();

    public MedalManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.medalsFile = new File(dataDirectory.toFile(), "medals.json");
        this.playerMedalsFile = new File(dataDirectory.toFile(), "player_medals.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Inicializar medalhas padrão
        initDefaultMedals();
    }
    
    private void initDefaultMedals() {
        availableMedals.put("nenhuma", new MedalInfo("", "", ""));
        availableMedals.put("estrela", new MedalInfo("✧", "howly.medal.estrela", "§e"));
        availableMedals.put("coracao", new MedalInfo("♥", "howly.medal.coracao", "§c"));
        availableMedals.put("coroa", new MedalInfo("♔", "howly.medal.coroa", "§6"));
        availableMedals.put("cafe", new MedalInfo("☕", "howly.medal.cafe", "§6"));
        availableMedals.put("sol", new MedalInfo("☀", "howly.medal.sol", "§e"));
        availableMedals.put("lua", new MedalInfo("☽", "howly.medal.lua", "§9"));
        availableMedals.put("musica", new MedalInfo("♪", "howly.medal.musica", "§d"));
        availableMedals.put("flor", new MedalInfo("✿", "howly.medal.flor", "§a"));
        availableMedals.put("raio", new MedalInfo("⚡", "howly.medal.raio", "§e"));
        availableMedals.put("diamante", new MedalInfo("✦", "howly.medal.diamante", "§b"));
    }

    public void loadMedals() {
        try {
            if (!medalsFile.exists()) {
                saveMedals();
                return;
            }

            try (Reader reader = new FileReader(medalsFile)) {
                Type medalMapType = new TypeToken<Map<String, MedalInfo>>() {}.getType();
                Map<String, MedalInfo> loadedMedals = gson.fromJson(reader, medalMapType);
                
                if (loadedMedals != null) {
                    availableMedals.clear();
                    availableMedals.putAll(loadedMedals);
                }
            }
            
            // Carregar medalhas dos jogadores
            if (playerMedalsFile.exists()) {
                try (Reader reader = new FileReader(playerMedalsFile)) {
                    Type playerMedalMapType = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> loadedPlayerMedals = gson.fromJson(reader, playerMedalMapType);
                    
                    if (loadedPlayerMedals != null) {
                        playerMedals.clear();
                        loadedPlayerMedals.forEach((uuidStr, medal) -> {
                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                playerMedals.put(uuid, medal);
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

    public void saveMedals() {
        try {
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            try (Writer writer = new FileWriter(medalsFile)) {
                gson.toJson(availableMedals, writer);
            }
            
            // Salvar medalhas dos jogadores
            try (Writer writer = new FileWriter(playerMedalsFile)) {
                Map<String, String> saveMap = new HashMap<>();
                playerMedals.forEach((uuid, medal) -> saveMap.put(uuid.toString(), medal));
                gson.toJson(saveMap, writer);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPlayerMedal(Player player) {
        // Verificar se o jogador tem uma medalha selecionada
        String selectedMedal = playerMedals.get(player.getUniqueId());
        if (selectedMedal != null && availableMedals.containsKey(selectedMedal)) {
            MedalInfo medalInfo = availableMedals.get(selectedMedal);
            
            // Verificar se o jogador tem permissão para usar esta medalha
            if (medalInfo.permission.isEmpty() || player.hasPermission(medalInfo.permission)) {
                return medalInfo.color + medalInfo.symbol;
            }
        }

        // Se não tiver medalha selecionada ou não tiver permissão, retornar vazio (nenhuma medalha)
        return "";
    }

    public String getFormattedPlayerMedal(Player player) {
        String medal = getPlayerMedal(player);
        return medal.isEmpty() ? "" : "[" + medal + "§f] ";
    }

    public void setPlayerMedal(UUID uuid, String medalId) {
        playerMedals.put(uuid, medalId);
        saveMedals(); // Salvar imediatamente
    }

    public void removePlayerMedal(UUID uuid) {
        playerMedals.remove(uuid);
        saveMedals(); // Salvar imediatamente
    }

    public Map<String, MedalInfo> getAvailableMedals() {
        return Collections.unmodifiableMap(availableMedals);
    }

    public List<String> getPlayerAvailableMedals(Player player) {
        List<String> availableMedals = new ArrayList<>();
        
        for (Map.Entry<String, MedalInfo> entry : this.availableMedals.entrySet()) {
            String medalId = entry.getKey();
            MedalInfo medalInfo = entry.getValue();
            
            if (medalInfo.permission.isEmpty() || player.hasPermission(medalInfo.permission)) {
                availableMedals.add(medalId);
            }
        }
        
        return availableMedals;
    }

    public boolean hasMedal(String medalId) {
        return availableMedals.containsKey(medalId);
    }

    public MedalInfo getMedalInfo(String medalId) {
        return availableMedals.get(medalId);
    }

    public static class MedalInfo {
        private final String symbol;
        private final String permission;
        private final String color;

        public MedalInfo(String symbol, String permission, String color) {
            this.symbol = symbol;
            this.permission = permission;
            this.color = color;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getPermission() {
            return permission;
        }

        public String getColor() {
            return color;
        }
        
        public String getColoredSymbol() {
            return color + symbol;
        }
    }
}

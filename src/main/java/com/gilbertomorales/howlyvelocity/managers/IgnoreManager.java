package com.gilbertomorales.howlyvelocity.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IgnoreManager {

    private final Path dataDirectory;
    private final File ignoreFile;
    private final Gson gson;

    // Map de UUID do jogador -> Set de UUIDs ignorados
    private final Map<UUID, Set<UUID>> playerIgnoreList = new ConcurrentHashMap<>();

    public IgnoreManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.ignoreFile = new File(dataDirectory.toFile(), "player_ignores.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void loadIgnoreData() {
        try {
            if (!ignoreFile.exists()) {
                saveIgnoreData();
                return;
            }

            try (Reader reader = new FileReader(ignoreFile)) {
                Type ignoreMapType = new TypeToken<Map<String, Set<String>>>() {}.getType();
                Map<String, Set<String>> loadedIgnores = gson.fromJson(reader, ignoreMapType);

                if (loadedIgnores != null) {
                    playerIgnoreList.clear();
                    loadedIgnores.forEach((playerUuidStr, ignoredUuidStrs) -> {
                        try {
                            UUID playerUuid = UUID.fromString(playerUuidStr);
                            Set<UUID> ignoredUuids = new HashSet<>();

                            for (String ignoredUuidStr : ignoredUuidStrs) {
                                try {
                                    ignoredUuids.add(UUID.fromString(ignoredUuidStr));
                                } catch (IllegalArgumentException e) {
                                    // Ignorar UUIDs inválidos
                                }
                            }

                            playerIgnoreList.put(playerUuid, ignoredUuids);
                        } catch (IllegalArgumentException e) {
                            // Ignorar UUIDs inválidos
                        }
                    });
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveIgnoreData() {
        try {
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            try (Writer writer = new FileWriter(ignoreFile)) {
                Map<String, Set<String>> saveMap = new HashMap<>();
                playerIgnoreList.forEach((playerUuid, ignoredUuids) -> {
                    Set<String> ignoredUuidStrs = new HashSet<>();
                    ignoredUuids.forEach(uuid -> ignoredUuidStrs.add(uuid.toString()));
                    saveMap.put(playerUuid.toString(), ignoredUuidStrs);
                });
                gson.toJson(saveMap, writer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adiciona um jogador à lista de ignorados
     */
    public boolean addIgnoredPlayer(UUID playerUuid, UUID ignoredUuid) {
        if (playerUuid.equals(ignoredUuid)) {
            return false; // Não pode ignorar a si mesmo
        }

        Set<UUID> ignoredPlayers = playerIgnoreList.computeIfAbsent(playerUuid, k -> new HashSet<>());
        boolean added = ignoredPlayers.add(ignoredUuid);

        if (added) {
            saveIgnoreData();
        }

        return added;
    }

    /**
     * Remove um jogador da lista de ignorados
     */
    public boolean removeIgnoredPlayer(UUID playerUuid, UUID ignoredUuid) {
        Set<UUID> ignoredPlayers = playerIgnoreList.get(playerUuid);
        if (ignoredPlayers == null) {
            return false;
        }

        boolean removed = ignoredPlayers.remove(ignoredUuid);

        if (removed) {
            if (ignoredPlayers.isEmpty()) {
                playerIgnoreList.remove(playerUuid);
            }
            saveIgnoreData();
        }

        return removed;
    }

    /**
     * Verifica se um jogador está ignorando outro
     */
    public boolean isIgnoring(UUID playerUuid, UUID targetUuid) {
        Set<UUID> ignoredPlayers = playerIgnoreList.get(playerUuid);
        return ignoredPlayers != null && ignoredPlayers.contains(targetUuid);
    }

    /**
     * Obtém a lista de jogadores ignorados por um jogador
     */
    public Set<UUID> getIgnoredPlayers(UUID playerUuid) {
        return playerIgnoreList.getOrDefault(playerUuid, new HashSet<>());
    }

    /**
     * Limpa toda a lista de ignorados de um jogador
     */
    public void clearIgnoreList(UUID playerUuid) {
        playerIgnoreList.remove(playerUuid);
        saveIgnoreData();
    }
}

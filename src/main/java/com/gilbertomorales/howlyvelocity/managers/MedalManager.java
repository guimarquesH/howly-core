package com.gilbertomorales.howlyvelocity.managers;

import com.velocitypowered.api.proxy.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MedalManager {

    private final DatabaseManager databaseManager;

    // Cache em memória para performance
    private final Map<String, MedalInfo> availableMedals = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerMedals = new ConcurrentHashMap<>();

    public MedalManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        initDefaultMedals();
    }

    private void initDefaultMedals() {
        // Medalhas padrão serão carregadas do banco de dados
        availableMedals.put("nenhuma", new MedalInfo("", "", ""));
    }

    public void loadMedals() {
        CompletableFuture.runAsync(() -> {
            try {
                loadAvailableMedalsFromDB();
                loadPlayerMedalsFromDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadAvailableMedalsFromDB() throws SQLException {
        String sql = "SELECT medal_id, symbol, permission, color FROM available_medals";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            availableMedals.clear();
            while (rs.next()) {
                String medalId = rs.getString("medal_id");
                String symbol = rs.getString("symbol");
                String permission = rs.getString("permission");
                String color = rs.getString("color");

                availableMedals.put(medalId, new MedalInfo(symbol, permission, color));
            }
        }
    }

    private void loadPlayerMedalsFromDB() throws SQLException {
        String sql = "SELECT player_uuid, medal_id FROM player_medals";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            playerMedals.clear();
            while (rs.next()) {
                try {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    String medalId = rs.getString("medal_id");
                    playerMedals.put(playerUuid, medalId);
                } catch (IllegalArgumentException e) {
                    // Ignorar UUIDs inválidos
                }
            }
        }
    }

    public void saveMedals() {
        CompletableFuture.runAsync(() -> {
            try {
                saveAvailableMedalsToDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveAvailableMedalsToDB() throws SQLException {
        String sql;
        if (databaseManager.isMySQL()) {
            sql = "INSERT INTO available_medals (medal_id, symbol, permission, color, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "symbol = VALUES(symbol), " +
                    "permission = VALUES(permission), " +
                    "color = VALUES(color), " +
                    "updated_at = VALUES(updated_at)";
        } else {
            sql = "INSERT OR REPLACE INTO available_medals (medal_id, symbol, permission, color, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, MedalInfo> entry : availableMedals.entrySet()) {
                stmt.setString(1, entry.getKey());
                stmt.setString(2, entry.getValue().getSymbol());
                stmt.setString(3, entry.getValue().getPermission());
                stmt.setString(4, entry.getValue().getColor());
                stmt.setLong(5, currentTime);
                stmt.setLong(6, currentTime);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Retorna apenas a medalha do jogador (sem espaços)
     */
    public String getPlayerMedal(Player player) {
        String selectedMedal = playerMedals.get(player.getUniqueId());
        if (selectedMedal != null && availableMedals.containsKey(selectedMedal)) {
            MedalInfo medalInfo = availableMedals.get(selectedMedal);

            if (medalInfo.permission.isEmpty() || player.hasPermission(medalInfo.permission)) {
                return medalInfo.color + medalInfo.symbol;
            }
        }
        return "";
    }

    /**
     * Retorna a medalha formatada com espaço APENAS se o jogador tiver medalha
     */
    public String getFormattedPlayerMedal(Player player) {
        String medal = getPlayerMedal(player);
        return medal.isEmpty() ? "" : medal + " ";
    }

    /**
     * Obtém a medalha atual do jogador (ID da medalha, não o símbolo)
     */
    public String getCurrentPlayerMedal(UUID uuid) {
        return playerMedals.get(uuid);
    }

    public void setPlayerMedal(UUID uuid, String medalId) {
        playerMedals.put(uuid, medalId);
        setPlayerMedalInDB(uuid, medalId);
    }

    private void setPlayerMedalInDB(UUID uuid, String medalId) {
        CompletableFuture.runAsync(() -> {
            String sql;
            if (databaseManager.isMySQL()) {
                sql = "INSERT INTO player_medals (player_uuid, medal_id, updated_at) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "medal_id = VALUES(medal_id), " +
                        "updated_at = VALUES(updated_at)";
            } else {
                sql = "INSERT OR REPLACE INTO player_medals (player_uuid, medal_id, updated_at) " +
                        "VALUES (?, ?, ?)";
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, medalId);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removePlayerMedal(UUID uuid) {
        playerMedals.remove(uuid);
        removePlayerMedalFromDB(uuid);
    }

    private void removePlayerMedalFromDB(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_medals WHERE player_uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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

    public void addAvailableMedal(String medalId, String symbol, String permission, String color) {
        availableMedals.put(medalId, new MedalInfo(symbol, permission, color));
        saveMedals();
    }

    public void removeAvailableMedal(String medalId) {
        availableMedals.remove(medalId);
        saveMedals();
    }

    public CompletableFuture<Void> migrateFromFilesToDatabase() {
        return CompletableFuture.runAsync(() -> {
            // Migração já foi feita, dados estão no banco
        });
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

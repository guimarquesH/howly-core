package com.gilbertomorales.howlyvelocity.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {

    private final DatabaseManager databaseManager;

    public PlayerDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Salva ou atualiza os dados de um jogador no banco de dados
     */
    public CompletableFuture<Void> savePlayerData(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_data (uuid, username, first_join, last_join) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE username = VALUES(username), last_join = VALUES(last_join)";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                long currentTime = System.currentTimeMillis();

                stmt.setString(1, uuid.toString());
                stmt.setString(2, username);
                stmt.setLong(3, currentTime);
                stmt.setLong(4, currentTime);

                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Busca o UUID de um jogador pelo nome
     */
    public CompletableFuture<UUID> getPlayerUUID(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM player_data WHERE username = ? ORDER BY last_join DESC LIMIT 1";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("uuid"));
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Busca o nome de um jogador pelo UUID
     */
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT username FROM player_data WHERE uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Verifica se um jogador existe no banco de dados
     */
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM player_data WHERE uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return false;
        });
    }

    /**
     * Obtém dados completos de um jogador
     */
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid, username, first_join, last_join FROM player_data WHERE uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new PlayerData(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getLong("first_join"),
                                rs.getLong("last_join")
                        );
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Classe para representar dados de um jogador
     */
    public static class PlayerData {
        private final UUID uuid;
        private final String username;
        private final long firstJoin;
        private final long lastJoin;

        public PlayerData(UUID uuid, String username, long firstJoin, long lastJoin) {
            this.uuid = uuid;
            this.username = username;
            this.firstJoin = firstJoin;
            this.lastJoin = lastJoin;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getUsername() {
            return username;
        }

        public long getFirstJoin() {
            return firstJoin;
        }

        public long getLastJoin() {
            return lastJoin;
        }
    }
}
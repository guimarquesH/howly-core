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

    public CompletableFuture<Void> savePlayerData(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = databaseManager.isMySQL() ?
                    "INSERT INTO players (uuid, username, first_join, last_join) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE username = VALUES(username), last_join = VALUES(last_join)" :
                    "INSERT OR REPLACE INTO players (uuid, username, first_join, last_join) " +
                    "VALUES (?, ?, COALESCE((SELECT first_join FROM players WHERE uuid = ?), ?), ?)";

                long currentTime = System.currentTimeMillis();

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.setLong(3, currentTime);
                    stmt.setLong(4, currentTime);
                    
                    if (!databaseManager.isMySQL()) {
                        stmt.setString(5, uuid.toString());
                        stmt.setLong(6, currentTime);
                        stmt.setLong(7, currentTime);
                    }
                    
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao salvar dados do jogador", e);
            }
        });
    }

    public CompletableFuture<String> getPlayerName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT username FROM players WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("username");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao buscar nome do jogador", e);
            }
            return null;
        });
    }

    public CompletableFuture<UUID> getPlayerUUID(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT uuid FROM players WHERE username = ? ORDER BY last_join DESC LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, username);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return UUID.fromString(rs.getString("uuid"));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao buscar UUID do jogador", e);
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao verificar existência do jogador", e);
            }
        });
    }
}

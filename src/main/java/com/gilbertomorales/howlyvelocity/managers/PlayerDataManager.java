package com.gilbertomorales.howlyvelocity.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final DatabaseManager databaseManager;

    // Cache para melhorar performance
    private final ConcurrentHashMap<String, UUID> nameToUuidCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> uuidToNameCache = new ConcurrentHashMap<>();

    public PlayerDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        createPlayerTable();
    }

    private void createPlayerTable() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "CREATE TABLE IF NOT EXISTS players (" +
                                 "uuid VARCHAR(36) PRIMARY KEY, " +
                                 "name VARCHAR(16) NOT NULL, " +
                                 "first_join BIGINT NOT NULL, " +
                                 "last_join BIGINT NOT NULL, " +
                                 "INDEX idx_name (name))"
                 )) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> updatePlayerData(UUID uuid, String name) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                long currentTime = System.currentTimeMillis();

                // Verificar se o jogador já existe
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT uuid FROM players WHERE uuid = ?")) {
                    checkStmt.setString(1, uuid.toString());
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        // Atualizar jogador existente
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE players SET name = ?, last_join = ? WHERE uuid = ?")) {
                            updateStmt.setString(1, name);
                            updateStmt.setLong(2, currentTime);
                            updateStmt.setString(3, uuid.toString());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Inserir novo jogador
                        try (PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO players (uuid, name, first_join, last_join) VALUES (?, ?, ?, ?)")) {
                            insertStmt.setString(1, uuid.toString());
                            insertStmt.setString(2, name);
                            insertStmt.setLong(3, currentTime);
                            insertStmt.setLong(4, currentTime);
                            insertStmt.executeUpdate();
                        }
                    }
                }

                // Atualizar cache
                nameToUuidCache.put(name.toLowerCase(), uuid);
                uuidToNameCache.put(uuid, name);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Busca o UUID de um jogador pelo nome
     * @param name Nome do jogador
     * @return CompletableFuture com o UUID ou null se não encontrado
     */
    public CompletableFuture<UUID> getPlayerUUID(String name) {
        // Verificar cache primeiro
        UUID cachedUUID = nameToUuidCache.get(name.toLowerCase());
        if (cachedUUID != null) {
            return CompletableFuture.completedFuture(cachedUUID);
        }

        // Buscar no banco de dados
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT uuid FROM players WHERE LOWER(name) = LOWER(?)")) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    // Atualizar cache
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    return uuid;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Busca o nome de um jogador pelo UUID
     * @param uuid UUID do jogador
     * @return CompletableFuture com o nome ou null se não encontrado
     */
    public CompletableFuture<String> getPlayerName(UUID uuid) {
        // Verificar cache primeiro
        String cachedName = uuidToNameCache.get(uuid);
        if (cachedName != null) {
            return CompletableFuture.completedFuture(cachedName);
        }

        // Buscar no banco de dados
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT name FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("name");
                    // Atualizar cache
                    uuidToNameCache.put(uuid, name);
                    return name;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Busca informações de login de um jogador
     * @param uuid UUID do jogador
     * @return CompletableFuture com array [firstJoin, lastJoin] ou null se não encontrado
     */
    public CompletableFuture<long[]> getPlayerLoginInfo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT first_join, last_join FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    long firstJoin = rs.getLong("first_join");
                    long lastJoin = rs.getLong("last_join");
                    return new long[]{firstJoin, lastJoin};
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}

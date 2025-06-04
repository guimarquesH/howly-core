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
    private final ConcurrentHashMap<Integer, UUID> idToUuidCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> uuidToIdCache = new ConcurrentHashMap<>();

    public PlayerDataManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> updatePlayerData(UUID uuid, String name) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                long currentTime = System.currentTimeMillis();

                // Verificar se o jogador já existe
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT id, uuid FROM players WHERE uuid = ?")) {
                    checkStmt.setString(1, uuid.toString());
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        // Atualizar jogador existente
                        int playerId = rs.getInt("id");
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE players SET name = ?, last_join = ? WHERE uuid = ?")) {
                            updateStmt.setString(1, name);
                            updateStmt.setLong(2, currentTime);
                            updateStmt.setString(3, uuid.toString());
                            updateStmt.executeUpdate();
                        }
                        
                        // Atualizar cache
                        uuidToIdCache.put(uuid, playerId);
                        idToUuidCache.put(playerId, uuid);
                    } else {
                        // Inserir novo jogador
                        try (PreparedStatement insertStmt = conn.prepareStatement(
                                "INSERT INTO players (uuid, name, first_join, last_join) VALUES (?, ?, ?, ?)",
                                PreparedStatement.RETURN_GENERATED_KEYS)) {
                            insertStmt.setString(1, uuid.toString());
                            insertStmt.setString(2, name);
                            insertStmt.setLong(3, currentTime);
                            insertStmt.setLong(4, currentTime);
                            insertStmt.executeUpdate();
                            
                            // Obter o ID gerado
                            try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    int playerId = generatedKeys.getInt(1);
                                    // Atualizar cache
                                    uuidToIdCache.put(uuid, playerId);
                                    idToUuidCache.put(playerId, uuid);
                                }
                            }
                        }
                    }
                }

                // Atualizar cache de nomes
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
                         "SELECT uuid, id FROM players WHERE LOWER(name) = LOWER(?)")) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int playerId = rs.getInt("id");
                    
                    // Atualizar cache
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    uuidToIdCache.put(uuid, playerId);
                    idToUuidCache.put(playerId, uuid);
                    
                    return uuid;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Busca o UUID de um jogador pelo ID
     * @param playerId ID do jogador
     * @return CompletableFuture com o UUID ou null se não encontrado
     */
    public CompletableFuture<UUID> getPlayerUUIDById(int playerId) {
        // Verificar cache primeiro
        UUID cachedUUID = idToUuidCache.get(playerId);
        if (cachedUUID != null) {
            return CompletableFuture.completedFuture(cachedUUID);
        }

        // Buscar no banco de dados
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT uuid, name FROM players WHERE id = ?")) {
                stmt.setInt(1, playerId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    
                    // Atualizar cache
                    idToUuidCache.put(playerId, uuid);
                    uuidToIdCache.put(uuid, playerId);
                    uuidToNameCache.put(uuid, name);
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
     * Busca o ID de um jogador pelo UUID
     * @param uuid UUID do jogador
     * @return CompletableFuture com o ID ou null se não encontrado
     */
    public CompletableFuture<Integer> getPlayerId(UUID uuid) {
        // Verificar cache primeiro
        Integer cachedId = uuidToIdCache.get(uuid);
        if (cachedId != null) {
            return CompletableFuture.completedFuture(cachedId);
        }

        // Buscar no banco de dados
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, name FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int playerId = rs.getInt("id");
                    String name = rs.getString("name");
                    
                    // Atualizar cache
                    uuidToIdCache.put(uuid, playerId);
                    idToUuidCache.put(playerId, uuid);
                    uuidToNameCache.put(uuid, name);
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    
                    return playerId;
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
                         "SELECT name, id FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("name");
                    int playerId = rs.getInt("id");
                    
                    // Atualizar cache
                    uuidToNameCache.put(uuid, name);
                    uuidToIdCache.put(uuid, playerId);
                    idToUuidCache.put(playerId, uuid);
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    
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

    /**
     * Busca informações completas de um jogador pelo ID
     * @param playerId ID do jogador
     * @return CompletableFuture com PlayerInfo ou null se não encontrado
     */
    public CompletableFuture<PlayerInfo> getPlayerInfoById(int playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, uuid, name, first_join, last_join FROM players WHERE id = ?")) {
                stmt.setInt(1, playerId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    long firstJoin = rs.getLong("first_join");
                    long lastJoin = rs.getLong("last_join");
                    
                    // Atualizar cache
                    uuidToIdCache.put(uuid, playerId);
                    idToUuidCache.put(playerId, uuid);
                    uuidToNameCache.put(uuid, name);
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    
                    return new PlayerInfo(playerId, uuid, name, firstJoin, lastJoin);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Busca informações completas de um jogador pelo UUID
     * @param uuid UUID do jogador
     * @return CompletableFuture com PlayerInfo ou null se não encontrado
     */
    public CompletableFuture<PlayerInfo> getPlayerInfo(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, uuid, name, first_join, last_join FROM players WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    int playerId = rs.getInt("id");
                    String name = rs.getString("name");
                    long firstJoin = rs.getLong("first_join");
                    long lastJoin = rs.getLong("last_join");
                    
                    // Atualizar cache
                    uuidToIdCache.put(uuid, playerId);
                    idToUuidCache.put(playerId, uuid);
                    uuidToNameCache.put(uuid, name);
                    nameToUuidCache.put(name.toLowerCase(), uuid);
                    
                    return new PlayerInfo(playerId, uuid, name, firstJoin, lastJoin);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return null;
        });
    }

    /**
     * Classe para armazenar informações completas do jogador
     */
    public static class PlayerInfo {
        private final int id;
        private final UUID uuid;
        private final String name;
        private final long firstJoin;
        private final long lastJoin;

        public PlayerInfo(int id, UUID uuid, String name, long firstJoin, long lastJoin) {
            this.id = id;
            this.uuid = uuid;
            this.name = name;
            this.firstJoin = firstJoin;
            this.lastJoin = lastJoin;
        }

        public int getId() {
            return id;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public long getFirstJoin() {
            return firstJoin;
        }

        public long getLastJoin() {
            return lastJoin;
        }
    }

    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_CLEANUP_INTERVAL = 300000; // 5 minutos

    public void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        
        // Limpar cache se estiver muito grande
        if (nameToUuidCache.size() > MAX_CACHE_SIZE) {
            nameToUuidCache.clear();
            uuidToNameCache.clear();
            idToUuidCache.clear();
            uuidToIdCache.clear();
        }
    }

    // Método para ser chamado periodicamente
    public void startCacheCleanup() {
        // Implementar limpeza periódica se necessário
    }
}

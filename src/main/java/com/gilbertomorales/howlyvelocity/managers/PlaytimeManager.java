package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.proxy.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeManager {

    private final DatabaseManager databaseManager;
    
    // Cache para sessões ativas (UUID -> timestamp de início da sessão)
    private final Map<UUID, Long> activeSessions = new ConcurrentHashMap<>();

    public PlaytimeManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Inicia uma sessão para um jogador
     */
    public void startSession(UUID playerUuid) {
        long currentTime = System.currentTimeMillis();
        activeSessions.put(playerUuid, currentTime);
        
        // Atualizar no banco de dados
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                // Primeiro verificar se o jogador já existe na tabela
                String checkSql = "SELECT total_time FROM player_playtime WHERE player_uuid = ?";
                
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, playerUuid.toString());
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        // Jogador existe, apenas atualizar a sessão
                        String updateSql = "UPDATE player_playtime SET session_start = ?, last_updated = ? WHERE player_uuid = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setLong(1, currentTime);
                            updateStmt.setLong(2, currentTime);
                            updateStmt.setString(3, playerUuid.toString());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Jogador não existe, inserir novo registro
                        String insertSql;
                        
                        if (databaseManager.isMySQL()) {
                            insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) " +
                                      "VALUES (?, 0, ?, ?)";
                        } else if (databaseManager.isH2()) {
                            insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) " +
                                      "VALUES (?, 0, ?, ?)";
                        } else {
                            insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) " +
                                      "VALUES (?, 0, ?, ?)";
                        }
                        
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, playerUuid.toString());
                            insertStmt.setLong(2, currentTime);
                            insertStmt.setLong(3, currentTime);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Finaliza uma sessão para um jogador
     */
    public void endSession(UUID playerUuid) {
        Long sessionStart = activeSessions.remove(playerUuid);
        if (sessionStart == null) {
            return; // Não havia sessão ativa
        }
        
        long sessionDuration = System.currentTimeMillis() - sessionStart;
        
        // Atualizar tempo total no banco de dados
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "UPDATE player_playtime SET " +
                           "total_time = total_time + ?, " +
                           "session_start = NULL, " +
                           "last_updated = ? " +
                           "WHERE player_uuid = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, sessionDuration);
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.setString(3, playerUuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Obtém o tempo total online de um jogador
     */
    public CompletableFuture<Long> getPlayerPlaytime(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT total_time, session_start FROM player_playtime WHERE player_uuid = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long totalTime = rs.getLong("total_time");
                            Long sessionStart = rs.getObject("session_start", Long.class);
                            
                            // Se há uma sessão ativa, adicionar o tempo da sessão atual
                            if (sessionStart != null) {
                                long currentSessionTime = System.currentTimeMillis() - sessionStart;
                                totalTime += currentSessionTime;
                            }
                            
                            return totalTime;
                        }
                    }
                }
                
                // Se não encontrou registro, criar um novo com tempo zero
                String insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) VALUES (?, 0, NULL, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            return 0L;
        });
    }

    /**
     * Obtém o top 10 jogadores com mais tempo online
     */
    public CompletableFuture<List<PlaytimeEntry>> getTopPlaytime() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlaytimeEntry> topList = new ArrayList<>();
            
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT pt.player_uuid, pt.total_time, pt.session_start, p.name " +
                           "FROM player_playtime pt " +
                           "JOIN players p ON pt.player_uuid = p.uuid " +
                           "ORDER BY pt.total_time DESC " +
                           "LIMIT 10";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                            long totalTime = rs.getLong("total_time");
                            Long sessionStart = rs.getObject("session_start", Long.class);
                            String playerName = rs.getString("name");
                            
                            // Se há uma sessão ativa, adicionar o tempo da sessão atual
                            if (sessionStart != null) {
                                long currentSessionTime = System.currentTimeMillis() - sessionStart;
                                totalTime += currentSessionTime;
                            }
                            
                            topList.add(new PlaytimeEntry(playerUuid, playerName, totalTime));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            // Reordenar considerando sessões ativas
            topList.sort((a, b) -> Long.compare(b.getPlaytime(), a.getPlaytime()));
            
            return topList;
        });
    }

    /**
     * Reseta o tempo online de um jogador
     */
    public CompletableFuture<Boolean> resetPlayerPlaytime(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "UPDATE player_playtime SET total_time = 0, last_updated = ? WHERE player_uuid = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, System.currentTimeMillis());
                    stmt.setString(2, playerUuid.toString());
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    // Se não afetou nenhuma linha, o jogador pode não existir na tabela
                    if (rowsAffected == 0) {
                        // Inserir um novo registro com tempo zero
                        String insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) VALUES (?, 0, NULL, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, playerUuid.toString());
                            insertStmt.setLong(2, System.currentTimeMillis());
                            insertStmt.executeUpdate();
                        }
                    }
                    
                    // Remover qualquer sessão ativa do cache
                    activeSessions.remove(playerUuid);
                    
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Formata o tempo de jogo para exibição
     */
    public String formatPlaytime(long millis) {
        return TimeUtils.formatDuration(millis);
    }

    /**
     * Classe para representar uma entrada no ranking de tempo
     */
    public static class PlaytimeEntry {
        private final UUID playerUuid;
        private final String playerName;
        private final long playtime;

        public PlaytimeEntry(UUID playerUuid, String playerName, long playtime) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.playtime = playtime;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getPlaytime() {
            return playtime;
        }
    }
}

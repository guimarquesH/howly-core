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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlaytimeManager {

    private final DatabaseManager databaseManager;

    // Cache para sessões ativas (UUID -> timestamp de início da sessão)
    private final Map<UUID, Long> activeSessions = new ConcurrentHashMap<>();
    // Cache para tempo total (UUID -> tempo total em milissegundos)
    private final Map<UUID, Long> totalTimeCache = new ConcurrentHashMap<>();

    // Executor para salvar periodicamente
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public PlaytimeManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        // Carregar cache de tempo total ao iniciar
        loadTotalTimeCache();
        // Iniciar salvamento periódico a cada 30 segundos
        startPeriodicSave();
    }

    /**
     * Carrega o cache de tempo total do banco de dados
     */
    private void loadTotalTimeCache() {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT player_uuid, total_time, session_start FROM player_playtime";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        try {
                            UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                            long totalTime = rs.getLong("total_time");
                            Long sessionStart = rs.getObject("session_start", Long.class);

                            // Carregar tempo total no cache
                            totalTimeCache.put(playerUuid, totalTime);

                            // Se havia uma sessão ativa quando o servidor foi fechado,
                            // considerar esse tempo como perdido e limpar a sessão
                            if (sessionStart != null) {
                                // Calcular tempo da sessão perdida
                                long currentTime = System.currentTimeMillis();
                                long sessionTime = currentTime - sessionStart;

                                // Se a sessão foi há menos de 5 minutos, considerar como válida
                                // (caso o servidor tenha sido reiniciado rapidamente)
                                if (sessionTime < 300000) { // 5 minutos
                                    totalTime += sessionTime;
                                    totalTimeCache.put(playerUuid, totalTime);

                                    // Atualizar no banco
                                    updateTotalTimeInDatabase(playerUuid, totalTime, null);
                                } else {
                                    // Sessão muito antiga, apenas limpar
                                    updateTotalTimeInDatabase(playerUuid, totalTime, null);
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            // Ignorar UUIDs inválidos
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Inicia o salvamento periódico das sessões ativas
     */
    private void startPeriodicSave() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveActiveSessions();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Salva o progresso das sessões ativas no banco de dados
     */
    private void saveActiveSessions() {
        if (activeSessions.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        try (Connection conn = databaseManager.getConnection()) {
            String sql = "UPDATE player_playtime SET total_time = ?, last_updated = ? WHERE player_uuid = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<UUID, Long> entry : activeSessions.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    Long sessionStart = entry.getValue();

                    if (sessionStart != null) {
                        long sessionTime = currentTime - sessionStart;
                        long cachedTotal = totalTimeCache.getOrDefault(playerUuid, 0L);
                        long newTotal = cachedTotal + sessionTime;

                        stmt.setLong(1, newTotal);
                        stmt.setLong(2, currentTime);
                        stmt.setString(3, playerUuid.toString());
                        stmt.addBatch();

                        // Atualizar cache
                        totalTimeCache.put(playerUuid, newTotal);
                        // Atualizar início da sessão
                        activeSessions.put(playerUuid, currentTime);
                    }
                }

                stmt.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Atualiza o tempo total no banco de dados
     */
    private void updateTotalTimeInDatabase(UUID playerUuid, long totalTime, Long sessionStart) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "UPDATE player_playtime SET total_time = ?, session_start = ?, last_updated = ? WHERE player_uuid = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setLong(1, totalTime);
                    if (sessionStart != null) {
                        stmt.setLong(2, sessionStart);
                    } else {
                        stmt.setNull(2, java.sql.Types.BIGINT);
                    }
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setString(4, playerUuid.toString());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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
                        // Jogador existe, atualizar a sessão
                        long existingTotal = rs.getLong("total_time");
                        totalTimeCache.put(playerUuid, existingTotal);

                        String updateSql = "UPDATE player_playtime SET session_start = ?, last_updated = ? WHERE player_uuid = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setLong(1, currentTime);
                            updateStmt.setLong(2, currentTime);
                            updateStmt.setString(3, playerUuid.toString());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Jogador não existe, inserir novo registro
                        String insertSql = "INSERT INTO player_playtime (player_uuid, total_time, session_start, last_updated) VALUES (?, 0, ?, ?)";

                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                            insertStmt.setString(1, playerUuid.toString());
                            insertStmt.setLong(2, currentTime);
                            insertStmt.setLong(3, currentTime);
                            insertStmt.executeUpdate();

                            // Inicializar o cache
                            totalTimeCache.put(playerUuid, 0L);
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

        // Atualizar o cache
        Long currentTotal = totalTimeCache.getOrDefault(playerUuid, 0L);
        long newTotal = currentTotal + sessionDuration;
        totalTimeCache.put(playerUuid, newTotal);

        // Atualizar tempo total no banco de dados imediatamente
        updateTotalTimeInDatabase(playerUuid, newTotal, null);
    }

    /**
     * Obtém o tempo total online de um jogador
     */
    public CompletableFuture<Long> getPlayerPlaytime(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Verificar primeiro no cache
            Long cachedTime = totalTimeCache.get(playerUuid);
            Long sessionStart = activeSessions.get(playerUuid);

            if (cachedTime != null) {
                // Se há uma sessão ativa, adicionar o tempo da sessão atual
                if (sessionStart != null) {
                    long currentSessionTime = System.currentTimeMillis() - sessionStart;
                    return cachedTime + currentSessionTime;
                }
                return cachedTime;
            }

            // Se não estiver no cache, buscar do banco de dados
            try (Connection conn = databaseManager.getConnection()) {
                String sql = "SELECT total_time, session_start FROM player_playtime WHERE player_uuid = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long totalTime = rs.getLong("total_time");

                            // Atualizar o cache
                            totalTimeCache.put(playerUuid, totalTime);

                            // Se há uma sessão ativa, adicionar o tempo da sessão atual
                            if (sessionStart != null) {
                                long currentSessionTime = System.currentTimeMillis() - sessionStart;
                                return totalTime + currentSessionTime;
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

                    // Atualizar o cache
                    totalTimeCache.put(playerUuid, 0L);
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
            Map<UUID, Long> currentTimes = new HashMap<>();

            // Primeiro, copiar todos os tempos do cache
            for (Map.Entry<UUID, Long> entry : totalTimeCache.entrySet()) {
                UUID playerUuid = entry.getKey();
                long totalTime = entry.getValue();

                // Adicionar tempo da sessão atual se estiver online
                Long sessionStart = activeSessions.get(playerUuid);
                if (sessionStart != null) {
                    long currentSessionTime = System.currentTimeMillis() - sessionStart;
                    totalTime += currentSessionTime;
                }

                currentTimes.put(playerUuid, totalTime);
            }

            try (Connection conn = databaseManager.getConnection()) {
                // Buscar nomes dos jogadores
                String sql = "SELECT pt.player_uuid, pt.total_time, p.name " +
                        "FROM player_playtime pt " +
                        "JOIN players p ON pt.player_uuid = p.uuid " +
                        "ORDER BY pt.total_time DESC " +
                        "LIMIT 20"; // Buscar mais que 10 para caso alguns não estejam no cache

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                            String playerName = rs.getString("name");

                            // Usar o tempo do cache se disponível, senão usar do banco
                            long totalTime = currentTimes.getOrDefault(playerUuid, rs.getLong("total_time"));

                            // Adicionar tempo da sessão atual se não estiver no cache mas estiver online
                            if (!currentTimes.containsKey(playerUuid)) {
                                Long sessionStart = activeSessions.get(playerUuid);
                                if (sessionStart != null) {
                                    long currentSessionTime = System.currentTimeMillis() - sessionStart;
                                    totalTime += currentSessionTime;
                                }
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

            // Limitar a 10 resultados
            if (topList.size() > 10) {
                topList = topList.subList(0, 10);
            }

            return topList;
        });
    }

    /**
     * Reseta o tempo online de um jogador
     */
    public CompletableFuture<Boolean> resetPlayerPlaytime(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Atualizar o cache
            totalTimeCache.put(playerUuid, 0L);

            // Se o jogador estiver online, reiniciar a sessão
            if (activeSessions.containsKey(playerUuid)) {
                activeSessions.put(playerUuid, System.currentTimeMillis());
            }

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
     * Finaliza o PlaytimeManager e salva todas as sessões ativas
     */
    public void shutdown() {
        // Salvar todas as sessões ativas antes de fechar
        saveActiveSessions();

        // Finalizar todas as sessões ativas
        for (UUID playerUuid : new HashSet<>(activeSessions.keySet())) {
            endSession(playerUuid);
        }

        // Parar o scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
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

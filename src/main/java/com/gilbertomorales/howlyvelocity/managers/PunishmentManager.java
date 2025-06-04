package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PunishmentManager {
    private static final Logger logger = LoggerFactory.getLogger(PunishmentManager.class);
    private final DatabaseManager databaseManager;
    private final ScheduledExecutorService scheduler;

    public PunishmentManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Iniciar limpeza automática de punições expiradas a cada 5 minutos
        scheduler.scheduleAtFixedRate(this::cleanExpiredPunishments, 5, 5, TimeUnit.MINUTES);
    }

    public void addPunishment(Punishment punishment) {
        String sql = "INSERT INTO punishments (player_uuid, type, reason, punisher, created_at, expires_at, active) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, punishment.getPlayerUUID().toString());
            stmt.setString(2, punishment.getType().name());
            stmt.setString(3, punishment.getReason());
            stmt.setString(4, punishment.getPunisher());
            stmt.setLong(5, punishment.getCreatedAt());
            
            if (punishment.getExpiresAt() != null) {
                stmt.setLong(6, punishment.getExpiresAt());
            } else {
                stmt.setNull(6, java.sql.Types.BIGINT);
            }
            
            stmt.setBoolean(7, punishment.isActive());

            stmt.executeUpdate();
            logger.info("Punição adicionada: {} para jogador {}", punishment.getType(), punishment.getPlayerUUID());

        } catch (SQLException e) {
            logger.error("Erro ao adicionar punição: " + e.getMessage(), e);
        }
    }

    public void removePunishment(UUID playerUUID, PunishmentType type) {
        String sql = "UPDATE punishments SET active = FALSE WHERE player_uuid = ? AND type = ? AND active = TRUE";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, type.name());

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Punição removida: {} para jogador {}", type, playerUUID);
            }

        } catch (SQLException e) {
            logger.error("Erro ao remover punição: " + e.getMessage(), e);
        }
    }

    public Punishment getActivePunishment(UUID playerUUID, PunishmentType type) {
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND type = ? AND active = TRUE AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, type.name());
            stmt.setLong(3, System.currentTimeMillis());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return createPunishmentFromResultSet(rs);
            }

        } catch (SQLException e) {
            logger.error("Erro ao buscar punição ativa: " + e.getMessage(), e);
        }

        return null;
    }

    public List<Punishment> getPlayerPunishments(UUID playerUUID) {
        List<Punishment> punishments = new ArrayList<>();
        String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                punishments.add(createPunishmentFromResultSet(rs));
            }

        } catch (SQLException e) {
            logger.error("Erro ao buscar punições do jogador: " + e.getMessage(), e);
        }

        return punishments;
    }

    private Punishment createPunishmentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
        PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
        String reason = rs.getString("reason");
        String punisher = rs.getString("punisher");
        long createdAt = rs.getLong("created_at");
        
        // Obter expires_at como Long, pode ser null
        Long expiresAt = null;
        if (!rs.wasNull()) {
            expiresAt = rs.getLong("expires_at");
            if (rs.wasNull()) {
                expiresAt = null;
            }
        }
        
        boolean active = rs.getBoolean("active");

        return new Punishment(id, playerUUID, type, reason, punisher, createdAt, expiresAt, active);
    }

    private void cleanExpiredPunishments() {
        String sql = "UPDATE punishments SET active = FALSE WHERE expires_at IS NOT NULL AND expires_at <= ? AND active = TRUE";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, System.currentTimeMillis());
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                logger.info("Limpeza automática: {} punições expiradas foram desativadas", updated);
            }

        } catch (SQLException e) {
            logger.error("Erro na limpeza automática de punições: " + e.getMessage(), e);
        }
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}

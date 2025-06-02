package com.gilbertomorales.howlyvelocity.api.punishment;

import com.gilbertomorales.howlyvelocity.api.punishment.events.PunishmentEvent;
import com.gilbertomorales.howlyvelocity.managers.DatabaseManager;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PunishmentManager implements PunishmentAPI {

    private final DatabaseManager databaseManager;
    private final ProxyServer server;

    public PunishmentManager(DatabaseManager databaseManager, ProxyServer server) {
        this.databaseManager = databaseManager;
        this.server = server;
    }

    @Override
    public CompletableFuture<Punishment> banPlayer(UUID playerUUID, String reason, Long duration, String punisher) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Desativar bans anteriores
                deactivatePunishments(playerUUID, PunishmentType.BAN);
                
                // Criar novo ban
                Punishment punishment = createPunishment(playerUUID, PunishmentType.BAN, reason, duration, punisher);
                
                // Kickar jogador se estiver online
                Optional<Player> player = server.getPlayer(playerUUID);
                if (player.isPresent()) {
                    String message = "§c§lVOCÊ FOI BANIDO!\n\n" +
                                   "§fMotivo: §c" + reason + "\n" +
                                   "§fPunidor: §e" + punisher + "\n" +
                                   "§fDuração: §a" + (duration == null ? "Permanente" : TimeUtils.formatDuration(duration)) + "\n\n" +
                                   "§7Apele em: §bdiscord.gg/howly";
                    
                    player.get().disconnect(Component.text(message));
                }
                
                // Disparar evento
                server.getEventManager().fireAndForget(new PunishmentEvent(punishment));
                
                return punishment;
                
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao banir jogador", e);
            }
        });
    }

    @Override
    public CompletableFuture<Punishment> kickPlayer(UUID playerUUID, String reason, String punisher) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Criar kick
                Punishment punishment = createPunishment(playerUUID, PunishmentType.KICK, reason, null, punisher);
                
                // Kickar jogador se estiver online
                Optional<Player> player = server.getPlayer(playerUUID);
                if (player.isPresent()) {
                    String message = "§c§lVOCÊ FOI EXPULSO!\n\n" +
                                   "§fMotivo: §c" + reason + "\n" +
                                   "§fPunidor: §e" + punisher + "\n\n" +
                                   "§7Você pode reconectar imediatamente.";
                    
                    player.get().disconnect(Component.text(message));
                }
                
                // Disparar evento
                server.getEventManager().fireAndForget(new PunishmentEvent(punishment));
                
                return punishment;
                
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao expulsar jogador", e);
            }
        });
    }

    @Override
    public CompletableFuture<Punishment> mutePlayer(UUID playerUUID, String reason, Long duration, String punisher) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Desativar mutes anteriores
                deactivatePunishments(playerUUID, PunishmentType.MUTE);
                
                // Criar novo mute
                Punishment punishment = createPunishment(playerUUID, PunishmentType.MUTE, reason, duration, punisher);
                
                // Notificar jogador se estiver online
                Optional<Player> player = server.getPlayer(playerUUID);
                if (player.isPresent()) {
                    String message = "§c§lVOCÊ FOI SILENCIADO!\n\n" +
                                   "§fMotivo: §c" + reason + "\n" +
                                   "§fPunidor: §e" + punisher + "\n" +
                                   "§fDuração: §a" + (duration == null ? "Permanente" : TimeUtils.formatDuration(duration));
                    
                    player.get().sendMessage(Component.text(message));
                }
                
                // Disparar evento
                server.getEventManager().fireAndForget(new PunishmentEvent(punishment));
                
                return punishment;
                
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao silenciar jogador", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unbanPlayer(UUID playerUUID, String unbanner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return deactivatePunishments(playerUUID, PunishmentType.BAN) > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao desbanir jogador", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> unmutePlayer(UUID playerUUID, String unmuter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return deactivatePunishments(playerUUID, PunishmentType.MUTE) > 0;
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao desmutar jogador", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerBanned(UUID playerUUID) {
        return getActivePunishment(playerUUID, PunishmentType.BAN)
                .thenApply(punishment -> punishment != null);
    }

    @Override
    public CompletableFuture<Boolean> isPlayerMuted(UUID playerUUID) {
        return getActivePunishment(playerUUID, PunishmentType.MUTE)
                .thenApply(punishment -> punishment != null);
    }

    @Override
    public CompletableFuture<Punishment> getActiveBan(UUID playerUUID) {
        return getActivePunishment(playerUUID, PunishmentType.BAN);
    }

    @Override
    public CompletableFuture<Punishment> getActiveMute(UUID playerUUID) {
        return getActivePunishment(playerUUID, PunishmentType.MUTE);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPlayerPunishments(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            punishments.add(createPunishmentFromResultSet(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao buscar punições do jogador", e);
            }
            
            return punishments;
        });
    }

    @Override
    public CompletableFuture<Punishment> getPunishmentById(int punishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT * FROM punishments WHERE id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, punishmentId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createPunishmentFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao buscar punição", e);
            }
            
            return null;
        });
    }

    private CompletableFuture<Punishment> getActivePunishment(UUID playerUUID, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT * FROM punishments WHERE player_uuid = ? AND type = ? AND active = ? " +
                           "AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.setString(2, type.name());
                    stmt.setBoolean(3, true);
                    stmt.setLong(4, System.currentTimeMillis());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createPunishmentFromResultSet(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao buscar punição ativa", e);
            }
            
            return null;
        });
    }

    private Punishment createPunishment(UUID playerUUID, PunishmentType type, String reason, 
                                      Long duration, String punisher) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "INSERT INTO punishments (player_uuid, type, reason, punisher, created_at, expires_at, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            long createdAt = System.currentTimeMillis();
            Long expiresAt = duration != null ? createdAt + duration : null;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, type.name());
                stmt.setString(3, reason);
                stmt.setString(4, punisher);
                stmt.setLong(5, createdAt);
                if (expiresAt != null) {
                    stmt.setLong(6, expiresAt);
                } else {
                    stmt.setNull(6, java.sql.Types.BIGINT);
                }
                stmt.setBoolean(7, true);
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        return new Punishment(id, playerUUID, type, reason, punisher, createdAt, expiresAt, true);
                    }
                }
            }
        }
        
        throw new SQLException("Falha ao criar punição");
    }

    private int deactivatePunishments(UUID playerUUID, PunishmentType type) throws SQLException {
        try (Connection connection = databaseManager.getConnection()) {
            String sql = "UPDATE punishments SET active = ? WHERE player_uuid = ? AND type = ? AND active = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setString(2, playerUUID.toString());
                stmt.setString(3, type.name());
                stmt.setBoolean(4, true);
                
                return stmt.executeUpdate();
            }
        }
    }

    private Punishment createPunishmentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
        PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
        String reason = rs.getString("reason");
        String punisher = rs.getString("punisher");
        long createdAt = rs.getLong("created_at");
        Long expiresAt = rs.getObject("expires_at", Long.class);
        boolean active = rs.getBoolean("active");
        
        return new Punishment(id, playerUUID, type, reason, punisher, createdAt, expiresAt, active);
    }
}

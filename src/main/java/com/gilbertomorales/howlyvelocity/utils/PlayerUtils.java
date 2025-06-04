package com.gilbertomorales.howlyvelocity.utils;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utilitário para buscar jogadores por nome ou ID
 */
public class PlayerUtils {

    /**
     * Busca um jogador por identificador (nome ou #ID)
     * @param server Servidor proxy
     * @param identifier Nome do jogador ou #ID
     * @return CompletableFuture com PlayerResult
     */
    public static CompletableFuture<PlayerResult> findPlayer(ProxyServer server, String identifier) {
        PlayerDataManager playerDataManager = HowlyAPI.getInstance().getPlugin().getPlayerDataManager();

        // Verificar se é busca por ID (#1, #2, etc.)
        if (identifier.startsWith("#")) {
            try {
                int playerId = Integer.parseInt(identifier.substring(1));
                return playerDataManager.getPlayerUUIDById(playerId).thenCompose(uuid -> {
                    if (uuid != null) {
                        Optional<Player> onlinePlayer = server.getPlayer(uuid);
                        if (onlinePlayer.isPresent()) {
                            return CompletableFuture.completedFuture(new PlayerResult(onlinePlayer.get(), playerId, true));
                        } else {
                            return playerDataManager.getPlayerInfoById(playerId).thenApply(info -> {
                                if (info != null) {
                                    return new PlayerResult(info, false);
                                }
                                return null;
                            });
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                });
            } catch (NumberFormatException e) {
                return CompletableFuture.completedFuture(null);
            }
        }

        // Busca por nome
        Optional<Player> onlinePlayer = server.getPlayer(identifier);
        if (onlinePlayer.isPresent()) {
            Player player = onlinePlayer.get();
            return playerDataManager.getPlayerId(player.getUniqueId()).thenApply(playerId -> 
                new PlayerResult(player, playerId, true)
            );
        }

        // Jogador offline, buscar no banco
        return playerDataManager.getPlayerUUID(identifier).thenCompose(uuid -> {
            if (uuid != null) {
                return playerDataManager.getPlayerInfo(uuid).thenApply(info -> 
                    info != null ? new PlayerResult(info, false) : null
                );
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    /**
     * Classe para resultado da busca de jogador
     */
    public static class PlayerResult {
        private final Player onlinePlayer;
        private final PlayerDataManager.PlayerInfo offlineInfo;
        private final boolean isOnline;
        private final Integer playerId;

        // Construtor para jogador online
        public PlayerResult(Player onlinePlayer, Integer playerId, boolean isOnline) {
            this.onlinePlayer = onlinePlayer;
            this.offlineInfo = null;
            this.isOnline = isOnline;
            this.playerId = playerId;
        }

        // Construtor para jogador offline
        public PlayerResult(PlayerDataManager.PlayerInfo offlineInfo, boolean isOnline) {
            this.onlinePlayer = null;
            this.offlineInfo = offlineInfo;
            this.isOnline = isOnline;
            this.playerId = offlineInfo.getId();
        }

        public boolean isOnline() {
            return isOnline;
        }

        public Player getOnlinePlayer() {
            return onlinePlayer;
        }

        public PlayerDataManager.PlayerInfo getOfflineInfo() {
            return offlineInfo;
        }

        public UUID getUUID() {
            return isOnline ? onlinePlayer.getUniqueId() : offlineInfo.getUuid();
        }

        public String getName() {
            return isOnline ? onlinePlayer.getUsername() : offlineInfo.getName();
        }

        public Integer getPlayerId() {
            return playerId;
        }
    }
}

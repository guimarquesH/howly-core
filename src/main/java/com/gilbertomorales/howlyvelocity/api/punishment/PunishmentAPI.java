package com.gilbertomorales.howlyvelocity.api.punishment;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentAPI {

    /**
     * Bane um jogador
     * @param playerUUID UUID do jogador
     * @param reason Motivo do banimento
     * @param duration Duração em milissegundos (null para permanente)
     * @param punisher Nome do punidor
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Punishment> banPlayer(UUID playerUUID, String reason, Long duration, String punisher);

    /**
     * Kicka um jogador
     * @param playerUUID UUID do jogador
     * @param reason Motivo do kick
     * @param punisher Nome do punidor
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Punishment> kickPlayer(UUID playerUUID, String reason, String punisher);

    /**
     * Muta um jogador
     * @param playerUUID UUID do jogador
     * @param reason Motivo do mute
     * @param duration Duração em milissegundos (null para permanente)
     * @param punisher Nome do punidor
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Punishment> mutePlayer(UUID playerUUID, String reason, Long duration, String punisher);

    /**
     * Remove o banimento de um jogador
     * @param playerUUID UUID do jogador
     * @param unbanner Nome de quem removeu o ban
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Boolean> unbanPlayer(UUID playerUUID, String unbanner);

    /**
     * Remove o mute de um jogador
     * @param playerUUID UUID do jogador
     * @param unmuter Nome de quem removeu o mute
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Boolean> unmutePlayer(UUID playerUUID, String unmuter);

    /**
     * Verifica se um jogador está banido
     * @param playerUUID UUID do jogador
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Boolean> isPlayerBanned(UUID playerUUID);

    /**
     * Verifica se um jogador está mutado
     * @param playerUUID UUID do jogador
     * @return CompletableFuture com o resultado
     */
    CompletableFuture<Boolean> isPlayerMuted(UUID playerUUID);

    /**
     * Obtém o banimento ativo de um jogador
     * @param playerUUID UUID do jogador
     * @return CompletableFuture com a punição ou null se não estiver banido
     */
    CompletableFuture<Punishment> getActiveBan(UUID playerUUID);

    /**
     * Obtém o mute ativo de um jogador
     * @param playerUUID UUID do jogador
     * @return CompletableFuture com a punição ou null se não estiver mutado
     */
    CompletableFuture<Punishment> getActiveMute(UUID playerUUID);

    /**
     * Obtém todas as punições de um jogador
     * @param playerUUID UUID do jogador
     * @return CompletableFuture com a lista de punições
     */
    CompletableFuture<List<Punishment>> getPlayerPunishments(UUID playerUUID);

    /**
     * Obtém uma punição pelo ID
     * @param punishmentId ID da punição
     * @return CompletableFuture com a punição ou null se não encontrada
     */
    CompletableFuture<Punishment> getPunishmentById(int punishmentId);
}

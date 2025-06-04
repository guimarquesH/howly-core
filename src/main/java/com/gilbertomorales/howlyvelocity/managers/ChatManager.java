package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.HowlyVelocity;
import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.utils.ChatUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChatManager {
    private final ProxyServer server;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private PunishmentAPI punishmentAPI;

    public ChatManager(ProxyServer server, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
    }

    /**
     * Inicializa a API de punições após a HowlyAPI estar disponível
     */
    public void initializePunishmentAPI() {
        try {
            HowlyAPI api = HowlyAPI.getInstance();
            if (api != null) {
                this.punishmentAPI = api.getPunishmentAPI();
            }
        } catch (Exception e) {
            // Log do erro se necessário
            this.punishmentAPI = null;
        }
    }

    public void sendMessage(Player sender, String message) {
        // Aplicar cores se o jogador tiver permissão
        String coloredMessage = ChatUtils.applyColors(message, sender);

        // Verificar se a API está disponível antes de verificar mute
        if (punishmentAPI != null) {
            // Verificar se o jogador está mutado
            punishmentAPI.getActiveMute(sender.getUniqueId()).thenAccept(mute -> {
                if (mute != null) {
                    // Jogador está mutado, não enviar mensagem
                    String timeRemaining = mute.isPermanent() ? "Permanente" : com.gilbertomorales.howlyvelocity.utils.TimeUtils.formatDuration(mute.getRemainingTime());
                    String muteMessage = "\n§cVocê está silenciado.\n\n" +
                            "§fMotivo: §7" + mute.getReason() + "\n" +
                            "§fTempo restante: §7" + timeRemaining + "\n\n" +
                            "§eVocê pode apelar no nosso discord §ndiscord.gg/howly§e.\n";

                    sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(muteMessage));
                    return;
                }

                // Jogador não está mutado, enviar mensagem
                sendServerMessage(sender, coloredMessage);
            });
        } else {
            // Se a API não estiver disponível, enviar mensagem normalmente
            sendServerMessage(sender, coloredMessage);
        }
    }

    private void sendServerMessage(Player sender, String message) {
        Optional<ServerConnection> serverConnection = sender.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }

        RegisteredServer currentServer = serverConnection.get().getServer();
        String formattedMessage = formatMessage(sender, message);

        // Enviar para todos os jogadores no mesmo servidor
        for (Player player : currentServer.getPlayersConnected()) {
            player.sendMessage(Component.text(formattedMessage));
        }
    }

    private String formatMessage(Player sender, String message) {
        // Obter o GroupManager
        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        
        // Obter medalha formatada
        String medal = medalManager.getFormattedPlayerMedal(sender);
        
        // Obter tag do jogador
        String tag = tagManager.getPlayerTag(sender);
        
        // Obter o grupo de maior prioridade e sua cor para o nome
        String groupPrefix = groupManager.getPlayerGroupPrefix(sender);
        String nameColor = groupManager.getPlayerGroupNameColor(sender);

        // Formatação: [Medalha][Tag] [Grupo] NomeColorido: mensagem
        StringBuilder formattedMessage = new StringBuilder();
        
        // Adicionar medalha se existir
        if (medal != null && !medal.isEmpty()) {
            formattedMessage.append(medal);
        }
        
        // Adicionar tag se existir
        if (tag != null && !tag.isEmpty()) {
            formattedMessage.append(tag).append(" ");
        }
        
        // Adicionar grupo se existir
        if (groupPrefix != null && !groupPrefix.isEmpty()) {
            formattedMessage.append(groupPrefix).append(" ");
        }
        
        // Adicionar nome com cor do grupo de maior prioridade
        formattedMessage.append(nameColor).append(sender.getUsername());
        
        // Adicionar mensagem
        formattedMessage.append("§7: §f").append(message);

        return formattedMessage.toString();
    }
    
    /**
     * Verifica se um jogador está mutado de forma síncrona
     * @param uuid UUID do jogador
     * @return true se o jogador estiver mutado, false caso contrário
     */
    public boolean isPlayerMuted(UUID uuid) {
        if (punishmentAPI == null) {
            return false;
        }
        
        try {
            // Obter o resultado de forma síncrona com timeout
            return punishmentAPI.getActiveMute(uuid)
                .thenApply(mute -> mute != null)
                .get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Em caso de erro, assumir que não está mutado
            return false;
        }
    }
}

package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatManager {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final PunishmentAPI punishmentAPI;
    
    // Jogadores que estão usando o chat global
    private final ConcurrentHashMap<UUID, Boolean> globalChatUsers = new ConcurrentHashMap<>();

    public ChatManager(ProxyServer server, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
    }

    public void sendMessage(Player sender, String message) {
        // Verificar se o jogador está mutado
        punishmentAPI.getActiveMute(sender.getUniqueId()).thenAccept(mute -> {
            if (mute != null) {
                // Jogador está mutado, não enviar mensagem
                String timeRemaining = mute.isPermanent() ? "Permanente" : com.gilbertomorales.howlyvelocity.utils.TimeUtils.formatDuration(mute.getRemainingTime());
                sender.sendMessage(Component.text("§cVocê está silenciado e não pode falar no chat."));
                sender.sendMessage(Component.text("§cMotivo: §f" + mute.getReason()));
                sender.sendMessage(Component.text("§cTempo restante: §f" + timeRemaining));
                return;
            }
            
            // Jogador não está mutado, enviar mensagem
            boolean isGlobalChat = globalChatUsers.getOrDefault(sender.getUniqueId(), false);
            
            if (isGlobalChat) {
                sendGlobalMessage(sender, message);
            } else {
                sendServerMessage(sender, message);
            }
        });
    }

    private void sendServerMessage(Player sender, String message) {
        Optional<ServerConnection> serverConnection = sender.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }
        
        RegisteredServer currentServer = serverConnection.get().getServer();
        String formattedMessage = formatMessage(sender, message, false);
        
        // Enviar para todos os jogadores no mesmo servidor
        for (Player player : currentServer.getPlayersConnected()) {
            player.sendMessage(Component.text(formattedMessage));
        }
    }

    private void sendGlobalMessage(Player sender, String message) {
        String formattedMessage = formatMessage(sender, message, true);
        
        // Enviar para todos os jogadores na rede
        for (Player player : server.getAllPlayers()) {
            player.sendMessage(Component.text(formattedMessage));
        }
    }

    private String formatMessage(Player sender, String message, boolean isGlobal) {
        String medal = medalManager.getFormattedPlayerMedal(sender);
        String tag = tagManager.getPlayerTag(sender);
        String nameColor = tagManager.getPlayerNameColor(sender);
        
        if (isGlobal) {
            return "§7[G] " + medal + tag + " " + nameColor + sender.getUsername() + "§7: §f" + message;
        } else {
            return medal + tag + " " + nameColor + sender.getUsername() + "§7: §f" + message;
        }
    }

    public void toggleGlobalChat(Player player) {
        boolean current = globalChatUsers.getOrDefault(player.getUniqueId(), false);
        globalChatUsers.put(player.getUniqueId(), !current);
        
        if (!current) {
            player.sendMessage(Component.text("§aChat global ativado. Suas mensagens serão enviadas para toda a rede."));
        } else {
            player.sendMessage(Component.text("§aChatglobal desativado. Suas mensagens serão enviadas apenas para o servidor atual."));
        }
    }

    public boolean isUsingGlobalChat(Player player) {
        return globalChatUsers.getOrDefault(player.getUniqueId(), false);
    }
}

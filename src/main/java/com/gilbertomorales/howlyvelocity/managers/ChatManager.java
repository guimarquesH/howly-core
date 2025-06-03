package com.gilbertomorales.howlyvelocity.managers;

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
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
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
        String medal = medalManager.getFormattedPlayerMedal(sender);
        String tag = tagManager.getPlayerTag(sender);
        String nameColor = tagManager.getPlayerNameColor(sender);

        return medal + tag + " " + nameColor + sender.getUsername() + "§7: §f" + message;
    }
}
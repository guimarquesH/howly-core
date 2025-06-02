package com.gilbertomorales.howlyvelocity.listeners;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChatListener {

    private final ProxyServer server;
    private final PunishmentAPI punishmentAPI;
    private final TagManager tagManager;
    private final MedalManager medalManager;

    public ChatListener(ProxyServer server, HowlyAPI api, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.punishmentAPI = api.getPunishmentAPI();
        this.tagManager = tagManager;
        this.medalManager = medalManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Sempre cancelar o chat padrão para evitar mensagens duplicadas
        event.setResult(PlayerChatEvent.ChatResult.denied());
        
        // Verificar se o jogador está mutado
        CompletableFuture<Punishment> muteFuture = punishmentAPI.getActiveMute(player.getUniqueId());
        
        muteFuture.thenAccept(mute -> {
            if (mute != null) {
                // Jogador está mutado, informar ao jogador
                String timeRemaining = mute.isPermanent() ? "Permanente" : TimeUtils.formatDuration(mute.getRemainingTime());
                player.sendMessage(Component.text("§cVocê está silenciado e não pode falar no chat."));
                player.sendMessage(Component.text("§cMotivo: §f" + mute.getReason()));
                player.sendMessage(Component.text("§cTempo restante: §f" + timeRemaining));
            } else {
                // Jogador não está mutado, processar mensagem
                sendServerMessage(player, event.getMessage());
            }
        });
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

package com.gilbertomorales.howlyvelocity.listeners;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.ChatManager;
import com.gilbertomorales.howlyvelocity.managers.IgnoreManager;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.ChatUtils;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChatListener {

    private final ProxyServer server;
    private final PunishmentAPI punishmentAPI;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final IgnoreManager ignoreManager;
    private final ChatManager chatManager;
    private final GroupManager groupManager;

    public ChatListener(ProxyServer server, HowlyAPI api, TagManager tagManager,
                        MedalManager medalManager, IgnoreManager ignoreManager,
                        ChatManager chatManager, GroupManager groupManager) {
        this.server = server;
        this.punishmentAPI = api.getPunishmentAPI();
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.ignoreManager = ignoreManager;
        this.chatManager = chatManager;
        this.groupManager = groupManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Cancelar o evento padrão
        event.setResult(PlayerChatEvent.ChatResult.denied());

        // Verificar se o jogador está mutado
        CompletableFuture<Punishment> muteFuture = punishmentAPI.getActiveMute(player.getUniqueId());

        muteFuture.thenAccept(mute -> {
            if (mute != null) {
                // Jogador está mutado, informar ao jogador
                String timeRemaining = mute.isPermanent() ? "Permanente" : TimeUtils.formatDuration(mute.getRemainingTime());
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("\n§cVocê está silenciado.\n"));
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§fMotivo: §7" + mute.getReason()));
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("§fTempo restante: §7" + timeRemaining));
                player.sendMessage(LegacyComponentSerializer.legacySection().deserialize("\n§eVocê pode apelar no nosso discord §ndiscord.gg/howly§e.\n"));
            } else {
                // Jogador não está mutado, processar mensagem
                String coloredMessage = ChatUtils.applyColors(message, player);
                sendServerMessage(player, coloredMessage);
            }
        });
    }

    /**
     * Envia mensagem para todos os jogadores do mesmo servidor, respeitando a lista de ignorados
     */
    private void sendServerMessage(Player sender, String message) {
        Optional<ServerConnection> serverConnection = sender.getCurrentServer();
        if (serverConnection.isEmpty()) {
            return;
        }

        RegisteredServer currentServer = serverConnection.get().getServer();
        Component formattedMessage = formatMessage(sender, message);

        // Enviar para todos os jogadores no mesmo servidor, exceto os que estão ignorando o remetente
        for (Player player : currentServer.getPlayersConnected()) {
            // Verificar se o jogador não está ignorando o remetente
            if (!ignoreManager.isIgnoring(player.getUniqueId(), sender.getUniqueId())) {
                player.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * Formata mensagem para chat do servidor usando Components
     */
    private Component formatMessage(Player sender, String message) {
        Component finalMessage = Component.empty();

        // Medalha
        String medalSymbol = medalManager.getFormattedPlayerMedal(sender).trim();
        if (!medalSymbol.isEmpty()) {
            Component medalComponent = Component.text(medalSymbol)
                    .hoverEvent(Component.text("§fMedalha: " + medalSymbol + "\n§aAdquira em: howlymc.com"));
            finalMessage = finalMessage.append(medalComponent).append(Component.text(" "));
        }

        // Tag
        String tagFormatted = tagManager.getFormattedPlayerTag(sender).trim();
        if (!tagFormatted.isEmpty()) {
            Component tagComponent = Component.text(tagFormatted)
                    .hoverEvent(Component.text("§fTag: " + tagFormatted + "\n§aAdquira em: howlymc.com"));
            finalMessage = finalMessage.append(tagComponent).append(Component.text(" "));
        }

        // Grupo (prefixo)
        if (groupManager.isLuckPermsAvailable()) {
            String groupPrefix = groupManager.getPlayerGroupPrefix(sender);
            if (!groupPrefix.isEmpty()) {
                Component groupComponent = Component.text(groupPrefix)
                        .hoverEvent(Component.text("§fGrupo: " + groupManager.getPlayerGroupInfo(sender).getDisplayName()));
            finalMessage = finalMessage.append(groupComponent).append(Component.text(" "));
        }
}

        // Nome do jogador (usar cor do grupo se disponível, senão usar cor da tag)
        String nameColor;
        if (groupManager.isLuckPermsAvailable()) {
            nameColor = groupManager.getPlayerGroupNameColor(sender);
        } else {
            nameColor = tagManager.getPlayerNameColor(sender);
        }

        TextColor playerNameColor = getTextColorFromCode(nameColor);
        finalMessage = finalMessage.append(Component.text(sender.getUsername()).color(playerNameColor));

        // Adicionar mensagem com tooltip de data
        long timestamp = System.currentTimeMillis();
        String dateFormatted = TimeUtils.formatDate(timestamp).replace(" ", " às ");

        Component messageComponent = Component.text(": ", TextColor.color(170, 170, 170))
                .append(Component.text(message).color(TextColor.color(255, 255, 255)))
                .hoverEvent(Component.text("§7Enviada em §f" + dateFormatted + "§7."));

        finalMessage = finalMessage.append(messageComponent);

        return finalMessage;
    }





    private TextColor getTextColorFromCode(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return TextColor.color(255, 255, 255);
        }

        char code = colorCode.charAt(colorCode.length() - 1);
        return switch (code) {
            case '0' -> TextColor.color(0, 0, 0);          // Preto
            case '1' -> TextColor.color(0, 0, 170);        // Azul escuro
            case '2' -> TextColor.color(0, 170, 0);        // Verde escuro
            case '3' -> TextColor.color(0, 170, 170);      // Ciano
            case '4' -> TextColor.color(170, 0, 0);        // Vermelho escuro
            case '5' -> TextColor.color(170, 0, 170);      // Roxo
            case '6' -> TextColor.color(255, 170, 0);      // Dourado
            case '7' -> TextColor.color(170, 170, 170);    // Cinza
            case '8' -> TextColor.color(85, 85, 85);       // Cinza escuro
            case '9' -> TextColor.color(85, 85, 255);      // Azul
            case 'a' -> TextColor.color(85, 255, 85);      // Verde
            case 'b' -> TextColor.color(85, 255, 255);     // Azul claro
            case 'c' -> TextColor.color(255, 85, 85);      // Vermelho
            case 'd' -> TextColor.color(255, 85, 255);     // Rosa
            case 'e' -> TextColor.color(255, 255, 85);     // Amarelo
            case 'f' -> TextColor.color(255, 255, 255);    // Branco
            default -> TextColor.color(255, 255, 255);
        };
    }
}

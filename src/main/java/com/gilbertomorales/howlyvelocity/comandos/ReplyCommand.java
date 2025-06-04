package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.IgnoreManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.ChatUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextColor;
import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ReplyCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final IgnoreManager ignoreManager;
    private final TellCommand tellCommand;

    public ReplyCommand(ProxyServer server, TagManager tagManager, IgnoreManager ignoreManager, TellCommand tellCommand) {
        this.server = server;
        this.tagManager = tagManager;
        this.ignoreManager = ignoreManager;
        this.tellCommand = tellCommand;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("Apenas jogadores podem usar este comando.").color(TextColor.color(255, 85, 85)));
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Utilize: /r <mensagem>").color(TextColor.color(255, 85, 85)));
            return;
        }

        UUID lastSenderUUID = tellCommand.getLastMessageSender(sender.getUniqueId());
        if (lastSenderUUID == null) {
            sender.sendMessage(Component.text("Você não tem ninguém para responder.").color(TextColor.color(255, 85, 85)));
            return;
        }

        Optional<Player> targetOptional = server.getPlayer(lastSenderUUID);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("O jogador não está mais online.").color(TextColor.color(255, 85, 85)));
            return;
        }

        Player target = targetOptional.get();

        // Verificar se o destinatário está ignorando o remetente
        if (ignoreManager.isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(Component.text("Este jogador está te ignorando.").color(TextColor.color(255, 85, 85)));
            return;
        }

        // Construir a mensagem
        String rawMessage = String.join(" ", args);
        String message = ChatUtils.applyColors(rawMessage, sender);

        // Criar componentes formatados para remetente e destinatário
        Component senderMessage = createReplyMessage("para", target, sender, message);
        Component targetMessage = createReplyMessage("de", sender, target, message);

        // Enviar mensagens
        sender.sendMessage(senderMessage);
        target.sendMessage(targetMessage);

        // Atualizar para /r do destinatário
        tellCommand.getLastMessageSender(target.getUniqueId());
    }

    /**
     * Cria uma mensagem formatada para o sistema de reply
     */
    private Component createReplyMessage(String direction, Player displayPlayer, Player contextPlayer, String message) {
        Component finalMessage = Component.text("Mensagem " + direction + " ").color(TextColor.color(85, 85, 85));

        // Obter grupo do jogador que será exibido
        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        String groupPrefix = "";
        String nameColor = "§7";
        
        if (groupManager.isLuckPermsAvailable()) {
            groupPrefix = groupManager.getPlayerGroupPrefix(displayPlayer);
            nameColor = groupManager.getPlayerGroupNameColor(displayPlayer);
        }

        // Adicionar grupo se existir (SEM espaço extra se não tiver grupo)
        if (!groupPrefix.isEmpty()) {
            finalMessage = finalMessage.append(Component.text(groupPrefix + " "));
        }

        // Adicionar nome do jogador
        TextColor playerNameColor = getTextColorFromCode(nameColor);
        finalMessage = finalMessage.append(Component.text(displayPlayer.getUsername()).color(playerNameColor));

        // Adicionar dois pontos e mensagem
        finalMessage = finalMessage.append(Component.text(": ").color(TextColor.color(85, 85, 85)))
                .append(Component.text(message).color(TextColor.color(255, 255, 255)));

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

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.IgnoreManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.ChatUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

import java.util.*;

public class TellCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final IgnoreManager ignoreManager;
    private final Map<UUID, UUID> lastReplies = new HashMap<>();

    public TellCommand(ProxyServer server, TagManager tagManager, IgnoreManager ignoreManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.ignoreManager = ignoreManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("Apenas jogadores podem usar este comando.").color(TextColor.color(255, 85, 85)));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUtilize: /tell <jogador> <mensagem>").color(TextColor.color(255, 85, 85)));
            return;
        }

        String targetName = args[0];
        Optional<Player> targetOptional = server.getPlayer(targetName);

        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("Jogador não encontrado ou offline.").color(TextColor.color(255, 85, 85)));
            return;
        }

        Player target = targetOptional.get();

        // Não permitir enviar mensagem para si mesmo
        if (target.equals(sender)) {
            sender.sendMessage(Component.text("Você não pode enviar mensagens para si mesmo.").color(TextColor.color(255, 85, 85)));
            return;
        }

        // Verificar se o destinatário está ignorando o remetente
        if (ignoreManager.isIgnoring(target.getUniqueId(), sender.getUniqueId())) {
            sender.sendMessage(Component.text("Este jogador está te ignorando.").color(TextColor.color(255, 85, 85)));
            return;
        }

        // Construir a mensagem
        String rawMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String message = ChatUtils.applyColors(rawMessage, sender);

        // Criar componentes formatados para remetente e destinatário
        Component senderMessage = createTellMessage("para", target, sender, message);
        Component targetMessage = createTellMessage("de", sender, target, message);

        // Enviar mensagens
        sender.sendMessage(senderMessage);
        target.sendMessage(targetMessage);

        // Salvar para /r
        lastReplies.put(target.getUniqueId(), sender.getUniqueId());
    }

    /**
     * Cria uma mensagem formatada para o sistema de tell
     */
    private Component createTellMessage(String direction, Player displayPlayer, Player contextPlayer, String message) {
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
        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .toList();
        }
        return List.of();
    }

    public UUID getLastMessageSender(UUID playerUUID) {
        return lastReplies.get(playerUUID);
    }
}

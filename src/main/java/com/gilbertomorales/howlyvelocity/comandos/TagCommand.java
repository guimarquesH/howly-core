package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.stream.Collectors;

public class TagCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;

    public TagCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        String[] args = invocation.arguments();

        // Mostrar todas as tags disponíveis
        if (args.length == 0) {
            showAvailableTags(player);
            return;
        }

        // Selecionar uma tag
        String tagId = args[0].toLowerCase();

        if (!tagManager.hasTag(tagId)) {
            player.sendMessage(Component.text("§cTag não encontrada. Use /tag para ver as tags disponíveis."));
            return;
        }

        TagManager.TagInfo tagInfo = tagManager.getTagInfo(tagId);

        // Verificar permissão
        if (!tagInfo.getPermission().isEmpty() && !player.hasPermission(tagInfo.getPermission())) {
            player.sendMessage(Component.text("§cVocê não tem permissão para usar esta tag."));
            return;
        }

        // Definir a tag
        tagManager.setPlayerTag(player.getUniqueId(), tagId);

        if (tagId.equals("nenhuma")) {
            player.sendMessage(Component.text("§aTag removida com sucesso!"));
        } else {
            player.sendMessage(Component.text("§aTag alterada para " + tagInfo.getDisplay() + " §acom sucesso!"));
        }
    }

    private void showAvailableTags(Player player) {
        List<String> availableTags = tagManager.getPlayerAvailableTags(player);

        if (availableTags.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não tem nenhuma tag disponível."));
            return;
        }

        Component message = Component.text("§fTags disponíveis: ");

        for (int i = 0; i < availableTags.size(); i++) {
            String tagId = availableTags.get(i);
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(tagId);

            String displayText = tagId.equals("nenhuma") ? "§7[Nenhuma]" : tagInfo.getDisplay();
            String color = tagInfo.getNameColor();

            // Aqui, a sugestão de comando será clicável para o jogador
            Component tagComponent = Component.text(color + displayText)
                    .clickEvent(ClickEvent.runCommand("/tag " + tagId))  // Garante que o comando será executado
                    .hoverEvent(HoverEvent.showText(Component.text("§7Clique para selecionar esta tag")));

            message = message.append(tagComponent);

            if (i < availableTags.size() - 1) {
                message = message.append(Component.text("§7, "));
            }
        }

        message = message.append(Component.text("§7."));
        player.sendMessage(message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return List.of();
        }

        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();

            // Se a sugestão começar com o argumento atual do jogador, mostre as opções de tags.
            return tagManager.getPlayerAvailableTags(player).stream()
                    .filter(tag -> tag.toLowerCase().startsWith(arg))  // Filtra as sugestões que começam com o argumento
                    .collect(Collectors.toList());  // Coleta as sugestões
        }

        return List.of();
    }
}
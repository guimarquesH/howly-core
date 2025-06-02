package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.StringJoiner;

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
        player.sendMessage(Component.text("§aTag alterada para " + tagInfo.getDisplay() + " §acom sucesso!"));
    }

    private void showAvailableTags(Player player) {
        List<String> availableTags = tagManager.getPlayerAvailableTags(player);

        if (availableTags.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não tem nenhuma tag disponível."));
            return;
        }

        player.sendMessage(Component.text("§eUtilize /tag <tag> para alterar sua tag."));

        StringJoiner tagsJoiner = new StringJoiner(", ");
        for (String tagId : availableTags) {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(tagId);
            tagsJoiner.add(tagInfo.getDisplay());
        }

        player.sendMessage(Component.text("§fTags disponíveis: §7Nenhuma, " + tagsJoiner.toString()));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return List.of();
        }

        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();
            return tagManager.getPlayerAvailableTags(player).stream()
                .filter(tag -> tag.toLowerCase().startsWith(arg))
                .toList();
        }

        return List.of();
    }
}

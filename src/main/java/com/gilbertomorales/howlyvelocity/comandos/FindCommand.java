package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class FindCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;

    public FindCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.ajudante")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §cAjudante §cou superior para usar este comando."));
                return;
            }
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(Component.text("§cUtilize: /find <jogador>"));
            return;
        }

        Optional<Player> targetOptional = server.getPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("§cJogador não encontrado ou offline."));
            return;
        }

        Player target = targetOptional.get();
        String serverName = target.getCurrentServer()
            .map(connection -> connection.getServerInfo().getName())
            .orElse("Lobby");

        sender.sendMessage(Component.text("§eUsuário " + tagManager.getFormattedPlayerName(target) + " §eestá conectado em §n" + serverName + "§e."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return server.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(invocation.arguments()[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}

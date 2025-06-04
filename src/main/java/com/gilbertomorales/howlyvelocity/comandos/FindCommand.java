package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

import java.util.List;

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
            sender.sendMessage(Component.text("§cUtilize: /find <jogador/#id>"));
            return;
        }

        String targetIdentifier = args[0];
        sender.sendMessage(Component.text("§eBuscando usuário..."));

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                if (result.isOnline()) {
                    Player target = result.getOnlinePlayer();
                    String serverName = target.getCurrentServer()
                            .map(connection -> connection.getServerInfo().getName())
                            .orElse("Lobby");

                    GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
                    String formattedName = groupManager.getFormattedPlayerName(target);

                    sender.sendMessage(Component.text("§eUsuário " + formattedName + " §eestá conectado em §n" + serverName + "§e."));
                } else {
                    sender.sendMessage(Component.text("§cO usuário §f" + result.getName() + " §cestá offline."));
                }
            } else {
                sender.sendMessage(Component.text("§cUsuário não encontrado."));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
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
}

package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

public class SendCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;

    public SendCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.coordenador")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §cCoordenador §cou superior para usar este comando."));
                return;
            }
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            sender.sendMessage(Component.text("§cUtilize: /send <jogador> <servidor>"));
            return;
        }

        Optional<Player> targetOptional = server.getPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("§cJogador não encontrado ou offline."));
            return;
        }

        Optional<RegisteredServer> serverOptional = server.getServer(args[1]);
        if (serverOptional.isEmpty()) {
            sender.sendMessage(Component.text("§cServidor não encontrado."));
            return;
        }

        Player target = targetOptional.get();
        RegisteredServer targetServer = serverOptional.get();

        target.createConnectionRequest(targetServer).fireAndForget();

        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        String senderName = sender instanceof Player ? groupManager.getFormattedPlayerName((Player) sender) : "§4[CONSOLE]";
        String targetName = groupManager.getFormattedPlayerName(target);

        sender.sendMessage(Component.text("§aJogador " + targetName + " §aenviado para o servidor §e" + targetServer.getServerInfo().getName()));
        target.sendMessage(Component.text("§aVocê foi enviado para o servidor §e" + targetServer.getServerInfo().getName() + " §apor " + senderName));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(invocation.arguments()[0].toLowerCase()))
                    .toList();
        } else if (invocation.arguments().length == 2) {
            return server.getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(invocation.arguments()[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}

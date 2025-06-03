package com.gilbertomorales.howlyvelocity.comandos;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.List;

public class OnlineCommand implements SimpleCommand {

    private final ProxyServer server;
    private final String[] colors = {"§a", "§b", "§c", "§d", "§e", "§2", "§1", "§6", "§5", "§4"};

    public OnlineCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (!sender.hasPermission("howly.ajudante")) {
            sender.sendMessage(Component.text("§cVocê precisa ser do grupo §eAjudante §cou superior para usar este comando."));
            return;
        }

        int totalPlayers = server.getPlayerCount();
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eEstatísticas de usuários por servidor conectado:"));
        sender.sendMessage(Component.text(" "));

        int colorIndex = 0;
        for (RegisteredServer registeredServer : server.getAllServers()) {
            String serverName = registeredServer.getServerInfo().getName();
            int playerCount = registeredServer.getPlayersConnected().size();

            String color = colors[colorIndex % colors.length];
            double percentage = (playerCount * 100.0) / totalPlayers;
            int coloredSquares = (int) Math.round(percentage / 10.0);

            StringBuilder squares = new StringBuilder();
            for (int i = 0; i < coloredSquares; i++) {
                squares.append(color).append("■");
            }
            for (int i = coloredSquares; i < 10; i++) {
                squares.append("§8■");
            }

            String label = playerCount == 1 ? "usuário" : "usuários";
            sender.sendMessage(Component.text(String.format("§f%s: §7%d %s %s §7%.1f%%",
                    serverName, playerCount, label, squares.toString(), percentage)));

            colorIndex++;
        }

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fTotal de usuários conectados: §a" + totalPlayers));
        sender.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

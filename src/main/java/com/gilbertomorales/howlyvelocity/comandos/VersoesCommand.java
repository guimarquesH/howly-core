package com.gilbertomorales.howlyvelocity.comandos;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.stream.Collectors;

public class VersoesCommand implements SimpleCommand {

    private final ProxyServer server;
    private final String[] colors = {"§a", "§b", "§c", "§d", "§e", "§2", "§1", "§6", "§5", "§4"};

    public VersoesCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.gerente")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §4Gerente §cou superior para usar este comando."));
                return;
            }
        }

        Collection<Player> players = server.getAllPlayers();
        if (players.isEmpty()) {
            sender.sendMessage(Component.text("§cNenhum jogador online."));
            return;
        }

        // Contar versões
        Map<String, Integer> versionCounts = players.stream()
            .collect(Collectors.groupingBy(
                player -> player.getProtocolVersion().getName(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        // Ordenar por quantidade (decrescente)
        List<Map.Entry<String, Integer>> sortedVersions = versionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList();

        int totalPlayers = players.size();

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eEstatísticas de uso de versão dos usuários conectados:"));
        sender.sendMessage(Component.text(" "));

        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : sortedVersions) {
            String version = entry.getKey();
            int count = entry.getValue();
            double percentage = (count * 100.0) / totalPlayers;

            int coloredSquares = (int) Math.round(percentage / 10.0);
            
            String color = colors[colorIndex % colors.length];
            StringBuilder squares = new StringBuilder();

            for (int i = 0; i < coloredSquares; i++) {
                squares.append(color).append("■");
            }

            for (int i = coloredSquares; i < 10; i++) {
                squares.append("§8■");
            }

            sender.sendMessage(Component.text(String.format("§fv%s: §7%d %s %s §7%.1f%%",
                    version,
                    count,
                    count == 1 ? "usuário" : "usuários",
                    squares.toString(),
                    percentage
            )));

            colorIndex++;
        }
        
        sender.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

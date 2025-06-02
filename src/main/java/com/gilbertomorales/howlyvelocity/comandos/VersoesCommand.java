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
    private final String[] colors = {"§a", "§b", "§c", "§d", "§e", "§f", "§6", "§9", "§2", "§5"};

    public VersoesCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.helper")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §aHelper §cou superior para usar este comando."));
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
        sender.sendMessage(Component.text("§eEstatísticas de versões dos jogadores:"));
        sender.sendMessage(Component.text("§fTotal de jogadores: §a" + totalPlayers));
        sender.sendMessage(Component.text(" "));

        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : sortedVersions) {
            String version = entry.getKey();
            int count = entry.getValue();
            double percentage = (count * 100.0) / totalPlayers;
            
            // Calcular quantos quadradinhos colorir (de 10)
            int coloredSquares = (int) Math.round(percentage / 10.0);
            
            String color = colors[colorIndex % colors.length];
            StringBuilder squares = new StringBuilder();
            
            // Adicionar quadradinhos coloridos
            for (int i = 0; i < coloredSquares; i++) {
                squares.append(color).append("■");
            }
            
            // Adicionar quadradinhos vazios
            for (int i = coloredSquares; i < 10; i++) {
                squares.append("§8■");
            }
            
            sender.sendMessage(Component.text(String.format("§f%s: %s §7(§f%d §7jogadores - §f%.1f%%§7)",
                version, squares.toString(), count, percentage)));
            
            colorIndex++;
        }
        
        sender.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

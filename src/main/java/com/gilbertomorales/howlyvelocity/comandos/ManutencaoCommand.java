package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.MOTDManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

public class ManutencaoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final MOTDManager motdManager;

    public ManutencaoCommand(ProxyServer server, MOTDManager motdManager) {
        this.server = server;
        this.motdManager = motdManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("howly.gerente")) {
            source.sendMessage(Component.text("§cVocê precisa ser do grupo §4Gerente §cou superior para usar este comando."));
            return;
        }

        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "iniciar":
                startMaintenance(source);
                break;
            case "finalizar":
                endMaintenance(source);
                break;
            default:
                sendUsage(source);
                break;
        }
    }

    private void startMaintenance(CommandSource source) {
        if (motdManager.isInMaintenance()) {
            source.sendMessage(Component.text("§cO servidor já está em modo de manutenção."));
            return;
        }

        motdManager.setMaintenance(true);
        source.sendMessage(Component.text("§aModo de manutenção ativado com sucesso!"));
        
        // Kickar jogadores sem permissão
        kickPlayersWithoutPermission();
    }

    private void endMaintenance(CommandSource source) {
        if (!motdManager.isInMaintenance()) {
            source.sendMessage(Component.text("§cO servidor não está em modo de manutenção."));
            return;
        }

        motdManager.setMaintenance(false);
        source.sendMessage(Component.text("§aModo de manutenção desativado com sucesso!"));
    }

    private void kickPlayersWithoutPermission() {
        String kickMessage = motdManager.getMaintenanceKickMessage();
        
        for (Player player : server.getAllPlayers()) {
            if (!player.hasPermission("howly.coordenador")) {
                player.disconnect(LegacyComponentSerializer.legacySection().deserialize(kickMessage));
            }
        }
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§eUso do comando /manutencao:"));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§e/manutencao iniciar §8- §7Ativa o modo de manutenção"));
        source.sendMessage(Component.text("§e/manutencao finalizar §8- §7Desativa o modo de manutenção"));
        source.sendMessage(Component.text(""));
        
        // Mostrar status atual
        String status = motdManager.isInMaintenance() ? "§aAtivado" : "§cDesativado";
        source.sendMessage(Component.text("§fStatus atual: " + status));
        source.sendMessage(Component.text(""));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return List.of("iniciar", "finalizar").stream()
                    .filter(s -> s.startsWith(invocation.arguments()[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

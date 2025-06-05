package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.MOTDManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MOTDCommand implements SimpleCommand {

    private final MOTDManager motdManager;

    public MOTDCommand(MOTDManager motdManager) {
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
            case "ver":
                showCurrentMotd(source);
                break;
            case "definir":
                if (args.length < 2) {
                    source.sendMessage(Component.text("§cUtilize: /motd definir <mensagem>"));
                    return;
                }
                String newMotd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                setMotd(source, newMotd);
                break;
            case "padrao":
                resetMotd(source);
                break;
            default:
                sendUsage(source);
                break;
        }
    }

    private void showCurrentMotd(CommandSource source) {
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§eMOTD atual:"));
        source.sendMessage(Component.text("§fLinha 1: §r" + motdManager.getFirstLine()));
        source.sendMessage(Component.text("§fLinha 2: §r" + motdManager.getSecondLine()));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§eVisualização:"));
        source.sendMessage(Component.text(Cores.colorir(motdManager.getFirstLine())));
        source.sendMessage(Component.text(Cores.colorir(motdManager.getSecondLine())));
        source.sendMessage(Component.text(""));
    }

    private void setMotd(CommandSource source, String newMotd) {
        motdManager.setSecondLine(newMotd);
        source.sendMessage(Component.text("§aA segunda linha do MOTD foi alterada com sucesso!"));
        showCurrentMotd(source);
    }

    private void resetMotd(CommandSource source) {
        if (motdManager.isDefault()) {
            source.sendMessage(Component.text("§cO MOTD já está com a mensagem padrão."));
            return;
        }
        
        motdManager.resetToDefault();
        source.sendMessage(Component.text("§aO MOTD foi restaurado para a mensagem padrão."));
        showCurrentMotd(source);
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§eUso do comando /motd:"));
        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§e/motd ver §8- §7Exibe o MOTD atual"));
        source.sendMessage(Component.text("§e/motd definir <mensagem> §8- §7Define a segunda linha do MOTD"));
        source.sendMessage(Component.text("§e/motd padrao §8- §7Restaura o MOTD para a mensagem padrão"));
        source.sendMessage(Component.text(""));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return List.of("ver", "definir", "padrao").stream()
                    .filter(s -> s.startsWith(invocation.arguments()[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

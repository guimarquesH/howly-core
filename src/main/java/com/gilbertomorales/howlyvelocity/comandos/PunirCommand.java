package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PunirCommand implements SimpleCommand {

    private final ProxyServer server;
    private final PunishmentAPI punishmentAPI;

    public PunirCommand(ProxyServer server) {
        this.server = server;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (!sender.hasPermission("howly.moderador")) {
            sender.sendMessage(Component.text("§cVocê precisa ser do grupo §2Moderador §cou superior para usar este comando."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 3) {
            sendUsage(sender);
            return;
        }

        String action = args[0].toLowerCase();
        String playerName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Optional<Player> targetOptional = server.getPlayer(playerName);
        UUID targetUUID;
        
        if (targetOptional.isPresent()) {
            targetUUID = targetOptional.get().getUniqueId();
        } else {
            sender.sendMessage(Component.text("§cJogador não encontrado ou offline."));
            return;
        }

        String punisher = sender.getUsername();

        switch (action) {
            case "ban" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("§cUso: /punir ban <jogador> <tempo> <motivo>"));
                    return;
                }
                
                String timeStr = args[2];
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                long duration = TimeUtils.parseTimeString(timeStr);
                
                if (duration <= 0) {
                    sender.sendMessage(Component.text("§cTempo inválido! Use: 1d, 2h, 30m, etc."));
                    return;
                }
                
                punishmentAPI.banPlayer(targetUUID, reason, duration, punisher);
                sender.sendMessage(Component.text("§aJogador §f" + playerName + " §abanido por §e" + TimeUtils.formatDuration(duration) + " §apor: §f" + reason));
            }
            
            case "kick" -> {
                punishmentAPI.kickPlayer(targetUUID, reason, punisher);
                sender.sendMessage(Component.text("§aJogador §f" + playerName + " §akickado por: §f" + reason));
            }
            
            case "mute" -> {
                if (args.length < 4) {
                    sender.sendMessage(Component.text("§cUso: /punir mute <jogador> <tempo> <motivo>"));
                    return;
                }
                
                String timeStr = args[2];
                reason = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                long duration = TimeUtils.parseTimeString(timeStr);
                
                if (duration <= 0) {
                    sender.sendMessage(Component.text("§cTempo inválido! Use: 1d, 2h, 30m, etc."));
                    return;
                }
                
                punishmentAPI.mutePlayer(targetUUID, reason, duration, punisher);
                sender.sendMessage(Component.text("§aJogador §f" + playerName + " §amutado por §e" + TimeUtils.formatDuration(duration) + " §apor: §f" + reason));
            }
            
            default -> sendUsage(sender);
        }
    }

    private void sendUsage(Player sender) {
        sender.sendMessage(Component.text("§c§lUso do comando /punir:"));
        sender.sendMessage(Component.text("§e/punir ban <jogador> <tempo> <motivo>"));
        sender.sendMessage(Component.text("§e/punir kick <jogador> <motivo>"));
        sender.sendMessage(Component.text("§e/punir mute <jogador> <tempo> <motivo>"));
        sender.sendMessage(Component.text("§7Exemplo de tempo: 1d, 2h, 30m, 1w"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 1) {
            return List.of("ban", "kick", "mute").stream()
                .filter(action -> action.startsWith(args[0].toLowerCase()))
                .toList();
        } else if (args.length == 2) {
            return server.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("mute"))) {
            return List.of("1h", "2h", "1d", "3d", "7d", "30d");
        }
        
        return List.of();
    }
}

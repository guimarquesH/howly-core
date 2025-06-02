package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MuteCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PunishmentAPI punishmentAPI;
    private final Logger logger;

    public MuteCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
        this.logger = com.gilbertomorales.howlyvelocity.HowlyVelocity.getInstance().getLogger();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        // Verificar permissão apenas se for jogador
        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.moderador")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §2Moderador §cou superior para usar este comando."));
                return;
            }
        }

        String[] args = invocation.arguments();
        if (args.length < 3) {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§cUso: /mute <jogador> <tempo> <motivo>"));
                sender.sendMessage(Component.text("§7Exemplo de tempo: 1d, 2h, 30m, 1w, perm"));
            } else {
                logger.info(LogColor.error("Mute", "Uso: /mute <jogador> <tempo> <motivo>"));
                logger.info(LogColor.info("Mute", "Exemplo de tempo: 1d, 2h, 30m, 1w, perm"));
            }
            return;
        }

        String playerName = args[0];
        String timeStr = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Optional<Player> targetOptional = server.getPlayer(playerName);
        UUID targetUUID;
        
        if (targetOptional.isPresent()) {
            targetUUID = targetOptional.get().getUniqueId();
        } else {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§cJogador não encontrado ou offline."));
            } else {
                logger.info(LogColor.error("Mute", "Jogador não encontrado ou offline."));
            }
            return;
        }

        long duration = TimeUtils.parseTimeString(timeStr);
        if (duration == 0) {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§cTempo inválido! Use: 1d, 2h, 30m, etc. ou 'perm' para permanente."));
            } else {
                logger.info(LogColor.error("Mute", "Tempo inválido! Use: 1d, 2h, 30m, etc. ou 'perm' para permanente."));
            }
            return;
        }

        String punisher = sender instanceof Player ? ((Player) sender).getUsername() : "Console";
        Long durationMillis = duration == -1 ? null : duration;

        punishmentAPI.mutePlayer(targetUUID, reason, durationMillis, punisher).thenAccept(punishment -> {
            String timeDisplay = duration == -1 ? "Permanente" : TimeUtils.formatDuration(duration);
            
            if (targetOptional.isPresent()) {
                if (sender instanceof Player) {
                    sender.sendMessage(Component.text("§aJogador " + tagManager.getFormattedPlayerName(targetOptional.get()) + " §amutado por §e" + timeDisplay + " §apor: §f" + reason));
                } else {
                    logger.info(LogColor.success("Mute", "Jogador " + targetOptional.get().getUsername() + " mutado por " + timeDisplay + " por: " + reason));
                }
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(Component.text("§aJogador §f" + playerName + " §amutado por §e" + timeDisplay + " §apor: §f" + reason));
                } else {
                    logger.info(LogColor.success("Mute", "Jogador " + playerName + " mutado por " + timeDisplay + " por: " + reason));
                }
            }
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 1) {
            return server.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        } else if (args.length == 2) {
            return List.of("1h", "2h", "1d", "3d", "7d", "30d", "perm");
        }
        
        return List.of();
    }
}

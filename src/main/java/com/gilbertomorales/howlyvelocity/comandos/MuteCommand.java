package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MuteCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PlayerDataManager playerDataManager;
    private final HowlyAPI api;

    public MuteCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.playerDataManager = HowlyAPI.getInstance().getPlugin().getPlayerDataManager();
        this.api = HowlyAPI.getInstance();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("howly.gerente")) {
            source.sendMessage(Component.text("§cVocê precisa ser do grupo §4Gerente §cou superior para usar este comando."));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(Component.text(" "));
            source.sendMessage(Component.text("§eUtilize: /mute <jogador> <tempo> <motivo>"));
            source.sendMessage(Component.text(" "));
            source.sendMessage(Component.text("§fExemplo: §7/mute Jogador 7d Spam no chat"));
            source.sendMessage(Component.text("§fTempos: §7s (segundos), m (minutos), h (horas), d (dias), w (semanas), M (meses), permanent"));
            source.sendMessage(Component.text(" "));
            return;
        }

        final String targetName = args[0];
        final String timeArg = args[1];
        final String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (reason.isEmpty()) {
            source.sendMessage(Component.text("§cVocê precisa especificar um motivo para o silenciamento."));
            return;
        }

        // Obter nome do punidor
        final String punisherName;
        if (source instanceof Player) {
            punisherName = ((Player) source).getUsername();
        } else {
            punisherName = "Console";
        }

        // Processar tempo
        final Long duration;
        if (!timeArg.equalsIgnoreCase("permanent") && !timeArg.equalsIgnoreCase("perm")) {
            try {
                duration = TimeUtils.parseDuration(timeArg);
                if (duration == null || duration <= 0) {
                    source.sendMessage(Component.text("§cTempo inválido. Use: s, m, h, d, w, M ou 'permanent'."));
                    return;
                }
            } catch (Exception e) {
                source.sendMessage(Component.text("§cTempo inválido. Use: s, m, h, d, w, M ou 'permanent'."));
                return;
            }
        } else {
            duration = null;
        }

        // Primeiro, tentar encontrar o jogador online
        Optional<Player> targetOptional = server.getPlayer(targetName);

        if (targetOptional.isPresent()) {
            // Jogador está online
            Player target = targetOptional.get();
            mutePlayer(source, target.getUniqueId(), target.getUsername(), reason, duration, punisherName);
        } else {
            // Jogador está offline, buscar no banco de dados
            source.sendMessage(Component.text("§eBuscando jogador no banco de dados..."));

            playerDataManager.getPlayerUUID(targetName).thenAccept(uuid -> {
                if (uuid != null) {
                    // Jogador encontrado no banco de dados
                    playerDataManager.getPlayerName(uuid).thenAccept(correctName -> {
                        mutePlayer(source, uuid, correctName != null ? correctName : targetName, reason, duration, punisherName);
                    });
                } else {
                    // Jogador não encontrado
                    source.sendMessage(Component.text("§cJogador não encontrado no banco de dados."));
                }
            });
        }
    }

    private void mutePlayer(CommandSource source, UUID targetUUID, String targetName, String reason, Long duration, String punisherName) {
        CompletableFuture<Punishment> muteFuture = api.getPunishmentAPI().mutePlayer(targetUUID, reason, duration, punisherName);

        muteFuture.thenAccept(punishment -> {
            // Notificar staff
            String durationStr = duration == null ? "permanentemente" : "por " + TimeUtils.formatDuration(duration);
            final String muteMessage = "\n§c" + targetName + " §7foi silenciado por §c" + punisherName + "\n§7Motivo: §f" + reason + "\n";
            
            server.getAllPlayers().stream()
                    .filter(p -> p.hasPermission("howly.gerente"))
                    .forEach(p -> p.sendMessage(Component.text(muteMessage)));

            // Notificar quem executou o comando
            source.sendMessage(Component.text("§aJogador silenciado com sucesso!"));
        }).exceptionally(ex -> {
            source.sendMessage(Component.text("§cErro ao silenciar jogador: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String partialTime = args[1].toLowerCase();
            return List.of("1h", "1d", "7d", "30d", "permanent").stream()
                    .filter(time -> time.startsWith(partialTime))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}

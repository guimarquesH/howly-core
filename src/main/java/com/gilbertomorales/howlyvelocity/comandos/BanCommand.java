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

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class BanCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PlayerDataManager playerDataManager;
    private final HowlyAPI api;

    public BanCommand(ProxyServer server, TagManager tagManager) {
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
            source.sendMessage(legacySection().deserialize("§cVocê precisa ser do grupo §4Gerente §cou superior para usar este comando."));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(legacySection().deserialize(""));
            source.sendMessage(legacySection().deserialize("§eUtilize: /ban <usuário> <tempo> <motivo>"));
            source.sendMessage(legacySection().deserialize(""));
            source.sendMessage(legacySection().deserialize("§fExemplo: §7/ban Usuario 7d Motivo"));
            source.sendMessage(legacySection().deserialize("§fTempos: §7s (segundos), m (minutos), h (horas), d (dias), w (semanas), M (meses), permanente"));
            source.sendMessage(legacySection().deserialize(""));
            return;
        }

        final String targetName = args[0];
        final String timeArg = args[1];
        final String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (reason.isEmpty()) {
            source.sendMessage(legacySection().deserialize("§cVocê precisa especificar um motivo para a punição."));
            return;
        }

        final String punisherName = (source instanceof Player)
                ? ((Player) source).getUsername()
                : "Console";

        final Long duration;
        if (!timeArg.equalsIgnoreCase("permanent") && !timeArg.equalsIgnoreCase("perm") && !timeArg.equalsIgnoreCase("permanente")) {
            try {
                duration = TimeUtils.parseDuration(timeArg);
                if (duration == null || duration <= 0) {
                    source.sendMessage(legacySection().deserialize("§cTempo inválido. Use: s, m, h, d, w, M ou 'permanente'."));
                    return;
                }
            } catch (Exception e) {
                source.sendMessage(legacySection().deserialize("§cTempo inválido. Use: s, m, h, d, w, M ou 'permanente'."));
                return;
            }
        } else {
            duration = null;
        }

        Optional<Player> targetOptional = server.getPlayer(targetName);

        if (targetOptional.isPresent()) {
            Player target = targetOptional.get();
            banUser(source, target.getUniqueId(), target.getUsername(), reason, duration, punisherName);
        } else {
            source.sendMessage(legacySection().deserialize("§eBuscando usuário no banco de dados..."));
            playerDataManager.getPlayerUUID(targetName).thenAccept(uuid -> {
                if (uuid != null) {
                    playerDataManager.getPlayerName(uuid).thenAccept(correctName -> {
                        banUser(source, uuid, correctName != null ? correctName : targetName, reason, duration, punisherName);
                    });
                } else {
                    source.sendMessage(legacySection().deserialize("§cUsuário não encontrado no banco de dados."));
                }
            });
        }
    }

    private void banUser(CommandSource source, UUID targetUUID, String targetName, String reason, Long duration, String punisherName) {
        CompletableFuture<Punishment> banFuture = api.getPunishmentAPI().banPlayer(targetUUID, reason, duration, punisherName);

        banFuture.thenAccept(punishment -> {
            String banMessage = "\n§c" + targetName + " foi banido por " + punisherName + ".\n§cMotivo: " + reason + "\n";
            server.getAllPlayers().stream()
                    .filter(p -> p.hasPermission("howly.ajudante"))
                    .forEach(p -> p.sendMessage(legacySection().deserialize(banMessage)));

            source.sendMessage(legacySection().deserialize("§aUsuário banido com sucesso!"));
        }).exceptionally(ex -> {
            source.sendMessage(legacySection().deserialize("§cErro ao aplicar punição: " + ex.getMessage()));
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

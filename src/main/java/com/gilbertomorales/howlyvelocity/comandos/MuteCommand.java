package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

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
            source.sendMessage(Component.text("§eUtilize: /mute <jogador/#id> <tempo> <motivo>"));
            source.sendMessage(Component.text(" "));
            source.sendMessage(Component.text("§fExemplo: §7/mute Jogador 7d Spam no chat"));
            source.sendMessage(Component.text("§fExemplo: §7/mute #123 7d Spam no chat"));
            source.sendMessage(Component.text("§fTempos: §7s (segundos), m (minutos), h (horas), d (dias), w (semanas), M (meses), permanent"));
            source.sendMessage(Component.text(" "));
            return;
        }

        final String targetIdentifier = args[0];
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
        if (!timeArg.equalsIgnoreCase("permanent") && !timeArg.equalsIgnoreCase("perm") && !timeArg.equalsIgnoreCase("permanente")) {
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

        source.sendMessage(Component.text("§eBuscando jogador..."));

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                mutePlayer(source, result.getUUID(), result.getName(), reason, duration, punisherName);
            } else {
                source.sendMessage(Component.text("§cJogador não encontrado."));
            }
        }).exceptionally(ex -> {
            source.sendMessage(Component.text("§cErro ao buscar jogador: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void mutePlayer(CommandSource source, java.util.UUID targetUUID, String targetName, String reason, Long duration, String punisherName) {
        CompletableFuture<Punishment> muteFuture = api.getPunishmentAPI().mutePlayer(targetUUID, reason, duration, punisherName);

        muteFuture.thenAccept(punishment -> {
            String durationStr = duration == null ? "Permanente" : TimeUtils.formatDuration(duration);
            final String muteMessage = "\n§c" + targetName + " foi silenciado por " + punisherName + ".\n§cMotivo: " + reason + "\n";

            server.getAllPlayers().stream()
                    .filter(p -> p.hasPermission("howly.ajudante"))
                    .forEach(p -> p.sendMessage(legacySection().deserialize(muteMessage)));

            // Confirmar para quem executou
            source.sendMessage(legacySection().deserialize("§aUsuário silenciado com sucesso!"));
        }).exceptionally(ex -> {
            source.sendMessage(legacySection().deserialize("§cErro ao silenciar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            List<String> suggestions = server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
            
            // Adicionar sugestões de ID se começar com #
            if (partialName.startsWith("#")) {
                suggestions.addAll(List.of("#1", "#2", "#3", "#4", "#5"));
            }
            
            return suggestions;
        } else if (args.length == 2) {
            String partialTime = args[1].toLowerCase();
            return List.of("1h", "1d", "7d", "30d", "permanent").stream()
                    .filter(time -> time.startsWith(partialTime))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}

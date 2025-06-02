package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class InfoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final PunishmentAPI punishmentAPI;

    public InfoCommand(ProxyServer server, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
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

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(Component.text("§cUso: /info <jogador>"));
            return;
        }

        Optional<Player> targetOptional = server.getPlayer(args[0]);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("§cJogador não encontrado ou offline."));
            return;
        }

        Player target = targetOptional.get();
        String serverName = target.getCurrentServer()
            .map(connection -> connection.getServerInfo().getName())
            .orElse("Desconhecido");

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações de " + tagManager.getFormattedPlayerName(target) + "§e:"));
        sender.sendMessage(Component.text("§fUUID: §7" + target.getUniqueId().toString()));
        sender.sendMessage(Component.text("§fServidor atual: §7" + serverName));
        sender.sendMessage(Component.text("§fPing: §7" + target.getPing() + "ms"));
        sender.sendMessage(Component.text("§fVersão: §7" + target.getProtocolVersion().getName()));
        sender.sendMessage(Component.text("§fMedalha: §7" + medalManager.getPlayerMedal(target)));

        // Verificar punições ativas
        punishmentAPI.getActiveBan(target.getUniqueId()).thenAccept(ban -> {
            punishmentAPI.getActiveMute(target.getUniqueId()).thenAccept(mute -> {
                if (ban != null || mute != null) {
                    sender.sendMessage(Component.text(" "));
                    sender.sendMessage(Component.text("§cPunições ativas:"));
                    
                    if (ban != null) {
                        String timeRemaining = ban.isPermanent() ? "Permanente" : TimeUtils.formatDuration(ban.getRemainingTime());
                        sender.sendMessage(Component.text("§fBanimento: §c" + ban.getReason()));
                        sender.sendMessage(Component.text("§fTempo restante: §7" + timeRemaining));
                        sender.sendMessage(Component.text("§fPunidor: §7" + ban.getPunisher()));
                    }
                    
                    if (mute != null) {
                        String timeRemaining = mute.isPermanent() ? "Permanente" : TimeUtils.formatDuration(mute.getRemainingTime());
                        sender.sendMessage(Component.text("§fMute: §c" + mute.getReason()));
                        sender.sendMessage(Component.text("§fTempo restante: §7" + timeRemaining));
                        sender.sendMessage(Component.text("§fPunidor: §7" + mute.getPunisher()));
                    }
                }
                sender.sendMessage(Component.text(" "));
            });
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            return server.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase().startsWith(invocation.arguments()[0].toLowerCase()))
                .toList();
        }
        return List.of();
    }
}

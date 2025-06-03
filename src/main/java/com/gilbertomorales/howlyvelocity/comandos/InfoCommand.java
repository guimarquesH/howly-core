package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
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
    private final PlayerDataManager playerDataManager;
    private final PunishmentAPI punishmentAPI;

    public InfoCommand(ProxyServer server, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.playerDataManager = HowlyAPI.getInstance().getPlugin().getPlayerDataManager();
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.ajudante")) {
                sender.sendMessage(Component.text("§cVocê precisa ser do grupo §eAjudante §cou superior para usar este comando."));
                return;
            }
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(Component.text("§cUtilize: /info <jogador>"));
            return;
        }

        String targetName = args[0];

        // Primeiro, tentar encontrar o jogador online
        Optional<Player> targetOptional = server.getPlayer(targetName);

        if (targetOptional.isPresent()) {
            // Jogador está online
            Player target = targetOptional.get();
            showPlayerInfo(sender, target, target.getUsername());
        } else {
            // Jogador está offline, buscar no banco de dados
            sender.sendMessage(Component.text("§eBuscando jogador no banco de dados..."));

            playerDataManager.getPlayerUUID(targetName).thenAccept(uuid -> {
                if (uuid != null) {
                    // Jogador encontrado no banco de dados
                    playerDataManager.getPlayerName(uuid).thenAccept(correctName -> {
                        showOfflinePlayerInfo(sender, uuid, correctName != null ? correctName : targetName);
                    });
                } else {
                    // Jogador não encontrado
                    sender.sendMessage(Component.text("§cJogador não encontrado no banco de dados."));
                }
            });
        }
    }

    private void showPlayerInfo(CommandSource sender, Player target, String targetName) {
        String serverName = target.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("Desconhecido");

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fUsuário: §7" + targetName));
        sender.sendMessage(Component.text("§fGrupo: §7Indefinido"));
        sender.sendMessage(Component.text("§fUUID: §7" + target.getUniqueId().toString()));
        sender.sendMessage(Component.text("§fID: §cVINCULA AQUI MACACO"));
        sender.sendMessage(Component.text(" "));

        String currentTagId = tagManager.getCurrentPlayerTag(target.getUniqueId());
        if (currentTagId == null || currentTagId.isEmpty()) {
            sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
        } else {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(currentTagId);
            sender.sendMessage(Component.text("§fTag: " + tagInfo.getDisplay()));
        }

        sender.sendMessage(Component.text("§fMedalha: §7" + medalManager.getPlayerMedal(target)));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fConexão: §7" + serverName));
        sender.sendMessage(Component.text("§fVersão: §7" + target.getProtocolVersion().getName()));
        sender.sendMessage(Component.text("§fPing: §7" + target.getPing() + "ms"));
        sender.sendMessage(Component.text(" "));

        // Buscar informações de login
        playerDataManager.getPlayerLoginInfo(target.getUniqueId()).thenAccept(loginInfo -> {
            if (loginInfo != null) {
                long firstJoin = loginInfo[0];
                long lastJoin = loginInfo[1];

                sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin) + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin) + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
            } else {
                sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                sender.sendMessage(Component.text("§fÚltimo login: §7Agora"));
            }
            sender.sendMessage(Component.text(" "));

            // Verificar punições
            checkPunishments(sender, target.getUniqueId());
        });
    }

    private void showOfflinePlayerInfo(CommandSource sender, java.util.UUID uuid, String targetName) {
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fUsuário: §7" + targetName));
        sender.sendMessage(Component.text("§fGrupo: §7Indefinido"));
        sender.sendMessage(Component.text("§fUUID: §7" + uuid.toString()));
        sender.sendMessage(Component.text("§fID: §cVINCULA AQUI MACACO"));
        sender.sendMessage(Component.text(" "));

        String currentTagId = tagManager.getCurrentPlayerTag(uuid);
        if (currentTagId == null || currentTagId.isEmpty()) {
            sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
        } else {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(currentTagId);
            sender.sendMessage(Component.text("§fTag: " + tagInfo.getDisplay()));
        }

        sender.sendMessage(Component.text("§fMedalha: §7Offline"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fConexão: §cOffline"));
        sender.sendMessage(Component.text("§fVersão: §7Desconhecida"));
        sender.sendMessage(Component.text("§fPing: §7N/A"));
        sender.sendMessage(Component.text(" "));

        // Buscar informações de login
        playerDataManager.getPlayerLoginInfo(uuid).thenAccept(loginInfo -> {
            if (loginInfo != null) {
                long firstJoin = loginInfo[0];
                long lastJoin = loginInfo[1];

                sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin) + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin) + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
            } else {
                sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                sender.sendMessage(Component.text("§fÚltimo login: §7Desconhecido"));
            }
            sender.sendMessage(Component.text(" "));

            // Verificar punições
            checkPunishments(sender, uuid);
        });
    }

    private void checkPunishments(CommandSource sender, java.util.UUID uuid) {
        punishmentAPI.getActiveBan(uuid).thenAccept(ban -> {
            punishmentAPI.getActiveMute(uuid).thenAccept(mute -> {
                if (ban != null || mute != null) {
                    sender.sendMessage(Component.text("§cPunições ativas:"));

                    if (ban != null) {
                        String timeRemaining = ban.isPermanent() ? "Permanente" : TimeUtils.formatDuration(ban.getRemainingTime());
                        sender.sendMessage(Component.text("§fBanimento: §c" + ban.getReason()));
                        sender.sendMessage(Component.text("§fTempo restante: §7" + timeRemaining));
                        sender.sendMessage(Component.text("§fAutor: §7" + ban.getPunisher()));
                    }

                    if (mute != null) {
                        String timeRemaining = mute.isPermanent() ? "Permanente" : TimeUtils.formatDuration(mute.getRemainingTime());
                        sender.sendMessage(Component.text("§fMute: §c" + mute.getReason()));
                        sender.sendMessage(Component.text("§fTempo restante: §7" + timeRemaining));
                        sender.sendMessage(Component.text("§fAutor: §7" + mute.getPunisher()));
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

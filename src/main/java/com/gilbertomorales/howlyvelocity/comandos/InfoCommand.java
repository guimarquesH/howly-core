package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.PlaytimeManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;

import java.util.List;
import java.util.Optional;

public class InfoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final MedalManager medalManager;
    private final PlayerDataManager playerDataManager;
    private final PunishmentAPI punishmentAPI;
    private final PlaytimeManager playtimeManager;

    public InfoCommand(ProxyServer server, TagManager tagManager, MedalManager medalManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.medalManager = medalManager;
        this.playerDataManager = HowlyAPI.getInstance().getPlugin().getPlayerDataManager();
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
        this.playtimeManager = HowlyAPI.getInstance().getPlugin().getPlaytimeManager();
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
            sender.sendMessage(Component.text("§cUtilize: /info <jogador> ou /info #<id>"));
            return;
        }

        String targetIdentifier = args[0];

        // Verificar se é busca por ID (#1, #2, etc.)
        if (targetIdentifier.startsWith("#")) {
            try {
                int playerId = Integer.parseInt(targetIdentifier.substring(1));
                showPlayerInfoById(sender, playerId);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("§cID inválido. Use /info #<número>"));
            }
            return;
        }

        // Busca por nome
        Optional<Player> targetOptional = server.getPlayer(targetIdentifier);

        if (targetOptional.isPresent()) {
            Player target = targetOptional.get();
            showPlayerInfo(sender, target, target.getUsername());
        } else {
            // Jogador está offline, buscar no banco de dados
            sender.sendMessage(Component.text("§eBuscando jogador no banco de dados..."));

            playerDataManager.getPlayerUUID(targetIdentifier).thenAccept(uuid -> {
                if (uuid != null) {
                    // Jogador encontrado no banco de dados
                    playerDataManager.getPlayerInfo(uuid).thenAccept(playerInfo -> {
                        if (playerInfo != null) {
                            showOfflinePlayerInfo(sender, playerInfo);
                        } else {
                            sender.sendMessage(Component.text("§cErro ao carregar informações do jogador."));
                        }
                    });
                } else {
                    // Jogador não encontrado
                    sender.sendMessage(Component.text("§cJogador não encontrado no banco de dados."));
                }
            });
        }
    }

    private void showPlayerInfoById(CommandSource sender, int playerId) {
        sender.sendMessage(Component.text("§eBuscando jogador com ID #" + playerId + "..."));

        playerDataManager.getPlayerInfoById(playerId).thenAccept(playerInfo -> {
            if (playerInfo != null) {
                // Verificar se o jogador está online
                Optional<Player> onlinePlayer = server.getPlayer(playerInfo.getUuid());
                if (onlinePlayer.isPresent()) {
                    showPlayerInfo(sender, onlinePlayer.get(), playerInfo.getName(), playerInfo.getId());
                } else {
                    showOfflinePlayerInfo(sender, playerInfo);
                }
            } else {
                sender.sendMessage(Component.text("§cJogador com ID #" + playerId + " não encontrado."));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar jogador: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void showPlayerInfo(CommandSource sender, Player target, String targetName) {
        // Buscar ID do jogador
        playerDataManager.getPlayerId(target.getUniqueId()).thenAccept(playerId -> {
            if (playerId != null) {
                showPlayerInfo(sender, target, targetName, playerId);
            } else {
                // Se não conseguir buscar o ID, mostrar sem ele
                showPlayerInfoWithoutId(sender, target, targetName);
            }
        }).exceptionally(ex -> {
            // Em caso de erro, mostrar sem o ID
            showPlayerInfoWithoutId(sender, target, targetName);
            return null;
        });
    }

    private void showPlayerInfo(CommandSource sender, Player target, String targetName, Integer playerId) {
        String serverName = target.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("Desconhecido");

        // Obter informações do grupo
        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        String formattedGroups = groupManager.getFormattedPlayerGroups(target);

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fUsuário: §7" + targetName));
        sender.sendMessage(Component.text("§fGrupo(s): " + formattedGroups));
        sender.sendMessage(Component.text("§fID: §7#" + playerId));
        sender.sendMessage(Component.text("§fUUID: §7" + target.getUniqueId().toString()));
        sender.sendMessage(Component.text(" "));

        String currentTagId = tagManager.getCurrentPlayerTag(target.getUniqueId());
        if (currentTagId == null || currentTagId.isEmpty()) {
            sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
        } else {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(currentTagId);
            if (tagInfo != null) {
                sender.sendMessage(Component.text("§fTag: " + tagInfo.getDisplay()));
            } else {
                sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
            }
        }

        // Buscar medalha do jogador
        String medal = medalManager.getPlayerMedal(target);
        if (medal.isEmpty()) {
            sender.sendMessage(Component.text("§fMedalha: §7Nenhuma"));
        } else {
            sender.sendMessage(Component.text("§fMedalha: " + medal));
        }

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fConexão: §7" + serverName));
        sender.sendMessage(Component.text("§fVersão: §7" + target.getProtocolVersion().getName()));
        sender.sendMessage(Component.text("§fPing: §7" + target.getPing() + "ms"));
        sender.sendMessage(Component.text(" "));

        // Buscar tempo online
        playtimeManager.getPlayerPlaytime(target.getUniqueId()).thenAccept(playtime -> {
            String formattedTime = playtimeManager.formatPlaytime(playtime);
            sender.sendMessage(Component.text("§fTempo online: §7" + formattedTime));
            sender.sendMessage(Component.text(" "));
            
            // Buscar informações de login
            playerDataManager.getPlayerLoginInfo(target.getUniqueId()).thenAccept(loginInfo -> {
                if (loginInfo != null) {
                    long firstJoin = loginInfo[0];
                    long lastJoin = loginInfo[1];

                    sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
                } else {
                    sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7Agora"));
                }
                sender.sendMessage(Component.text(" "));

                // Verificar punições
                checkPunishments(sender, target.getUniqueId());
            });
        }).exceptionally(ex -> {
            // Em caso de erro, continuar com o resto das informações
            sender.sendMessage(Component.text("§fTempo online: §7Erro ao carregar"));
            sender.sendMessage(Component.text(" "));
            
            // Buscar informações de login
            playerDataManager.getPlayerLoginInfo(target.getUniqueId()).thenAccept(loginInfo -> {
                if (loginInfo != null) {
                    long firstJoin = loginInfo[0];
                    long lastJoin = loginInfo[1];

                    sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
                } else {
                    sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7Agora"));
                }
                sender.sendMessage(Component.text(" "));

                // Verificar punições
                checkPunishments(sender, target.getUniqueId());
            });
            
            return null;
        });
    }

    private void showPlayerInfoWithoutId(CommandSource sender, Player target, String targetName) {
        String serverName = target.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("Desconhecido");

        // Obter informações do grupo
        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        String formattedGroups = groupManager.getFormattedPlayerGroups(target);

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fUsuário: §7" + targetName));
        sender.sendMessage(Component.text("§fGrupo(s): " + formattedGroups));
        sender.sendMessage(Component.text("§fID: §7Carregando..."));
        sender.sendMessage(Component.text("§fUUID: §7" + target.getUniqueId().toString()));
        sender.sendMessage(Component.text(" "));

        String currentTagId = tagManager.getCurrentPlayerTag(target.getUniqueId());
        if (currentTagId == null || currentTagId.isEmpty()) {
            sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
        } else {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(currentTagId);
            if (tagInfo != null) {
                sender.sendMessage(Component.text("§fTag: " + tagInfo.getDisplay()));
            } else {
                sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
            }
        }

        // Buscar medalha do jogador
        String medal = medalManager.getPlayerMedal(target);
        if (medal.isEmpty()) {
            sender.sendMessage(Component.text("§fMedalha: §7Nenhuma"));
        } else {
            sender.sendMessage(Component.text("§fMedalha: " + medal));
        }

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fConexão: §7" + serverName));
        sender.sendMessage(Component.text("§fVersão: §7" + target.getProtocolVersion().getName()));
        sender.sendMessage(Component.text("§fPing: §7" + target.getPing() + "ms"));
        sender.sendMessage(Component.text(" "));

        // Buscar tempo online
        playtimeManager.getPlayerPlaytime(target.getUniqueId()).thenAccept(playtime -> {
            String formattedTime = playtimeManager.formatPlaytime(playtime);
            sender.sendMessage(Component.text("§fTempo online: §7" + formattedTime));
            sender.sendMessage(Component.text(" "));
            
            // Buscar informações de login
            playerDataManager.getPlayerLoginInfo(target.getUniqueId()).thenAccept(loginInfo -> {
                if (loginInfo != null) {
                    long firstJoin = loginInfo[0];
                    long lastJoin = loginInfo[1];

                    sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
                } else {
                    sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7Agora"));
                }
                sender.sendMessage(Component.text(" "));

                // Verificar punições
                checkPunishments(sender, target.getUniqueId());
            });
        }).exceptionally(ex -> {
            // Em caso de erro, continuar com o resto das informações
            sender.sendMessage(Component.text("§fTempo online: §7Erro ao carregar"));
            sender.sendMessage(Component.text(" "));
            
            // Buscar informações de login
            playerDataManager.getPlayerLoginInfo(target.getUniqueId()).thenAccept(loginInfo -> {
                if (loginInfo != null) {
                    long firstJoin = loginInfo[0];
                    long lastJoin = loginInfo[1];

                    sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
                } else {
                    sender.sendMessage(Component.text("§fPrimeiro login: §7Desconhecido"));
                    sender.sendMessage(Component.text("§fÚltimo login: §7Agora"));
                }
                sender.sendMessage(Component.text(" "));

                // Verificar punições
                checkPunishments(sender, target.getUniqueId());
            });
            
            return null;
        });
    }

    private void showOfflinePlayerInfo(CommandSource sender, PlayerDataManager.PlayerInfo playerInfo) {
        // Obter informações do grupo para jogador offline
        GroupManager groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
        String formattedGroups = groupManager.getFormattedPlayerGroupsByUUID(playerInfo.getUuid());

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eInformações:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fUsuário: §7" + playerInfo.getName()));
        sender.sendMessage(Component.text("§fGrupo(s): " + formattedGroups));
        sender.sendMessage(Component.text("§fID: §7#" + playerInfo.getId()));
        sender.sendMessage(Component.text("§fUUID: §7" + playerInfo.getUuid().toString()));
        sender.sendMessage(Component.text(" "));

        String currentTagId = tagManager.getCurrentPlayerTag(playerInfo.getUuid());
        if (currentTagId == null || currentTagId.isEmpty()) {
            sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
        } else {
            TagManager.TagInfo tagInfo = tagManager.getTagInfo(currentTagId);
            if (tagInfo != null) {
                sender.sendMessage(Component.text("§fTag: " + tagInfo.getDisplay()));
            } else {
                sender.sendMessage(Component.text("§fTag: §7Nenhuma"));
            }
        }

        // Buscar medalha do jogador offline
        String medal = medalManager.getPlayerMedalByUUID(playerInfo.getUuid());
        if (medal.isEmpty()) {
            sender.sendMessage(Component.text("§fMedalha: §7Nenhuma"));
        } else {
            sender.sendMessage(Component.text("§fMedalha: " + medal));
        }

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§fConexão: §cOffline"));
        sender.sendMessage(Component.text("§fVersão: §7Desconhecida"));
        sender.sendMessage(Component.text("§fPing: §7N/A"));
        sender.sendMessage(Component.text(" "));

        // Buscar tempo online
        playtimeManager.getPlayerPlaytime(playerInfo.getUuid()).thenAccept(playtime -> {
            String formattedTime = playtimeManager.formatPlaytime(playtime);
            sender.sendMessage(Component.text("§fTempo online: §7" + formattedTime));
            sender.sendMessage(Component.text(" "));
            
            long firstJoin = playerInfo.getFirstJoin();
            long lastJoin = playerInfo.getLastJoin();

            sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
            sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
            sender.sendMessage(Component.text(" "));

            // Verificar punições
            checkPunishments(sender, playerInfo.getUuid());
        }).exceptionally(ex -> {
            // Em caso de erro, continuar com o resto das informações
            sender.sendMessage(Component.text("§fTempo online: §7Erro ao carregar"));
            sender.sendMessage(Component.text(" "));
            
            long firstJoin = playerInfo.getFirstJoin();
            long lastJoin = playerInfo.getLastJoin();

            sender.sendMessage(Component.text("§fPrimeiro login: §7" + TimeUtils.formatDate(firstJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(firstJoin) + ")"));
            sender.sendMessage(Component.text("§fÚltimo login: §7" + TimeUtils.formatDate(lastJoin).replace(" ", " às ") + " (" + TimeUtils.getTimeAgo(lastJoin) + ")"));
            sender.sendMessage(Component.text(" "));

            // Verificar punições
            checkPunishments(sender, playerInfo.getUuid());
            
            return null;
        });
    }

    private void checkPunishments(CommandSource sender, java.util.UUID uuid) {
        punishmentAPI.getActiveBan(uuid).thenAccept(ban -> {
            punishmentAPI.getActiveMute(uuid).thenAccept(mute -> {
                if (ban != null || mute != null) {
                    sender.sendMessage(Component.text("§cPunições ativas:\n"));

                    if (ban != null) {
                        String timeRemaining = ban.isPermanent() ? "Permanente" : TimeUtils.formatDuration(ban.getRemainingTime());
                        sender.sendMessage(Component.text("§fTipo: §cBAN"));
                        sender.sendMessage(Component.text("§fMotivo: §7" + ban.getReason()));
                        sender.sendMessage(Component.text("§fTempo restante: §7" + timeRemaining));
                        sender.sendMessage(Component.text("§fAutor: §7" + ban.getPunisher()));
                    }

                    if (ban != null && mute != null) {
                        sender.sendMessage(Component.text(" "));
                    }

                    if (mute != null) {
                        String timeRemaining = mute.isPermanent() ? "Permanente" : TimeUtils.formatDuration(mute.getRemainingTime());
                        sender.sendMessage(Component.text("§fTipo: §cMUTE"));
                        sender.sendMessage(Component.text("§fMotivo: §7" + mute.getReason()));
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
            String arg = invocation.arguments()[0].toLowerCase();
            
            // Sugerir apenas jogadores online
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .toList();
        }
        return List.of();
    }
}

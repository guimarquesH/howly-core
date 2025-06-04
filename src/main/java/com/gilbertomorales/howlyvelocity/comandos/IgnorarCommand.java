package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.IgnoreManager;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class IgnorarCommand implements SimpleCommand {

    private final ProxyServer server;
    private final IgnoreManager ignoreManager;
    private final PlayerDataManager playerDataManager;
    private final TagManager tagManager;

    public IgnorarCommand(ProxyServer server, IgnoreManager ignoreManager,
                          PlayerDataManager playerDataManager, TagManager tagManager) {
        this.server = server;
        this.ignoreManager = ignoreManager;
        this.playerDataManager = playerDataManager;
        this.tagManager = tagManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendUsage(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add", "adicionar" -> handleAdd(player, args);
            case "remove", "remover" -> handleRemove(player, args);
            case "list", "lista" -> handleList(player);
            case "clear", "limpar" -> handleClear(player);
            default -> sendUsage(player);
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§cUtilize: /ignorar add <jogador/#id>"));
            return;
        }

        String targetIdentifier = args[1];

        if (targetIdentifier.equals(player.getUsername()) || 
            (targetIdentifier.startsWith("#") && isPlayerOwnId(player, targetIdentifier))) {
            player.sendMessage(Component.text("§cVocê não pode ignorar a si mesmo."));
            return;
        }

        player.sendMessage(Component.text("§eBuscando usuário..."));

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                UUID targetUuid = result.getUUID();
                
                if (targetUuid.equals(player.getUniqueId())) {
                    player.sendMessage(Component.text("§cVocê não pode ignorar a si mesmo."));
                    return;
                }

                boolean added = ignoreManager.addIgnoredPlayer(player.getUniqueId(), targetUuid);

                if (added) {
                    if (result.isOnline()) {
                        Player target = result.getOnlinePlayer();
                        String formattedName = tagManager.getPlayerTag(target) + " " +
                                tagManager.getPlayerNameColor(target) + target.getUsername();
                        player.sendMessage(Component.text("§aVocê agora está ignorando " + formattedName + "§a."));
                    } else {
                        player.sendMessage(Component.text("§aVocê agora está ignorando §f" + result.getName() + "§a."));
                    }
                } else {
                    player.sendMessage(Component.text("§cVocê já está ignorando este jogador."));
                }
            } else {
                player.sendMessage(Component.text("§cUsuário não encontrado."));
            }
        }).exceptionally(ex -> {
            player.sendMessage(Component.text("§cErro ao buscar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("§cUtilize: /ignorar remover <jogador/#id>"));
            return;
        }

        String targetIdentifier = args[1];
        player.sendMessage(Component.text("§eBuscando usuário..."));

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                UUID targetUuid = result.getUUID();
                boolean removed = ignoreManager.removeIgnoredPlayer(player.getUniqueId(), targetUuid);

                if (removed) {
                    if (result.isOnline()) {
                        Player target = result.getOnlinePlayer();
                        String formattedName = tagManager.getPlayerTag(target) + " " +
                                tagManager.getPlayerNameColor(target) + target.getUsername();
                        player.sendMessage(Component.text("§aVocê não está mais ignorando " + formattedName + "§a."));
                    } else {
                        player.sendMessage(Component.text("§aVocê não está mais ignorando §f" + result.getName() + "§a."));
                    }
                } else {
                    player.sendMessage(Component.text("§cVocê não estava ignorando este jogador."));
                }
            } else {
                player.sendMessage(Component.text("§cUsuário não encontrado."));
            }
        }).exceptionally(ex -> {
            player.sendMessage(Component.text("§cErro ao buscar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void handleList(Player player) {
        Set<UUID> ignoredPlayers = ignoreManager.getIgnoredPlayers(player.getUniqueId());

        if (ignoredPlayers.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não está ignorando nenhum jogador."));
            return;
        }

        player.sendMessage(Component.text("§eJogadores ignorados (" + ignoredPlayers.size() + "):"));

        // Buscar nomes dos jogadores ignorados
        for (UUID ignoredUuid : ignoredPlayers) {
            Player ignoredPlayer = server.getPlayer(ignoredUuid).orElse(null);
            if (ignoredPlayer != null) {
                String formattedName = tagManager.getPlayerTag(ignoredPlayer) + " " +
                        tagManager.getPlayerNameColor(ignoredPlayer) + ignoredPlayer.getUsername();
                player.sendMessage(Component.text("§7- " + formattedName + " §a(Online)"));
            } else {
                playerDataManager.getPlayerName(ignoredUuid).thenAccept(name -> {
                    if (name != null) {
                        player.sendMessage(Component.text("§7- §f" + name + " §c(Offline)"));
                    }
                });
            }
        }
    }

    private void handleClear(Player player) {
        Set<UUID> ignoredPlayers = ignoreManager.getIgnoredPlayers(player.getUniqueId());

        if (ignoredPlayers.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não está ignorando nenhum jogador."));
            return;
        }

        ignoreManager.clearIgnoreList(player.getUniqueId());
        player.sendMessage(Component.text("§aLista de jogadores ignorados limpa com sucesso!"));
    }

    private boolean isPlayerOwnId(Player player, String idString) {
        try {
            int id = Integer.parseInt(idString.substring(1));
            // Verificar se o ID corresponde ao próprio jogador (implementação simplificada)
            return false; // Por enquanto, retorna false para evitar complexidade
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("§e/ignorar add <jogador/#id> §8- §7Ignora as mensagens de um jogador"));
        player.sendMessage(Component.text("§e/ignorar remover <jogador/#id> §8- §7Deixa de ignorar um jogador"));
        player.sendMessage(Component.text("§e/ignorar lista §8- §7Exibe a lista de jogadores ignorados"));
        player.sendMessage(Component.text("§e/ignorar limpar §8- §7Remove todos os jogadores da lista de ignorados"));
        player.sendMessage(Component.text(""));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("add", "remover", "lista", "limpar").stream()
                    .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remover"))) {
            String arg = args[1].toLowerCase();
            List<String> suggestions = server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .collect(Collectors.toList());
            
            // Adicionar sugestões de ID se começar com #
            if (arg.startsWith("#")) {
                suggestions.addAll(List.of("#1", "#2", "#3", "#4", "#5"));
            }
            
            return suggestions;
        }

        return List.of();
    }
}

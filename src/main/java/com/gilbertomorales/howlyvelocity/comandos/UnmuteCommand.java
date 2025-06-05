package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UnmuteCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PlayerDataManager playerDataManager;
    private final HowlyAPI api;

    public UnmuteCommand(ProxyServer server, TagManager tagManager) {
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

        if (args.length < 1) {
            source.sendMessage(Component.text("§cUtilize: /unmute <jogador/#id>"));
            return;
        }

        String targetIdentifier = args[0];

        // Obter nome do unmuter
        String unmuterName;
        if (source instanceof Player) {
            unmuterName = ((Player) source).getUsername();
        } else {
            unmuterName = "Console";
        }

        source.sendMessage(Component.text("§eBuscando jogador..."));

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                unmutePlayer(source, result.getUUID(), result.getName(), unmuterName);
            } else {
                source.sendMessage(Component.text("§cJogador não encontrado."));
            }
        }).exceptionally(ex -> {
            source.sendMessage(Component.text("§cErro ao buscar jogador: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void unmutePlayer(CommandSource source, java.util.UUID targetUUID, String targetName, String unmuterName) {
        // Verificar se o jogador está mutado
        api.getPunishmentAPI().isPlayerMuted(targetUUID).thenAccept(isMuted -> {
            if (!isMuted) {
                source.sendMessage(Component.text("§cEste jogador não está silenciado."));
                return;
            }

            // Desmutar jogador
            CompletableFuture<Boolean> unmuteFuture = api.getPunishmentAPI().unmutePlayer(targetUUID, unmuterName);

            unmuteFuture.thenAccept(success -> {
                if (success) {
                    // Notificar staff
                    String unmuteMessage = "\n§e" + targetName + " §efoi desmutado por §a" + unmuterName + "§e." + "\n";

                    server.getAllPlayers().stream()
                            .filter(p -> p.hasPermission("howly.ajudante"))
                            .forEach(p -> p.sendMessage(Component.text(unmuteMessage)));

                    // Notificar jogador se estiver online
                    server.getPlayer(targetUUID).ifPresent(player -> {
                        player.sendMessage(Component.text("§aVocê foi desmutado por §f" + unmuterName + "§a."));
                    });

                    // Notificar quem executou o comando
                    source.sendMessage(Component.text("§aJogador desmutado com sucesso!"));
                } else {
                    source.sendMessage(Component.text("§cNão foi possível desmutar o jogador."));
                }
            }).exceptionally(ex -> {
                source.sendMessage(Component.text("§cErro ao desmutar jogador: " + ex.getMessage()));
                ex.printStackTrace();
                return null;
            });
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
        }

        return List.of();
    }
}

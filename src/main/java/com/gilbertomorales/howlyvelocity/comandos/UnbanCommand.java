package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UnbanCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PunishmentAPI punishmentAPI;
    private final Logger logger;

    public UnbanCommand(ProxyServer server, TagManager tagManager) {
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
        if (args.length == 0) {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§cUso: /unban <jogador>"));
            } else {
                logger.info(LogColor.error("Unban", "Uso: /unban <jogador>"));
            }
            return;
        }

        String playerName = args[0];
        String unbanner = sender instanceof Player ? ((Player) sender).getUsername() : "Console";

        // Tentar encontrar o jogador online primeiro
        Optional<Player> targetOptional = server.getPlayer(playerName);
        if (targetOptional.isPresent()) {
            UUID targetUUID = targetOptional.get().getUniqueId();
            
            punishmentAPI.unbanPlayer(targetUUID, unbanner).thenAccept(success -> {
                if (success) {
                    if (sender instanceof Player) {
                        sender.sendMessage(Component.text("§aJogador " + tagManager.getFormattedPlayerName(targetOptional.get()) + " §afoi desbanido com sucesso!"));
                    } else {
                        logger.info(LogColor.success("Unban", "Jogador " + targetOptional.get().getUsername() + " foi desbanido com sucesso!"));
                    }
                } else {
                    if (sender instanceof Player) {
                        sender.sendMessage(Component.text("§cJogador não está banido ou não foi encontrado."));
                    } else {
                        logger.info(LogColor.error("Unban", "Jogador não está banido ou não foi encontrado."));
                    }
                }
            });
        } else {
            // TODO: Implementar busca por nome no banco de dados
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("§cJogador não encontrado. Implemente busca por nome no banco de dados."));
            } else {
                logger.info(LogColor.error("Unban", "Jogador não encontrado. Implemente busca por nome no banco de dados."));
            }
        }
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

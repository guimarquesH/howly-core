package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class KickCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;
    private final PunishmentAPI punishmentAPI;
    private final Logger logger;

    public KickCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
        this.logger = com.gilbertomorales.howlyvelocity.HowlyVelocity.getInstance().getLogger();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();

        if (sender instanceof Player player) {
            if (!player.hasPermission("howly.moderador")) {
                sender.sendMessage(legacySection().deserialize("§cVocê precisa ser do grupo §2Moderador §cou superior para usar este comando."));
                return;
            }
        }

        String[] args = invocation.arguments();
        if (args.length < 2) {
            if (sender instanceof Player) {
                sender.sendMessage(legacySection().deserialize("§eUtilize: /kick <usuário/#id> <motivo>"));
            } else {
                logger.info(LogColor.error("Kick", "Uso: /kick <usuário/#id> <motivo>"));
            }
            return;
        }

        String targetIdentifier = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (sender instanceof Player) {
            sender.sendMessage(legacySection().deserialize("§eBuscando usuário..."));
        }

        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                if (result.isOnline()) {
                    Player target = result.getOnlinePlayer();
                    String punisher = sender instanceof Player ? ((Player) sender).getUsername() : "Console";

                    punishmentAPI.kickPlayer(target.getUniqueId(), reason, punisher).thenAccept(punishment -> {
                        if (sender instanceof Player) {
                            String kickMessage = "\n§c" + target.getUsername() + " foi expulso por " + punisher + ".\n§cMotivo: " + reason + "\n";

                            // Envia apenas para a staff
                            server.getAllPlayers().stream()
                                    .filter(p -> p.hasPermission("howly.ajudante"))
                                    .forEach(p -> p.sendMessage(legacySection().deserialize(kickMessage)));

                            sender.sendMessage(legacySection().deserialize("§aUsuário expulso com sucesso!"));

                        } else {
                            logger.info(LogColor.success("Kick", "Usuário " + target.getUsername() + " expulso por: " + reason));
                        }
                    });
                } else {
                    if (sender instanceof Player) {
                        sender.sendMessage(legacySection().deserialize("§cO usuário precisa estar online para ser expulso."));
                    } else {
                        logger.info(LogColor.error("Kick", "Usuário precisa estar online para ser expulso."));
                    }
                }
            } else {
                if (sender instanceof Player) {
                    sender.sendMessage(legacySection().deserialize("§cUsuário não encontrado."));
                } else {
                    logger.info(LogColor.error("Kick", "Usuário não encontrado."));
                }
            }
        }).exceptionally(ex -> {
            if (sender instanceof Player) {
                sender.sendMessage(legacySection().deserialize("§cErro ao buscar usuário: " + ex.getMessage()));
            } else {
                logger.error(LogColor.error("Kick", "Erro ao buscar usuário: " + ex.getMessage()));
            }
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();
            List<String> suggestions = server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .toList();
            
            // Adicionar sugestões de ID se começar com #
            if (arg.startsWith("#")) {
                suggestions.addAll(List.of("#1", "#2", "#3", "#4", "#5"));
            }
            
            return suggestions;
        }
        return List.of();
    }
}

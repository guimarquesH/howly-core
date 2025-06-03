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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                sender.sendMessage(legacySection().deserialize("§eUtilize: /kick <usuário> <motivo>"));
            } else {
                logger.info(LogColor.error("Kick", "Uso: /kick <usuário> <motivo>"));
            }
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Optional<Player> targetOptional = server.getPlayer(playerName);
        if (targetOptional.isEmpty()) {
            if (sender instanceof Player) {
                sender.sendMessage(legacySection().deserialize("§cUsuário não encontrado ou offline."));
            } else {
                logger.info(LogColor.error("Kick", "Usuário não encontrado ou offline."));
            }
            return;
        }

        Player target = targetOptional.get();
        UUID targetUUID = target.getUniqueId();
        String punisher = sender instanceof Player ? ((Player) sender).getUsername() : "Console";

        punishmentAPI.kickPlayer(targetUUID, reason, punisher).thenAccept(punishment -> {
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

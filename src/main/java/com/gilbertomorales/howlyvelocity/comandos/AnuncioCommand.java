package com.gilbertomorales.howlyvelocity.comandos;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;

public class AnuncioCommand implements SimpleCommand {

    private final ProxyServer server;

    public AnuncioCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (!sender.hasPermission("howly.coordenador")) {
            sender.sendMessage(Component.text("§cVocê precisa ser do grupo Coordenador §cou superior para usar este comando."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sender.sendMessage(Component.text("§cUtilize: /anuncio <mensagem>"));
            return;
        }

        String message = String.join(" ", args);
        String formattedMessage = Cores.colorir("&d&l[ANÚNCIO] &f" + message);

        server.getAllPlayers().forEach(player -> {
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text(formattedMessage));
            player.sendMessage(Component.text(" "));
        });

        sender.sendMessage(Component.text("§aAnúncio enviado para todos os jogadores online."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

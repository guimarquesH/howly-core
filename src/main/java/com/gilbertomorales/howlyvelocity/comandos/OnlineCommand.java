package com.gilbertomorales.howlyvelocity.comandos;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.List;

public class OnlineCommand implements SimpleCommand {

    private final ProxyServer server;

    public OnlineCommand(ProxyServer server) {
        this.server = server;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (!sender.hasPermission("howly.helper")) {
            sender.sendMessage(Component.text("§cVocê precisa ser do grupo §aHelper §cou superior para usar este comando."));
            return;
        }

        int totalPlayers = server.getPlayerCount();
        sender.sendMessage(Component.text("§eInformações de jogadores online:"));
        sender.sendMessage(Component.text("§fTotal de jogadores online: §a" + totalPlayers));
        sender.sendMessage(Component.text(""));

        for (RegisteredServer registeredServer : server.getAllServers()) {
            String serverName = registeredServer.getServerInfo().getName();
            int playerCount = registeredServer.getPlayersConnected().size();
            
            sender.sendMessage(Component.text("§fServidor §e" + serverName + "§f: §a" + playerCount + " §fjogadores"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

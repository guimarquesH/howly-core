package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.ChatManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

public class GlobalCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ChatManager chatManager;

    public GlobalCommand(ProxyServer server, ChatManager chatManager) {
        this.server = server;
        this.chatManager = chatManager;
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
            // Alternar modo de chat global
            chatManager.toggleGlobalChat(player);
        } else {
            // Enviar mensagem diretamente no chat global
            String message = String.join(" ", args);
            chatManager.sendMessage(player, message);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of();
    }
}

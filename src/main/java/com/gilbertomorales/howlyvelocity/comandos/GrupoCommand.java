package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.GroupManager;
import com.gilbertomorales.howlyvelocity.utils.TitleAPI;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GrupoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final GroupManager groupManager;

    public GrupoCommand(ProxyServer server, GroupManager groupManager) {
        this.server = server;
        this.groupManager = groupManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("§cEste comando só pode ser executado por jogadores."));
            return;
        }

        if (!sender.hasPermission("howly.gerente")) {
            sender.sendMessage(Component.text("§cVocê precisa ser do grupo §4Gerente §cou superior para executar este comando."));
            return;
        }

        if (!groupManager.isLuckPermsAvailable()) {
            sender.sendMessage(Component.text("§cLuckPerms não está disponível. Sistema de grupos desabilitado."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String action = args[0].toLowerCase();

        if (action.equals("listar")) {
            listGroups(sender);
            return;
        }

        if (args.length < 3) {
            sendUsage(sender);
            return;
        }

        String targetName = args[1];
        String groupName = args[2].toLowerCase();

        Optional<Player> targetOptional = server.getPlayer(targetName);
        if (targetOptional.isEmpty()) {
            sender.sendMessage(Component.text("§cEste usuário não está online."));
            return;
        }

        Player target = targetOptional.get();

        if (!groupManager.groupExists(groupName)) {
            sender.sendMessage(Component.text("§cO grupo especificado não existe."));
            return;
        }

        switch (action) {
            case "adicionar", "add" -> addPlayerToGroup(sender, target, groupName);
            case "remover", "remove" -> removePlayerFromGroup(sender, target, groupName);
            case "definir", "set" -> setPlayerGroup(sender, target, groupName);
            default -> sendUsage(sender);
        }
    }

    private void addPlayerToGroup(Player sender, Player target, String groupName) {
        groupManager.addPlayerToGroup(target, groupName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(Component.text("§eUsuário \"" + target.getUsername() + "\" adicionado ao grupo \"" + groupName + "\"."));
                
                if (groupManager.isVipGroup(groupName)) {
                    sendVipTitle(target, groupName);
                }
            } else {
                sender.sendMessage(Component.text("§cEste usuário já está no grupo \"" + groupName + "\" ou ocorreu um erro."));
            }
        });
    }

    private void removePlayerFromGroup(Player sender, Player target, String groupName) {
        groupManager.removePlayerFromGroup(target, groupName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(Component.text("§eUsuário \"" + target.getUsername() + "\" removido do grupo \"" + groupName + "\"."));
            } else {
                sender.sendMessage(Component.text("§cEste usuário não pertence ao grupo \"" + groupName + "\" ou ocorreu um erro."));
            }
        });
    }

    private void setPlayerGroup(Player sender, Player target, String groupName) {
        groupManager.setPlayerGroup(target, groupName).thenAccept(success -> {
            if (success) {
                sender.sendMessage(Component.text("§eGrupo do usuário \"" + target.getUsername() + "\" definido como \"" + groupName + "\"."));
                
                if (groupManager.isVipGroup(groupName)) {
                    sendVipTitle(target, groupName);
                }
            } else {
                sender.sendMessage(Component.text("§cO jogador já pertence ao grupo \"" + groupName + "\" ou ocorreu um erro."));
            }
        });
    }

    private void listGroups(Player sender) {
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eGrupos disponíveis:"));
        sender.sendMessage(Component.text(" "));
        
        Map<String, GroupManager.GroupInfo> groups = groupManager.getAvailableGroups();
        
        // Ordenar por prioridade (maior para menor)
        groups.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getPriority(), a.getValue().getPriority()))
                .forEach(entry -> {
                    String groupId = entry.getKey();
                    GroupManager.GroupInfo info = entry.getValue();
                    
                    if (!groupId.equals("default")) {
                        String vipIndicator = info.isVip() ? " §7(VIP)" : "";
                        sender.sendMessage(Component.text("§7- " + info.getFormattedPrefix() + vipIndicator));
                    }
                });
        
        sender.sendMessage(Component.text(" "));
    }

    private void sendVipTitle(Player target, String groupName) {
        GroupManager.GroupInfo groupInfo = groupManager.getAvailableGroups().get(groupName);
        if (groupInfo == null || !groupInfo.isVip()) {
            return;
        }

        String title = groupInfo.getColor() + target.getUsername();
        String subtitle = "§f tornou-se " + groupInfo.getFormattedPrefix();

        // Enviar title para todos os jogadores
        for (Player player : server.getAllPlayers()) {
            TitleAPI.sendTitle(player, title, subtitle);
        }
    }

    private void sendUsage(Player sender) {
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§eUso do comando /grupo:"));
        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("§f/grupo listar §7- Lista todos os grupos disponíveis"));
        sender.sendMessage(Component.text("§f/grupo adicionar <usuário> <grupo> §7- Adiciona um usuário ao grupo"));
        sender.sendMessage(Component.text("§f/grupo remover <usuário> <grupo> §7- Remove um usuário do grupo"));
        sender.sendMessage(Component.text("§f/grupo definir <usuário> <grupo> §7- Define o grupo principal do usuário"));
        sender.sendMessage(Component.text(" "));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("adicionar", "remover", "definir", "listar").stream()
                    .filter(action -> action.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("listar")) {
            return server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            return groupManager.getAvailableGroups().keySet().stream()
                    .filter(group -> !group.equals("default"))
                    .filter(group -> group.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}

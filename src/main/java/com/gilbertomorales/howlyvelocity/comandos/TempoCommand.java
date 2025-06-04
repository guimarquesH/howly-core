package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.managers.GroupManager;
import com.gilbertomorales.howlyvelocity.managers.PlaytimeManager;
import com.gilbertomorales.howlyvelocity.utils.PlayerUtils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TempoCommand implements SimpleCommand {

    private final ProxyServer server;
    private final PlaytimeManager playtimeManager;
    private final GroupManager groupManager;

    public TempoCommand(ProxyServer server, PlaytimeManager playtimeManager) {
        this.server = server;
        this.playtimeManager = playtimeManager;
        this.groupManager = HowlyAPI.getInstance().getPlugin().getGroupManager();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player sender)) {
            source.sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        if (args.length == 0) {
            // Mostrar tempo próprio
            showPlayerTime(sender, sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "top" -> showTopPlaytime(sender);
            case "resetar", "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("§cUtilize: /tempo resetar <jogador/#id>"));
                    return;
                }
                
                if (!sender.hasPermission("howly.gerente")) {
                    sender.sendMessage(Component.text("§cVocê precisa ser do grupo §4Gerente §cou superior para usar este comando."));
                    return;
                }
                
                resetPlayerTime(sender, args[1]);
            }
            default -> {
                // Mostrar tempo de outro jogador
                showOtherPlayerTime(sender, args[0]);
            }
        }
    }

    private void showPlayerTime(Player sender, Player target) {
        sender.sendMessage(Component.text("§eBuscando tempo online..."));
        
        playtimeManager.getPlayerPlaytime(target.getUniqueId()).thenAccept(playtime -> {
            String formattedTime = playtimeManager.formatPlaytime(playtime);
            String formattedName = groupManager.getFormattedPlayerName(target);
            
            if (sender.equals(target)) {
                sender.sendMessage(Component.text("§eSeu tempo online: §f" + formattedTime));
            } else {
                sender.sendMessage(Component.text("§eTempo online de " + formattedName + "§e: §f" + formattedTime));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar tempo online: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void showOtherPlayerTime(Player sender, String targetIdentifier) {
        sender.sendMessage(Component.text("§eBuscando usuário..."));
        
        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                playtimeManager.getPlayerPlaytime(result.getUUID()).thenAccept(playtime -> {
                    String formattedTime = playtimeManager.formatPlaytime(playtime);
                    String playerName = result.getName();
                    
                    if (result.isOnline()) {
                        String formattedName = groupManager.getFormattedPlayerName(result.getOnlinePlayer());
                        sender.sendMessage(Component.text("§eTempo online de " + formattedName + "§e: §f" + formattedTime));
                    } else {
                        sender.sendMessage(Component.text("§eTempo online de §f" + playerName + "§e: §f" + formattedTime));
                    }
                }).exceptionally(ex -> {
                    sender.sendMessage(Component.text("§cErro ao buscar tempo online: " + ex.getMessage()));
                    ex.printStackTrace();
                    return null;
                });
            } else {
                sender.sendMessage(Component.text("§cUsuário não encontrado."));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void showTopPlaytime(Player sender) {
        sender.sendMessage(Component.text("§eBuscando ranking de tempo online..."));
        
        playtimeManager.getTopPlaytime().thenAccept(topList -> {
            if (topList.isEmpty()) {
                sender.sendMessage(Component.text("§cNenhum dado de tempo online encontrado."));
                return;
            }
            
            sender.sendMessage(Component.text(" "));
            sender.sendMessage(Component.text("§e§lTOP 10 - TEMPO ONLINE"));
            sender.sendMessage(Component.text(" "));
            
            for (int i = 0; i < topList.size(); i++) {
                PlaytimeManager.PlaytimeEntry entry = topList.get(i);
                String formattedTime = playtimeManager.formatPlaytime(entry.getPlaytime());
                
                String position = "§7" + (i + 1) + "º";
                
                // Obter o grupo do jogador
                String groupPrefix = "";
                String nameColor = "§f";
                
                // Verificar se o jogador está online para obter o grupo
                Optional<Player> onlinePlayer = server.getPlayer(entry.getPlayerUuid());
                if (onlinePlayer.isPresent() && groupManager.isLuckPermsAvailable()) {
                    GroupManager.GroupInfo groupInfo = groupManager.getPlayerGroupInfo(onlinePlayer.get());
                    groupPrefix = groupManager.getPlayerGroupPrefix(onlinePlayer.get());
                    nameColor = groupManager.getPlayerGroupNameColor(onlinePlayer.get());
                    
                    if (!groupPrefix.isEmpty()) {
                        groupPrefix += " ";
                    }
                }
                
                String playerName = nameColor + entry.getPlayerName();
                String time = "§a" + formattedTime;
                
                sender.sendMessage(Component.text(position + " " + groupPrefix + playerName + " §7- " + time));
            }
            
            sender.sendMessage(Component.text(" "));
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar ranking: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    private void resetPlayerTime(Player sender, String targetIdentifier) {
        sender.sendMessage(Component.text("§eBuscando usuário..."));
        
        PlayerUtils.findPlayer(server, targetIdentifier).thenAccept(result -> {
            if (result != null) {
                playtimeManager.resetPlayerPlaytime(result.getUUID()).thenAccept(success -> {
                    if (success) {
                        String playerName = result.getName();
                        sender.sendMessage(Component.text("§aTempo online de §f" + playerName + " §afoi resetado com sucesso!"));
                        
                        // Notificar o jogador se estiver online
                        if (result.isOnline()) {
                            Player target = result.getOnlinePlayer();
                            String senderName = groupManager.getFormattedPlayerName(sender);
                            target.sendMessage(Component.text("§eSeu tempo online foi resetado por " + senderName + "§e."));
                        }
                    } else {
                        sender.sendMessage(Component.text("§cNão foi possível resetar o tempo online do jogador."));
                    }
                }).exceptionally(ex -> {
                    sender.sendMessage(Component.text("§cErro ao resetar tempo online: " + ex.getMessage()));
                    ex.printStackTrace();
                    return null;
                });
            } else {
                sender.sendMessage(Component.text("§cUsuário não encontrado."));
            }
        }).exceptionally(ex -> {
            sender.sendMessage(Component.text("§cErro ao buscar usuário: " + ex.getMessage()));
            ex.printStackTrace();
            return null;
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 1) {
            String arg = args[0].toLowerCase();
            List<String> suggestions = List.of("top", "resetar").stream()
                    .filter(cmd -> cmd.startsWith(arg))
                    .collect(Collectors.toList());
            
            // Adicionar jogadores online
            suggestions.addAll(server.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .collect(Collectors.toList()));
            
            // Adicionar sugestões de ID se começar com #
            if (arg.startsWith("#")) {
                suggestions.addAll(List.of("#1", "#2", "#3", "#4", "#5"));
            }
            
            return suggestions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("resetar")) {
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

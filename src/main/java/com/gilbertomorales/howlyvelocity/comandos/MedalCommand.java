package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;

public class MedalCommand implements SimpleCommand {

    private final ProxyServer server;
    private final MedalManager medalManager;

    public MedalCommand(ProxyServer server, MedalManager medalManager) {
        this.server = server;
        this.medalManager = medalManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("§cApenas jogadores podem usar este comando."));
            return;
        }

        String[] args = invocation.arguments();
        
        // Mostrar todas as medalhas disponíveis
        if (args.length == 0) {
            showAvailableMedals(player);
            return;
        }

        // Selecionar uma medalha
        String medalId = args[0].toLowerCase();
        
        if (!medalManager.hasMedal(medalId)) {
            player.sendMessage(Component.text("§cMedalha não encontrada. Use /medalha para ver as medalhas disponíveis."));
            return;
        }
        
        MedalManager.MedalInfo medalInfo = medalManager.getMedalInfo(medalId);
        
        // Verificar permissão
        if (!medalInfo.getPermission().isEmpty() && !player.hasPermission(medalInfo.getPermission())) {
            player.sendMessage(Component.text("§cVocê não tem permissão para usar esta medalha."));
            return;
        }
        
        // Definir a medalha
        medalManager.setPlayerMedal(player.getUniqueId(), medalId);
        
        if (medalId.equals("nenhuma")) {
            player.sendMessage(Component.text("§aMedalha removida com sucesso!"));
        } else {
            player.sendMessage(Component.text("§aMedalha alterada para " + medalInfo.getColoredSymbol() + " §acom sucesso!"));
        }
    }

    private void showAvailableMedals(Player player) {
        List<String> availableMedals = medalManager.getPlayerAvailableMedals(player);
        
        if (availableMedals.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não tem nenhuma medalha disponível."));
            return;
        }
        
        Component message = Component.text("§fMedalhas disponíveis: ");
        
        for (int i = 0; i < availableMedals.size(); i++) {
            String medalId = availableMedals.get(i);
            MedalManager.MedalInfo medalInfo = medalManager.getMedalInfo(medalId);
            
            String displayText = medalId.equals("nenhuma") ? "§7[Nenhuma]" : "[" + medalInfo.getColoredSymbol() + "§f]";
            
            Component medalComponent = Component.text(displayText)
                .clickEvent(ClickEvent.runCommand("/medalha " + medalId))
                .hoverEvent(HoverEvent.showText(Component.text("§7Clique para selecionar esta medalha")));
            
            message = message.append(medalComponent);
            
            if (i < availableMedals.size() - 1) {
                message = message.append(Component.text("§7, "));
            }
        }
        
        message = message.append(Component.text("§7."));
        player.sendMessage(message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return List.of();
        }
        
        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();
            return medalManager.getPlayerAvailableMedals(player).stream()
                .filter(medal -> medal.toLowerCase().startsWith(arg))
                .toList();
        }
        
        return List.of();
    }
}

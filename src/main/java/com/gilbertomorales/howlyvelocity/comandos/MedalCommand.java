package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.StringJoiner;

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

        String subCommand = args[0].toLowerCase();

        // Comando para remover medalha
        if (subCommand.equals("remover") || subCommand.equals("remove")) {
            medalManager.removePlayerMedal(player.getUniqueId());
            player.sendMessage(Component.text("§aMedalha removida com sucesso! Agora você não possui nenhuma medalha."));
            return;
        }

        // Selecionar uma medalha
        String medalId = subCommand;

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
            // Criar componente com símbolo e cor
            Component medalComponent = Component.text(medalInfo.getSymbol()).color(getTextColorFromCode(medalInfo.getColor()));
            Component message = Component.text("§aMedalha alterada para ")
                    .append(medalComponent)
                    .append(Component.text(" §acom sucesso!"));

            player.sendMessage(message);
        }
    }

    private void showAvailableMedals(Player player) {
        List<String> availableMedals = medalManager.getPlayerAvailableMedals(player);

        if (availableMedals.isEmpty()) {
            player.sendMessage(Component.text("§cVocê não tem nenhuma medalha disponível."));
            return;
        }

        player.sendMessage(Component.text("§e§lMedalhas Disponíveis:"));
        player.sendMessage(Component.text("§7Use /medalha <nome> para selecionar uma medalha"));
        player.sendMessage(Component.text("§7Use /medalha remover para remover sua medalha atual"));
        player.sendMessage(Component.text(""));

        // Mostrar medalha atual
        String currentMedal = medalManager.getCurrentPlayerMedal(player.getUniqueId());
        if (currentMedal != null) {
            MedalManager.MedalInfo currentMedalInfo = medalManager.getMedalInfo(currentMedal);
            if (currentMedalInfo != null) {
                Component medalComponent = Component.text(currentMedalInfo.getSymbol()).color(getTextColorFromCode(currentMedalInfo.getColor()));
                player.sendMessage(Component.text("§aMedalha atual: ").append(medalComponent));
            }
        } else {
            player.sendMessage(Component.text("§7Medalha atual: Nenhuma"));
        }
        player.sendMessage(Component.text(""));

        // Listar medalhas disponíveis
        StringJoiner medalsJoiner = new StringJoiner("§7, ");
        for (String medalId : availableMedals) {
            if (!medalId.equals("nenhuma")) {
                MedalManager.MedalInfo medalInfo = medalManager.getMedalInfo(medalId);
                String medalDisplay = medalInfo.getColor() + medalInfo.getSymbol() + " §f" + capitalizeFirst(medalId);
                medalsJoiner.add(medalDisplay);
            }
        }

        player.sendMessage(Component.text("§fMedalhas disponíveis: " + medalsJoiner.toString()));
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private TextColor getTextColorFromCode(String colorCode) {
        if (colorCode == null || colorCode.isEmpty()) {
            return TextColor.color(255, 255, 255);
        }

        char code = colorCode.charAt(colorCode.length() - 1);
        return switch (code) {
            case '0' -> TextColor.color(0, 0, 0);          // Preto
            case '1' -> TextColor.color(0, 0, 170);        // Azul escuro
            case '2' -> TextColor.color(0, 170, 0);        // Verde escuro
            case '3' -> TextColor.color(0, 170, 170);      // Ciano
            case '4' -> TextColor.color(170, 0, 0);        // Vermelho escuro
            case '5' -> TextColor.color(170, 0, 170);      // Roxo
            case '6' -> TextColor.color(255, 170, 0);      // Dourado
            case '7' -> TextColor.color(170, 170, 170);    // Cinza
            case '8' -> TextColor.color(85, 85, 85);       // Cinza escuro
            case '9' -> TextColor.color(85, 85, 255);      // Azul
            case 'a' -> TextColor.color(85, 255, 85);      // Verde
            case 'b' -> TextColor.color(85, 255, 255);     // Azul claro
            case 'c' -> TextColor.color(255, 85, 85);      // Vermelho
            case 'd' -> TextColor.color(255, 85, 255);     // Rosa
            case 'e' -> TextColor.color(255, 255, 85);     // Amarelo
            case 'f' -> TextColor.color(255, 255, 255);    // Branco
            default -> TextColor.color(255, 255, 255);
        };
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return List.of();
        }

        if (invocation.arguments().length == 1) {
            String arg = invocation.arguments()[0].toLowerCase();

            // Adicionar "remover" às sugestões
            List<String> suggestions = new java.util.ArrayList<>(medalManager.getPlayerAvailableMedals(player));
            suggestions.add("remover");

            return suggestions.stream()
                    .filter(medal -> medal.toLowerCase().startsWith(arg))
                    .toList();
        }

        return List.of();
    }
}

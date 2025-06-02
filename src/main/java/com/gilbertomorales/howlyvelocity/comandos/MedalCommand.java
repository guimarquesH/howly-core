package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

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
            source.sendMessage(Component.text("Apenas jogadores podem usar este comando.").color(TextColor.color(255, 85, 85)));
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
            player.sendMessage(Component.text("Medalha não encontrada. Use /medalha para ver as medalhas disponíveis.").color(TextColor.color(255, 85, 85)));
            return;
        }

        MedalManager.MedalInfo medalInfo = medalManager.getMedalInfo(medalId);

        // Verificar permissão
        if (!medalInfo.getPermission().isEmpty() && !player.hasPermission(medalInfo.getPermission())) {
            player.sendMessage(Component.text("Você não tem permissão para usar esta medalha.").color(TextColor.color(255, 85, 85)));
            return;
        }

        // Definir a medalha
        medalManager.setPlayerMedal(player.getUniqueId(), medalId);

        if (medalId.equals("nenhuma")) {
            player.sendMessage(Component.text("Medalha removida com sucesso!").color(TextColor.color(85, 255, 85)));
        } else {
            // Criar componente com símbolo e cor
            Component medalComponent = Component.text(medalInfo.getSymbol()).color(getTextColorFromCode(medalInfo.getColor()));
            Component message = Component.text("Medalha alterada para ")
                    .color(TextColor.color(85, 255, 85))
                    .append(medalComponent)
                    .append(Component.text(" com sucesso!").color(TextColor.color(85, 255, 85)));

            player.sendMessage(message);
        }
    }

    private void showAvailableMedals(Player player) {
        List<String> availableMedals = medalManager.getPlayerAvailableMedals(player);

        if (availableMedals.isEmpty()) {
            player.sendMessage(Component.text("Você não tem nenhuma medalha disponível.").color(TextColor.color(255, 85, 85)));
            return;
        }

        player.sendMessage(Component.text("Medalhas Disponíveis:").color(TextColor.color(255, 255, 85)).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        player.sendMessage(Component.text("Use /medalha <nome> para selecionar uma medalha").color(TextColor.color(170, 170, 170)));
        player.sendMessage(Component.text(""));

        // Mostrar medalhas em formato de lista organizada
        for (String medalId : availableMedals) {
            MedalManager.MedalInfo medalInfo = medalManager.getMedalInfo(medalId);

            if (medalId.equals("nenhuma")) {
                Component message = Component.text("• ").color(TextColor.color(170, 170, 170))
                        .append(Component.text("Nenhuma").color(TextColor.color(255, 255, 255)))
                        .append(Component.text(" - Remove a medalha atual").color(TextColor.color(170, 170, 170)));
                player.sendMessage(message);
            } else {
                String displayName = capitalizeFirst(medalId);

                Component medalSymbol = Component.text(medalInfo.getSymbol()).color(getTextColorFromCode(medalInfo.getColor()));
                Component message = Component.text("• ").color(TextColor.color(170, 170, 170))
                        .append(medalSymbol)
                        .append(Component.text(" " + displayName).color(TextColor.color(255, 255, 255)))
                        .append(Component.text(" - " + medalId).color(TextColor.color(170, 170, 170)));

                player.sendMessage(message);
            }
        }

        // Mostrar medalha atual
        String currentMedal = medalManager.getPlayerMedal(player);
        if (!currentMedal.isEmpty()) {
            player.sendMessage(Component.text(""));

            // Extrair símbolo e cor da medalha atual
            String selectedMedal = medalManager.getPlayerAvailableMedals(player).stream()
                    .filter(medal -> {
                        MedalManager.MedalInfo info = medalManager.getMedalInfo(medal);
                        return currentMedal.equals(info.getColoredSymbol());
                    })
                    .findFirst()
                    .orElse("");

            if (!selectedMedal.isEmpty()) {
                MedalManager.MedalInfo currentInfo = medalManager.getMedalInfo(selectedMedal);
                Component currentMedalComponent = Component.text(currentInfo.getSymbol()).color(getTextColorFromCode(currentInfo.getColor()));
                Component message = Component.text("Medalha atual: ")
                        .color(TextColor.color(85, 255, 85))
                        .append(currentMedalComponent);
                player.sendMessage(message);
            }
        }
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
            return medalManager.getPlayerAvailableMedals(player).stream()
                    .filter(medal -> medal.toLowerCase().startsWith(arg))
                    .toList();
        }

        return List.of();
    }
}

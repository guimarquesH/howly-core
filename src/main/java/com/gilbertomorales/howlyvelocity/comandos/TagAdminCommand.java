package com.gilbertomorales.howlyvelocity.comandos;

import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagAdminCommand implements SimpleCommand {

    private final ProxyServer server;
    private final TagManager tagManager;

    public TagAdminCommand(ProxyServer server, TagManager tagManager) {
        this.server = server;
        this.tagManager = tagManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("howly.admin.tag")) {
            source.sendMessage(Component.text("§cVocê não tem permissão para usar este comando."));
            return;
        }

        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add", "adicionar" -> handleAdd(source, args);
            case "remove", "remover" -> handleRemove(source, args);
            case "list", "listar" -> handleList(source);
            case "edit", "editar" -> handleEdit(source, args);
            case "reload", "recarregar" -> handleReload(source);
            default -> sendUsage(source);
        }
    }

    private void handleAdd(CommandSource source, String[] args) {
        if (args.length < 4) {
            source.sendMessage(Component.text("§cUso: /tagadmin add <id> <display> <cor> [permissão]"));
            source.sendMessage(Component.text("§cExemplo: /tagadmin add vip &6[VIP] &6 howly.tag.vip"));
            source.sendMessage(Component.text("§cCores: &0 &1 &2 &3 &4 &5 &6 &7 &8 &9 &a &b &c &d &e &f"));
            return;
        }

        String tagId = args[1].toLowerCase();
        String display = args[2].replace("&", "§"); // Converter & para §
        String nameColor = args[3].replace("&", "§"); // Converter & para §
        String permission = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "";

        // Verificar se a tag já existe
        if (tagManager.hasTag(tagId)) {
            source.sendMessage(Component.text("§cEsta tag já existe. Use /tagadmin edit para editá-la."));
            return;
        }

        // Validar cor (agora aceita tanto & quanto §)
        if (!isValidColor(nameColor)) {
            source.sendMessage(Component.text("§cCor inválida. Use códigos como &6, &a, &c, etc."));
            return;
        }

        // Adicionar a tag
        tagManager.addAvailableTag(tagId, display, permission, nameColor);

        source.sendMessage(Component.text("§aTag criada com sucesso!"));
        source.sendMessage(Component.text("§fID: §7" + tagId));
        source.sendMessage(Component.text("§fDisplay: " + display));
        source.sendMessage(Component.text("§fCor do nome: " + nameColor + "Exemplo"));
        source.sendMessage(Component.text("§fPermissão: §7" + (permission.isEmpty() ? "Nenhuma" : permission)));
    }

    private void handleRemove(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(Component.text("§cUso: /tagadmin remove <id>"));
            return;
        }

        String tagId = args[1].toLowerCase();

        if (!tagManager.hasTag(tagId)) {
            source.sendMessage(Component.text("§cTag não encontrada."));
            return;
        }

        if (tagId.equals("nenhuma")) {
            source.sendMessage(Component.text("§cNão é possível remover a tag padrão."));
            return;
        }

        tagManager.removeAvailableTag(tagId);
        source.sendMessage(Component.text("§aTag removida com sucesso!"));
    }

    private void handleList(CommandSource source) {
        Map<String, TagManager.TagInfo> availableTags = tagManager.getAvailableTags();

        if (availableTags.isEmpty()) {
            source.sendMessage(Component.text("§cNenhuma tag encontrada."));
            return;
        }

        source.sendMessage(Component.text("§e§lTags Disponíveis:"));
        source.sendMessage(Component.text(" "));

        for (Map.Entry<String, TagManager.TagInfo> entry : availableTags.entrySet()) {
            String tagId = entry.getKey();
            TagManager.TagInfo tagInfo = entry.getValue();

            source.sendMessage(Component.text("§fID: §7" + tagId));
            source.sendMessage(Component.text("§fDisplay: " + tagInfo.getDisplay()));
            source.sendMessage(Component.text("§fCor: " + tagInfo.getNameColor() + "Exemplo"));
            source.sendMessage(Component.text("§fPermissão: §7" + (tagInfo.getPermission().isEmpty() ? "Nenhuma" : tagInfo.getPermission())));
            source.sendMessage(Component.text(" "));
        }
    }

    private void handleEdit(CommandSource source, String[] args) {
        if (args.length < 5) {
            source.sendMessage(Component.text("§cUso: /tagadmin edit <id> <display> <cor> [permissão]"));
            source.sendMessage(Component.text("§cExemplo: /tagadmin edit vip &6[VIP+] &6 howly.tag.vipplus"));
            return;
        }

        String tagId = args[1].toLowerCase();
        String display = args[2].replace("&", "§"); // Converter & para §
        String nameColor = args[3].replace("&", "§"); // Converter & para §
        String permission = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "";

        if (!tagManager.hasTag(tagId)) {
            source.sendMessage(Component.text("§cTag não encontrada."));
            return;
        }

        // Validar cor (agora aceita tanto & quanto §)
        if (!isValidColor(nameColor)) {
            source.sendMessage(Component.text("§cCor inválida. Use códigos como &6, &a, &c, etc."));
            return;
        }

        // Editar a tag (remove e adiciona novamente)
        tagManager.removeAvailableTag(tagId);
        tagManager.addAvailableTag(tagId, display, permission, nameColor);

        source.sendMessage(Component.text("§aTag editada com sucesso!"));
        source.sendMessage(Component.text("§fID: §7" + tagId));
        source.sendMessage(Component.text("§fDisplay: " + display));
        source.sendMessage(Component.text("§fCor do nome: " + nameColor + "Exemplo"));
        source.sendMessage(Component.text("§fPermissão: §7" + (permission.isEmpty() ? "Nenhuma" : permission)));
    }

    private void handleReload(CommandSource source) {
        tagManager.loadTags();
        source.sendMessage(Component.text("§aTags recarregadas com sucesso!"));
    }

    private boolean isValidColor(String color) {
        if (color.length() != 2) {
            return false;
        }

        char prefix = color.charAt(0);
        if (prefix != '§' && prefix != '&') {
            return false;
        }

        char colorCode = color.charAt(1);
        return "0123456789abcdef".indexOf(Character.toLowerCase(colorCode)) != -1;
    }

    private void sendUsage(CommandSource source) {
        source.sendMessage(Component.text("§c§lUso do comando /tagadmin:"));
        source.sendMessage(Component.text("§e/tagadmin add <id> <display> <cor> [permissão] §7- Adiciona uma nova tag"));
        source.sendMessage(Component.text("§e/tagadmin remove <id> §7- Remove uma tag"));
        source.sendMessage(Component.text("§e/tagadmin list §7- Lista todas as tags"));
        source.sendMessage(Component.text("§e/tagadmin edit <id> <display> <cor> [permissão] §7- Edita uma tag"));
        source.sendMessage(Component.text("§e/tagadmin reload §7- Recarrega as tags"));
        source.sendMessage(Component.text(" "));
        source.sendMessage(Component.text("§7Exemplos:"));
        source.sendMessage(Component.text("§f/tagadmin add vip &6[VIP] &6 howly.tag.vip"));
        source.sendMessage(Component.text("§f/tagadmin add admin &c[ADMIN] &c"));
        source.sendMessage(Component.text("§f/tagadmin add helper &a[HELPER] &a howly.tag.helper"));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return List.of("add", "remove", "list", "edit", "reload").stream()
                    .filter(subCmd -> subCmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("edit"))) {
            return tagManager.getAvailableTags().keySet().stream()
                    .filter(tag -> tag.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("edit"))) {
            return List.of("&0", "&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f").stream()
                    .filter(color -> color.startsWith(args[3]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}

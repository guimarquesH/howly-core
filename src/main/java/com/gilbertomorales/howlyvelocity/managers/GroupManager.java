package com.gilbertomorales.howlyvelocity.managers;

import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.NodeType;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GroupManager {

    private LuckPerms luckPerms;
    private final Map<String, GroupInfo> groupInfoMap = new HashMap<>();

    public GroupManager() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            initializeGroupInfo();
        } catch (IllegalStateException e) {
            // LuckPerms não está disponível
            this.luckPerms = null;
        }
    }

    private void initializeGroupInfo() {
        // Grupos de staff
        groupInfoMap.put("master", new GroupInfo("Master", "§6", "howly.master", 1000));
        groupInfoMap.put("gerente", new GroupInfo("Gerente", "§4", "howly.gerente", 900));
        groupInfoMap.put("coordenador", new GroupInfo("Coordenador", "§c", "howly.coordenador", 800));
        groupInfoMap.put("moderador", new GroupInfo("Moderador", "§2", "howly.moderador", 700));
        groupInfoMap.put("ajudante", new GroupInfo("Ajudante", "§e", "howly.ajudante", 600));
        groupInfoMap.put("construtor", new GroupInfo("Construtor", "§a", "howly.construtor", 500));

        // Grupos especiais
        groupInfoMap.put("youtuber", new GroupInfo("YouTuber", "§c", "howly.youtuber", 400));
        groupInfoMap.put("streamer", new GroupInfo("Streamer", "§9", "howly.streamer", 350));
        groupInfoMap.put("beta", new GroupInfo("Beta", "§3", "howly.beta", 300));

        // Grupos VIP
        groupInfoMap.put("supremo", new GroupInfo("Supremo", "§4", "howly.supremo", 250, true));
        groupInfoMap.put("mitico", new GroupInfo("Mítico", "§5", "howly.mitico", 200, true));
        groupInfoMap.put("lendario", new GroupInfo("Lendário", "§6", "howly.lendario", 150, true));
        groupInfoMap.put("epico", new GroupInfo("Épico", "§b", "howly.epico", 100, true));

        // Grupo padrão
        groupInfoMap.put("default", new GroupInfo("Membro", "§7", "", 0));
    }

    /**
     * Obtém o grupo principal de um jogador
     */
    public String getPlayerPrimaryGroup(Player player) {
        if (luckPerms == null) {
            return "default";
        }

        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                return user.getPrimaryGroup();
            }
        } catch (Exception e) {
            // Ignorar erros
        }

        return "default";
    }

    /**
     * Obtém informações do grupo de um jogador
     */
    public GroupInfo getPlayerGroupInfo(Player player) {
        String primaryGroup = getPlayerPrimaryGroup(player);
        return groupInfoMap.getOrDefault(primaryGroup.toLowerCase(), groupInfoMap.get("default"));
    }

    /**
     * Obtém o prefixo do grupo do jogador
     */
    public String getPlayerGroupPrefix(Player player) {
        GroupInfo groupInfo = getPlayerGroupInfo(player);
        if (groupInfo.getDisplayName().equals("Membro")) {
            return ""; // Não mostrar prefixo para membros
        }
        return groupInfo.getColor() + "[" + groupInfo.getDisplayName() + "]";
    }

    /**
     * Obtém a cor do nome do jogador baseada no grupo
     */
    public String getPlayerGroupNameColor(Player player) {
        GroupInfo groupInfo = getPlayerGroupInfo(player);
        return groupInfo.getColor();
    }

    /**
     * Obtém o nome formatado do jogador com grupo (sem tag)
     */
    public String getFormattedPlayerName(Player player) {
        String groupPrefix = getPlayerGroupPrefix(player);
        String nameColor = getPlayerGroupNameColor(player);

        if (groupPrefix.isEmpty()) {
            return nameColor + player.getUsername();
        } else {
            return groupPrefix + " " + nameColor + player.getUsername();
        }
    }

    /**
     * Verifica se um grupo existe
     */
    public boolean groupExists(String groupName) {
        if (luckPerms == null) {
            return groupInfoMap.containsKey(groupName.toLowerCase());
        }

        Group group = luckPerms.getGroupManager().getGroup(groupName.toLowerCase());
        return group != null;
    }

    /**
     * Adiciona um jogador a um grupo
     */
    public CompletableFuture<Boolean> addPlayerToGroup(Player player, String groupName) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                InheritanceNode node = InheritanceNode.builder(groupName.toLowerCase()).build();

                // Verificar se já tem o grupo
                boolean hasGroup = user.getNodes(NodeType.INHERITANCE).stream()
                        .anyMatch(n -> n.getGroupName().equalsIgnoreCase(groupName));

                if (!hasGroup) {
                    user.data().add(node);
                    luckPerms.getUserManager().saveUser(user);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Remove um jogador de um grupo
     */
    public CompletableFuture<Boolean> removePlayerFromGroup(Player player, String groupName) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                InheritanceNode node = InheritanceNode.builder(groupName.toLowerCase()).build();

                // Verificar se tem o grupo
                boolean hasGroup = user.getNodes(NodeType.INHERITANCE).stream()
                        .anyMatch(n -> n.getGroupName().equalsIgnoreCase(groupName));

                if (hasGroup) {
                    user.data().remove(node);
                    luckPerms.getUserManager().saveUser(user);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Define o grupo principal de um jogador (remove outros grupos)
     */
    public CompletableFuture<Boolean> setPlayerGroup(Player player, String groupName) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
                InheritanceNode node = InheritanceNode.builder(groupName.toLowerCase()).build();

                // Verificar se já tem o grupo
                boolean hasGroup = user.getNodes(NodeType.INHERITANCE).stream()
                        .anyMatch(n -> n.getGroupName().equalsIgnoreCase(groupName));

                if (!hasGroup) {
                    // Remover todos os grupos de herança
                    user.data().clear(n -> n instanceof InheritanceNode);
                    // Adicionar o novo grupo
                    user.data().add(node);
                    luckPerms.getUserManager().saveUser(user);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    /**
     * Verifica se um grupo é VIP
     */
    public boolean isVipGroup(String groupName) {
        GroupInfo groupInfo = groupInfoMap.get(groupName.toLowerCase());
        return groupInfo != null && groupInfo.isVip();
    }

    /**
     * Obtém todos os grupos disponíveis
     */
    public Map<String, GroupInfo> getAvailableGroups() {
        return Collections.unmodifiableMap(groupInfoMap);
    }

    /**
     * Obtém a prioridade do grupo do jogador
     */
    public int getPlayerGroupPriority(Player player) {
        GroupInfo groupInfo = getPlayerGroupInfo(player);
        return groupInfo.getPriority();
    }

    /**
     * Verifica se o LuckPerms está disponível
     */
    public boolean isLuckPermsAvailable() {
        return luckPerms != null;
    }

    public static class GroupInfo {
        private final String displayName;
        private final String color;
        private final String permission;
        private final int priority;
        private final boolean vip;

        public GroupInfo(String displayName, String color, String permission, int priority) {
            this(displayName, color, permission, priority, false);
        }

        public GroupInfo(String displayName, String color, String permission, int priority, boolean vip) {
            this.displayName = displayName;
            this.color = color;
            this.permission = permission;
            this.priority = priority;
            this.vip = vip;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getColor() {
            return color;
        }

        public String getPermission() {
            return permission;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isVip() {
            return vip;
        }

        public String getFormattedPrefix() {
            return color + "[" + displayName + "]";
        }
    }
}

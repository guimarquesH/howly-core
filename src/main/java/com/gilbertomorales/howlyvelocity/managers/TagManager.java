package com.gilbertomorales.howlyvelocity.managers;

import com.velocitypowered.api.proxy.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TagManager {

    private final DatabaseManager databaseManager;

    // Cache em memória para performance
    private final Map<String, TagInfo> availableTags = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTags = new ConcurrentHashMap<>();

    public TagManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void loadTags() {
        CompletableFuture.runAsync(() -> {
            try {
                loadAvailableTagsFromDB();
                loadPlayerTagsFromDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void loadAvailableTagsFromDB() throws SQLException {
        String sql = "SELECT tag_id, display_text, permission, name_color FROM available_tags";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            availableTags.clear();
            while (rs.next()) {
                String tagId = rs.getString("tag_id");
                String displayText = rs.getString("display_text");
                String permission = rs.getString("permission");
                String nameColor = rs.getString("name_color");

                availableTags.put(tagId, new TagInfo(displayText, permission, nameColor));
            }
        }
    }

    private void loadPlayerTagsFromDB() throws SQLException {
        String sql = "SELECT player_uuid, tag_id FROM player_tags";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            playerTags.clear();
            while (rs.next()) {
                try {
                    UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
                    String tagId = rs.getString("tag_id");
                    playerTags.put(playerUuid, tagId);
                } catch (IllegalArgumentException e) {
                    // Ignorar UUIDs inválidos
                }
            }
        }
    }

    public void saveTags() {
        CompletableFuture.runAsync(() -> {
            try {
                saveAvailableTagsToDB();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveAvailableTagsToDB() throws SQLException {
        String sql;

        if (databaseManager.isMySQL()) {
            sql = "INSERT INTO available_tags (tag_id, display_text, permission, name_color, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "display_text = VALUES(display_text), " +
                    "permission = VALUES(permission), " +
                    "name_color = VALUES(name_color), " +
                    "updated_at = VALUES(updated_at)";
        } else if (databaseManager.isH2()) {
            sql = "MERGE INTO available_tags (tag_id, display_text, permission, name_color, created_at, updated_at) " +
                    "KEY (tag_id) VALUES (?, ?, ?, ?, ?, ?)";
        } else {
            // SQLite
            sql = "INSERT OR REPLACE INTO available_tags (tag_id, display_text, permission, name_color, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, TagInfo> entry : availableTags.entrySet()) {
                stmt.setString(1, entry.getKey());
                stmt.setString(2, entry.getValue().getDisplay());
                stmt.setString(3, entry.getValue().getPermission());
                stmt.setString(4, entry.getValue().getNameColor());
                stmt.setLong(5, currentTime);
                stmt.setLong(6, currentTime);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Retorna apenas a tag do jogador (sem espaços)
     */
    public String getPlayerTag(Player player) {
        String selectedTag = playerTags.get(player.getUniqueId());
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);

            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                return tagInfo.display;
            }
        }
        // Retornar string vazia se não tiver tag
        return "";
    }

    /**
     * Retorna apenas a tag do jogador offline (sem espaços)
     */
    public String getPlayerTagByUUID(UUID playerUuid) {
        String selectedTag = playerTags.get(playerUuid);
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);

            // Para jogador offline, não podemos verificar permissão
            // Então apenas retornamos a tag se ela existir
            return tagInfo.display;
        }
        // Retornar string vazia se não tiver tag
        return "";
    }

    /**
     * Retorna a tag formatada com espaço APENAS se o jogador offline tiver tag
     */
    public String getFormattedPlayerTagByUUID(UUID playerUuid) {
        String tag = getPlayerTagByUUID(playerUuid);
        return tag.isEmpty() ? "" : tag + " ";
    }

    /**
     * Retorna o nome completo do jogador offline (tag + nome) formatado corretamente
     */
    public String getFormattedPlayerNameByUUID(UUID playerUuid, String playerName) {
        String tag = getPlayerTagByUUID(playerUuid);
        String nameColor = getPlayerNameColorByUUID(playerUuid);

        if (tag.isEmpty()) {
            // Sem tag: apenas cor + nome (sem espaços extras)
            return nameColor + playerName;
        } else {
            // Com tag: tag + espaço + cor + nome
            return tag + " " + nameColor + playerName;
        }
    }

    /**
     * Obtém a cor do nome para jogador offline
     */
    public String getPlayerNameColorByUUID(UUID playerUuid) {
        // Verificar se o jogador tem uma tag selecionada
        String selectedTag = playerTags.get(playerUuid);
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);
            return tagInfo.nameColor;
        }

        // Cor padrão se não tiver tag
        return "§7";
    }

    /**
     * Retorna a tag formatada com espaço APENAS se o jogador tiver tag
     */
    public String getFormattedPlayerTag(Player player) {
        String tag = getPlayerTag(player);
        return tag.isEmpty() ? "" : tag + " ";
    }

    /**
     * Retorna o nome completo do jogador (tag + nome) formatado corretamente
     */
    public String getFormattedPlayerName(Player player) {
        String tag = getPlayerTag(player);
        String nameColor = getPlayerNameColor(player);

        if (tag.isEmpty()) {
            // Sem tag: apenas cor + nome (sem espaços extras)
            return nameColor + player.getUsername();
        } else {
            // Com tag: tag + espaço + cor + nome
            return tag + " " + nameColor + player.getUsername();
        }
    }

    public String getPlayerNameColor(Player player) {
        // Verificar se o jogador tem uma tag selecionada
        String selectedTag = playerTags.get(player.getUniqueId());
        if (selectedTag != null && availableTags.containsKey(selectedTag)) {
            TagInfo tagInfo = availableTags.get(selectedTag);

            // Verificar se o jogador tem permissão para usar esta tag
            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                return tagInfo.nameColor;
            }
        }

        // Cor padrão se não tiver tag
        return "§7";
    }

    public void setPlayerTag(UUID uuid, String tagId) {
        playerTags.put(uuid, tagId);
        setPlayerTagInDB(uuid, tagId);
    }

    private void setPlayerTagInDB(UUID uuid, String tagId) {
        CompletableFuture.runAsync(() -> {
            String sql;

            if (databaseManager.isMySQL()) {
                sql = "INSERT INTO player_tags (player_uuid, tag_id, updated_at) " +
                        "VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "tag_id = VALUES(tag_id), " +
                        "updated_at = VALUES(updated_at)";
            } else if (databaseManager.isH2()) {
                sql = "MERGE INTO player_tags (player_uuid, tag_id, updated_at) " +
                        "KEY (player_uuid) VALUES (?, ?, ?)";
            } else {
                // SQLite
                sql = "INSERT OR REPLACE INTO player_tags (player_uuid, tag_id, updated_at) " +
                        "VALUES (?, ?, ?)";
            }

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, tagId);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void removePlayerTag(UUID uuid) {
        playerTags.remove(uuid);
        removePlayerTagFromDB(uuid);
    }

    private void removePlayerTagFromDB(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_tags WHERE player_uuid = ?";

            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Obtém a tag atual do jogador (ID da tag, não o display)
     */
    public String getCurrentPlayerTag(UUID uuid) {
        return playerTags.get(uuid);
    }

    public Map<String, TagInfo> getAvailableTags() {
        return Collections.unmodifiableMap(availableTags);
    }

    public List<String> getPlayerAvailableTags(Player player) {
        List<String> availableTags = new ArrayList<>();

        for (Map.Entry<String, TagInfo> entry : this.availableTags.entrySet()) {
            String tagId = entry.getKey();

            // Ignora a tag "nenhuma"
            if (tagId.equalsIgnoreCase("Nenhuma") || tagId.equalsIgnoreCase("nenhuma") || tagId.isEmpty()) {
                continue;
            }

            TagInfo tagInfo = entry.getValue();

            if (tagInfo.permission.isEmpty() || player.hasPermission(tagInfo.permission)) {
                availableTags.add(tagId);
            }
        }

        return availableTags;
    }


    public boolean hasTag(String tagId) {
        return availableTags.containsKey(tagId);
    }

    public TagInfo getTagInfo(String tagId) {
        return availableTags.get(tagId);
    }

    /**
     * Adiciona uma nova tag disponível (para uso administrativo)
     */
    public void addAvailableTag(String tagId, String display, String permission, String nameColor) {
        availableTags.put(tagId, new TagInfo(display, permission, nameColor));
        saveTags();
    }

    /**
     * Remove uma tag disponível (para uso administrativo)
     */
    public void removeAvailableTag(String tagId) {
        availableTags.remove(tagId);
        saveTags();
    }

    public CompletableFuture<Void> migrateFromFilesToDatabase() {
        return CompletableFuture.runAsync(() -> {
            // Migração já foi feita, dados estão no banco
        });
    }

    public static class TagInfo {
        private final String display;
        private final String permission;
        private final String nameColor;

        public TagInfo(String display, String permission, String nameColor) {
            this.display = display;
            this.permission = permission;
            this.nameColor = nameColor;
        }

        public String getDisplay() {
            return display;
        }

        public String getPermission() {
            return permission;
        }

        public String getNameColor() {
            return nameColor;
        }
    }
}

package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

public class DatabaseManager {

    private final ConfigManager configManager;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void initialize() {
        try {
            setupDataSource();
            createTables();
            logger.info("Banco de dados inicializado com sucesso!");
        } catch (Exception e) {
            logger.error("Erro ao inicializar banco de dados: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();

        String databaseType = configManager.getDatabaseType().toLowerCase();

        switch (databaseType) {
            case "mysql" -> {
                config.setJdbcUrl("jdbc:mysql://" + configManager.getDatabaseHost() + ":" +
                        configManager.getDatabasePort() + "/" + configManager.getDatabaseName() +
                        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
                config.setUsername(configManager.getDatabaseUsername());
                config.setPassword(configManager.getDatabasePassword());
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
            case "h2" -> {
                config.setJdbcUrl("jdbc:h2:./plugins/HowlyVelocity/database;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE");
                config.setUsername("sa");
                config.setPassword("");
                config.setDriverClassName("org.h2.Driver");
            }
            case "sqlite" -> {
                config.setJdbcUrl("jdbc:sqlite:./plugins/HowlyVelocity/database.db");
                config.setDriverClassName("org.sqlite.JDBC");
            }
            default -> throw new IllegalArgumentException("Tipo de banco de dados não suportado: " + databaseType);
        }

        // Configurações de pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Configurações específicas para UTF-8
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");

        dataSource = new HikariDataSource(config);
        logger.info("Conexão com banco de dados " + databaseType.toUpperCase() + " estabelecida!");
    }

    private void createTables() throws SQLException {
        executeDefaultSchema();
    }

    private void executeDefaultSchema() throws SQLException {
        String databaseType = configManager.getDatabaseType().toLowerCase();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            logger.info("Criando tabelas para banco " + databaseType.toUpperCase() + "...");

            // Verificar se a tabela players já existe
            if (tableExists(conn, "players")) {
                logger.info("Tabela 'players' já existe, verificando estrutura...");
                if (!columnExists(conn, "players", "id")) {
                    logger.info("Coluna 'id' não existe, recriando tabela 'players'...");
                    stmt.execute("DROP TABLE IF EXISTS players");
                }
            }

            // Schema para jogadores (com ID único)
            if (databaseType.equals("mysql")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        uuid VARCHAR(36) UNIQUE NOT NULL,
                        name VARCHAR(16) NOT NULL,
                        first_join BIGINT NOT NULL,
                        last_join BIGINT NOT NULL,
                        
                        INDEX idx_uuid (uuid),
                        INDEX idx_name (name),
                        INDEX idx_id (id)
                    )
                """);
            } else if (databaseType.equals("h2")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        uuid VARCHAR(36) UNIQUE NOT NULL,
                        name VARCHAR(16) NOT NULL,
                        first_join BIGINT NOT NULL,
                        last_join BIGINT NOT NULL
                    )
                """);

                // Criar índices separadamente para H2
                createIndexIfNotExists(stmt, "idx_players_uuid", "players", "uuid");
                createIndexIfNotExists(stmt, "idx_players_name", "players", "name");
                createIndexIfNotExists(stmt, "idx_players_id", "players", "id");

            } else { // SQLite
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        first_join INTEGER NOT NULL,
                        last_join INTEGER NOT NULL
                    )
                """);
            }

            logger.info("Tabela 'players' criada/verificada com sucesso!");

            // Schema para punições
            if (databaseType.equals("mysql")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        type ENUM('BAN', 'KICK', 'MUTE') NOT NULL,
                        reason TEXT NOT NULL,
                        punisher VARCHAR(50) NOT NULL,
                        created_at BIGINT NOT NULL,
                        expires_at BIGINT NULL,
                        active BOOLEAN DEFAULT TRUE,
                        
                        INDEX idx_player_uuid (player_uuid),
                        INDEX idx_type (type),
                        INDEX idx_active (active),
                        INDEX idx_expires_at (expires_at),
                        INDEX idx_created_at (created_at)
                    )
                """);
            } else if (databaseType.equals("h2")) {
                // H2 - criar tabela sem índices inline
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        type VARCHAR(10) NOT NULL CHECK (type IN ('BAN', 'KICK', 'MUTE')),
                        reason TEXT NOT NULL,
                        punisher VARCHAR(50) NOT NULL,
                        created_at BIGINT NOT NULL,
                        expires_at BIGINT NULL,
                        active BOOLEAN DEFAULT TRUE
                    )
                """);

                // Criar índices separadamente para H2
                createIndexIfNotExists(stmt, "idx_punishments_player_uuid", "punishments", "player_uuid");
                createIndexIfNotExists(stmt, "idx_punishments_type", "punishments", "type");
                createIndexIfNotExists(stmt, "idx_punishments_active", "punishments", "active");
                createIndexIfNotExists(stmt, "idx_punishments_expires_at", "punishments", "expires_at");
                createIndexIfNotExists(stmt, "idx_punishments_created_at", "punishments", "created_at");

            } else { // SQLite
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        type TEXT NOT NULL CHECK (type IN ('BAN', 'KICK', 'MUTE')),
                        reason TEXT NOT NULL,
                        punisher TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        expires_at INTEGER,
                        active INTEGER DEFAULT 1
                    )
                """);
            }

            // Schema para tags
            if (databaseType.equals("mysql")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_tags (
                        tag_id VARCHAR(50) PRIMARY KEY,
                        display_text VARCHAR(100) NOT NULL,
                        permission VARCHAR(100) DEFAULT '',
                        name_color VARCHAR(10) NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_tags (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        tag_id VARCHAR(50) NOT NULL,
                        updated_at BIGINT NOT NULL,
                        
                        INDEX idx_tag_id (tag_id),
                        INDEX idx_updated_at (updated_at)
                    )
                """);
            } else if (databaseType.equals("h2")) {
                // H2 - criar tabelas sem índices inline
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_tags (
                        tag_id VARCHAR(50) PRIMARY KEY,
                        display_text VARCHAR(100) NOT NULL,
                        permission VARCHAR(100) DEFAULT '',
                        name_color VARCHAR(10) NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_tags (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        tag_id VARCHAR(50) NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                // Criar índices separadamente para H2
                createIndexIfNotExists(stmt, "idx_player_tags_tag_id", "player_tags", "tag_id");
                createIndexIfNotExists(stmt, "idx_player_tags_updated_at", "player_tags", "updated_at");

            } else { // SQLite
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_tags (
                        tag_id TEXT PRIMARY KEY,
                        display_text TEXT NOT NULL,
                        permission TEXT DEFAULT '',
                        name_color TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_tags (
                        player_uuid TEXT PRIMARY KEY,
                        tag_id TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (tag_id) REFERENCES available_tags(tag_id)
                    )
                """);
            }

            // Schema para medalhas
            if (databaseType.equals("mysql")) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_medals (
                        medal_id VARCHAR(50) PRIMARY KEY,
                        symbol VARCHAR(10) NOT NULL,
                        permission VARCHAR(100) DEFAULT '',
                        color VARCHAR(10) NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_medals (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        medal_id VARCHAR(50) NOT NULL,
                        updated_at BIGINT NOT NULL,
                        
                        INDEX idx_medal_id (medal_id),
                        INDEX idx_updated_at (updated_at)
                    )
                """);
            } else if (databaseType.equals("h2")) {
                // H2 - criar tabelas sem índices inline
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_medals (
                        medal_id VARCHAR(50) PRIMARY KEY,
                        symbol VARCHAR(10) NOT NULL,
                        permission VARCHAR(100) DEFAULT '',
                        color VARCHAR(10) NOT NULL,
                        created_at BIGINT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_medals (
                        player_uuid VARCHAR(36) PRIMARY KEY,
                        medal_id VARCHAR(50) NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                """);

                // Criar índices separadamente para H2
                createIndexIfNotExists(stmt, "idx_player_medals_medal_id", "player_medals", "medal_id");
                createIndexIfNotExists(stmt, "idx_player_medals_updated_at", "player_medals", "updated_at");

            } else { // SQLite
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS available_medals (
                        medal_id TEXT PRIMARY KEY,
                        symbol TEXT NOT NULL,
                        permission TEXT DEFAULT '',
                        color TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_medals (
                        player_uuid TEXT PRIMARY KEY,
                        medal_id TEXT NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY (medal_id) REFERENCES available_medals(medal_id)
                    )
                """);
            }

            // Inserir dados padrão
            insertDefaultData(stmt, databaseType);

            logger.info("Todas as tabelas criadas com sucesso!");
        }
    }

    private boolean tableExists(Connection conn, String tableName) {
        try {
            ResultSet rs = conn.getMetaData().getTables(null, null, tableName.toUpperCase(), null);
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            logger.warn("Erro ao verificar se tabela existe: " + e.getMessage());
            return false;
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) {
        try {
            ResultSet rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase());
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (SQLException e) {
            logger.warn("Erro ao verificar se coluna existe: " + e.getMessage());
            return false;
        }
    }

    private void createIndexIfNotExists(Statement stmt, String indexName, String tableName, String columnName) {
        try {
            stmt.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (" + columnName + ")");
        } catch (SQLException e) {
            // Ignorar se o índice já existir
            if (!e.getMessage().contains("already exists")) {
                logger.warn("Erro ao criar índice " + indexName + ": " + e.getMessage());
            }
        }
    }

    private void insertDefaultData(Statement stmt, String databaseType) throws SQLException {
        long currentTime = System.currentTimeMillis();

        String[][] defaultTags = {
                {"genio", "§6[Gênio]", "howly.tag.genio", "§6"},
                {"heroi", "§c[Herói]", "howly.tag.heroi", "§c"},
                {"ninja", "§7[Ninja]", "howly.tag.ninja", "§7"},
                {"imortal", "§5[Imortal]", "howly.tag.imortal", "§5"},
                {"medroso", "§e[Medroso]", "howly.tag.medroso", "§e"},
                {"mago", "§9[Mago]", "howly.tag.mago", "§9"},
                {"guerreiro", "§6[Guerreiro]", "howly.tag.guerreiro", "§6"},
                {"assassino", "§8[Assassino]", "howly.tag.assassino", "§8"},
                {"arqueiro", "§a[Arqueiro]", "howly.tag.arqueiro", "§a"},
                {"mercenario", "§3[Mercenário]", "howly.tag.mercenario", "§3"},
                {"campeao", "§b[Campeão]", "howly.tag.campeao", "§b"},
                {"sombra", "§0[Sombra]", "howly.tag.sombra", "§0"},
                {"anjo", "§f[Anjo]", "howly.tag.anjo", "§f"},
                {"demonio", "§4[Demônio]", "howly.tag.demonio", "§4"},
        };

        String[][] defaultMedals = {
                {"aguakanji", "水", "howly.medalha.aguakanji", "§b"},
                {"ancora", "⚓", "howly.medalha.ancora", "§9"},
                {"bandeira", "⚑", "howly.medalha.bandeira", "§f"},
                {"carinha", "ツ", "howly.medalha.carinha", "§9"},
                {"carta", "✉", "howly.medalha.carta", "§f"},
                {"caveira", "☠", "howly.medalha.caveira", "§8"},
                {"cafe", "☕", "howly.medalha.cafe", "§6"},
                {"ceukanji", "空", "howly.medalha.ceukanji", "§9"},
                {"coracao", "♥", "howly.medalha.coracao", "§c"},
                {"correto", "✔", "howly.medalha.correto", "§a"},
                {"coroa", "♔", "howly.medalha.coroa", "§6"},
                {"diamante", "♦", "howly.medalha.diamante", "§b"},
                {"engrenagem", "⚙", "howly.medalha.engrenagem", "§7"},
                {"espada", "⚔", "howly.medalha.espada", "§7"},
                {"errado", "✖", "howly.medalha.errado", "§c"},
                {"estrela", "★", "howly.medalha.estrela", "§e"},
                {"feliz", "㋡", "howly.medalha.feliz", "§e"},
                {"flor", "✿", "howly.medalha.flor", "§a"},
                {"fogo", "♨", "howly.medalha.fogo", "§c"},
                {"fogokanji", "火", "howly.medalha.fogokanji", "§c"},
                {"gelo", "❄", "howly.medalha.gelo", "§f"},
                {"lua", "☽", "howly.medalha.lua", "§9"},
                {"luakanji", "月", "howly.medalha.luakanji", "§9"},
                {"martelo", "⚒", "howly.medalha.martelo", "§7"},
                {"musica", "♫", "howly.medalha.musica", "§d"},
                {"raio", "⚡", "howly.medalha.raio", "§e"},
                {"sol", "☀", "howly.medalha.sol", "§e"},
                {"solkanji", "日", "howly.medalha.solkanji", "§e"},
                {"toxico", "☣", "howly.medalha.toxico", "§2"},
                {"trevo", "♣", "howly.medalha.trevo", "§a"},
                {"yinyang", "☯", "howly.medalha.yin_yang", "§f"}
        };

        if (databaseType.equals("h2")) {
            // Tag e medalha padrão
            stmt.execute(String.format("""
            MERGE INTO available_tags (tag_id, display_text, permission, name_color, created_at, updated_at)
            VALUES ('Nenhuma', '', '', '§7', %d, %d)
        """, currentTime, currentTime));

            stmt.execute(String.format("""
            MERGE INTO available_medals (medal_id, symbol, permission, color, created_at, updated_at)
            VALUES ('nenhuma', '', '', '', %d, %d)
        """, currentTime, currentTime));

            for (String[] tag : defaultTags) {
                stmt.execute(String.format("""
                MERGE INTO available_tags (tag_id, display_text, permission, name_color, created_at, updated_at)
                VALUES ('%s', '%s', '%s', '%s', %d, %d)
            """, tag[0], tag[1], tag[2], tag[3], currentTime, currentTime));
            }

            for (String[] medal : defaultMedals) {
                stmt.execute(String.format("""
                MERGE INTO available_medals (medal_id, symbol, permission, color, created_at, updated_at)
                VALUES ('%s', '%s', '%s', '%s', %d, %d)
            """, medal[0], medal[1], medal[2], medal[3], currentTime, currentTime));
            }

        } else {
            // MySQL e SQLite
            String insertOrIgnore = databaseType.equals("mysql") ? "INSERT IGNORE INTO" : "INSERT OR IGNORE INTO";

            stmt.execute(String.format("""
            %s available_tags (tag_id, display_text, permission, name_color, created_at, updated_at)
            VALUES ('Nenhuma', '', '', '§7', %d, %d)
        """, insertOrIgnore, currentTime, currentTime));

            stmt.execute(String.format("""
            %s available_medals (medal_id, symbol, permission, color, created_at, updated_at)
            VALUES ('nenhuma', '', '', '', %d, %d)
        """, insertOrIgnore, currentTime, currentTime));

            for (String[] tag : defaultTags) {
                stmt.execute(String.format("""
                %s available_tags (tag_id, display_text, permission, name_color, created_at, updated_at)
                VALUES ('%s', '%s', '%s', '%s', %d, %d)
            """, insertOrIgnore, tag[0], tag[1], tag[2], tag[3], currentTime, currentTime));
            }

            for (String[] medal : defaultMedals) {
                stmt.execute(String.format("""
                %s available_medals (medal_id, symbol, permission, color, created_at, updated_at)
                VALUES ('%s', '%s', '%s', '%s', %d, %d)
            """, insertOrIgnore, medal[0], medal[1], medal[2], medal[3], currentTime, currentTime));
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Métodos de conveniência
    public boolean isMySQL() {
        return configManager.isMySQL();
    }

    public boolean isH2() {
        return configManager.isH2();
    }

    public boolean isSQLite() {
        return configManager.isSQLite();
    }
}

package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

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

        if (configManager.isMySQL()) {
            config.setJdbcUrl("jdbc:mysql://" + configManager.getDatabaseHost() + ":" +
                    configManager.getDatabasePort() + "/" + configManager.getDatabaseName() +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername(configManager.getDatabaseUsername());
            config.setPassword(configManager.getDatabasePassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // H2 Database (local)
            config.setJdbcUrl("jdbc:h2:./plugins/HowlyVelocity/database;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
            config.setUsername("sa");
            config.setPassword("");
            config.setDriverClassName("org.h2.Driver");
        }

        // Configurações de pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        logger.info("Conexão com banco de dados estabelecida!");
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela de dados dos jogadores
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    first_join BIGINT NOT NULL,
                    last_join BIGINT NOT NULL,
                    
                    INDEX idx_username (username),
                    INDEX idx_last_join (last_join)
                )
            """);

            // Tabela de punições
            if (configManager.isMySQL()) {
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
            } else {
                // H2 não suporta ENUM, usar VARCHAR com CHECK
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
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments (player_uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_type ON punishments (type)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_active ON punishments (active)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_expires_at ON punishments (expires_at)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_created_at ON punishments (created_at)");
            }

            logger.info("Tabelas criadas com sucesso!");
        }
    }

    public void executeMigrationScript(String scriptName) throws SQLException {
        try (InputStream inputStream = getClass().getResourceAsStream("/" + scriptName)) {
            if (inputStream == null) {
                logger.warn("Script de migração não encontrado: " + scriptName);
                return;
            }

            String script = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Executar cada comando SQL separadamente
                String[] commands = script.split(";");
                for (String command : commands) {
                    command = command.trim();
                    if (!command.isEmpty() && !command.startsWith("--")) {
                        stmt.execute(command);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Erro ao executar script de migração: " + e.getMessage());
            throw new SQLException(e);
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

    public boolean isMySQL() {
        return configManager.isMySQL();
    }
}
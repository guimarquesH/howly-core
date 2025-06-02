package com.gilbertomorales.howlyvelocity.managers;

import com.gilbertomorales.howlyvelocity.config.ConfigManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private final ConfigManager configManager;
    private final Logger logger;
    private HikariDataSource dataSource;
    private boolean isMySQL;

    public DatabaseManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void initialize() {
        String databaseType = configManager.getDatabaseType().toLowerCase();
        
        try {
            if ("mysql".equals(databaseType)) {
                initializeMySQL();
            } else {
                initializeH2();
            }
            
            createTables();
            logger.info(LogColor.success("Database", "Conexão estabelecida com sucesso!"));
            
        } catch (Exception e) {
            logger.error(LogColor.error("Database", "Erro ao conectar com " + databaseType.toUpperCase() + ": " + e.getMessage()));
            
            if (isMySQL) {
                logger.warn(LogColor.warning("Database", "Tentando fallback para H2..."));
                try {
                    initializeH2();
                    createTables();
                    logger.info(LogColor.success("Database", "Fallback para H2 realizado com sucesso!"));
                } catch (Exception fallbackError) {
                    logger.error(LogColor.error("Database", "Erro no fallback para H2: " + fallbackError.getMessage()));
                    throw new RuntimeException("Falha ao inicializar banco de dados", fallbackError);
                }
            } else {
                throw new RuntimeException("Falha ao inicializar H2", e);
            }
        }
    }

    private void initializeMySQL() {
        JsonObject mysqlConfig = configManager.getMySQLConfig();
    
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=%s&serverTimezone=UTC",
            mysqlConfig.get("host").getAsString(),
            mysqlConfig.get("port").getAsInt(),
            mysqlConfig.get("database").getAsString(),
            mysqlConfig.get("useSSL").getAsBoolean(),
            mysqlConfig.get("autoReconnect").getAsBoolean()
        ));
        config.setUsername(mysqlConfig.get("username").getAsString());
        config.setPassword(mysqlConfig.get("password").getAsString());
        config.setMaximumPoolSize(mysqlConfig.get("maxPoolSize").getAsInt());
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
    
        dataSource = new HikariDataSource(config);
        isMySQL = true;
    
        logger.info(LogColor.info("Database", "Conectando ao MySQL..."));
    }

    private void initializeH2() {
        try {
            // Registrar o driver H2 explicitamente
            Class.forName("org.h2.Driver");
            
            File databaseFile = new File(configManager.getDataDirectory().toFile(), configManager.getDatabaseFile());
            
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.h2.Driver");
            config.setJdbcUrl("jdbc:h2:" + databaseFile.getAbsolutePath() + ";MODE=MySQL");
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            
            dataSource = new HikariDataSource(config);
            isMySQL = false;
            
            logger.info(LogColor.info("Database", "Conectando ao H2..."));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver H2 não encontrado", e);
        }
    }

    private void createTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // Tabela de jogadores
            String playersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "first_join BIGINT NOT NULL," +
                "last_join BIGINT NOT NULL," +
                "total_time BIGINT DEFAULT 0" +
                (isMySQL ? ", INDEX idx_username (username)" : "") +
                ")";

            // Tabela de punições
            String punishmentsTable = "CREATE TABLE IF NOT EXISTS punishments (" +
                (isMySQL ? "id INT AUTO_INCREMENT PRIMARY KEY," : "id INT AUTO_INCREMENT PRIMARY KEY,") +
                "player_uuid VARCHAR(36) NOT NULL," +
                (isMySQL ? "type ENUM('BAN', 'KICK', 'MUTE') NOT NULL," : "type VARCHAR(10) NOT NULL,") +
                "reason TEXT NOT NULL," +
                "punisher VARCHAR(36) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "expires_at BIGINT," +
                (isMySQL ? "active BOOLEAN DEFAULT TRUE" : "active BOOLEAN DEFAULT TRUE") +
                (isMySQL ? ", INDEX idx_player_uuid (player_uuid), INDEX idx_type (type), INDEX idx_active (active)" : "") +
                ")";

            try (PreparedStatement stmt1 = connection.prepareStatement(playersTable);
                 PreparedStatement stmt2 = connection.prepareStatement(punishmentsTable)) {
                
                stmt1.executeUpdate();
                stmt2.executeUpdate();
            }
            
            // Criar índices para H2 (já que não podemos criar na definição da tabela)
            if (!isMySQL) {
                try {
                    connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_username ON players(username)").executeUpdate();
                    connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid)").executeUpdate();
                    connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_type ON punishments(type)").executeUpdate();
                    connection.prepareStatement("CREATE INDEX IF NOT EXISTS idx_active ON punishments(active)").executeUpdate();
                } catch (SQLException e) {
                    logger.warn(LogColor.warning("Database", "Erro ao criar índices: " + e.getMessage()));
                }
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean isMySQL() {
        return isMySQL;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info(LogColor.info("Database", "Conexão fechada."));
        }
    }
}

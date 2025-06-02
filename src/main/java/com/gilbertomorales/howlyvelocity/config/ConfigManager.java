package com.gilbertomorales.howlyvelocity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigManager {

    private final Path dataDirectory;
    private final File configFile;
    private final Gson gson;
    private JsonObject config;

    public ConfigManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.configFile = new File(dataDirectory.toFile(), "config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void loadConfig() {
        try {
            // Criar diretório se não existir
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            // Criar arquivo de configuração padrão se não existir
            if (!configFile.exists()) {
                createDefaultConfig();
            }

            // Carregar configuração
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, JsonObject.class);
            }

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar configuração", e);
        }
    }

    private void createDefaultConfig() throws IOException {
        JsonObject defaultConfig = new JsonObject();
        
        // Configuração do banco de dados
        JsonObject database = new JsonObject();
        database.addProperty("type", "h2");
        database.addProperty("file", "howly");
        
        JsonObject mysql = new JsonObject();
        mysql.addProperty("host", "localhost");
        mysql.addProperty("port", 3306);
        mysql.addProperty("database", "howly");
        mysql.addProperty("username", "root");
        mysql.addProperty("password", "");
        mysql.addProperty("useSSL", false);
        mysql.addProperty("autoReconnect", true);
        mysql.addProperty("maxPoolSize", 10);
        
        database.add("mysql", mysql);
        defaultConfig.add("database", database);
        
        // Configurações gerais
        JsonObject general = new JsonObject();
        general.addProperty("prefix", "§8[§6Howly§8] §f");
        general.addProperty("debug", false);
        
        defaultConfig.add("general", general);

        // Salvar arquivo
        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(defaultConfig, writer);
        }
    }

    public String getDatabaseType() {
        return config.getAsJsonObject("database").get("type").getAsString();
    }

    public String getDatabaseFile() {
        return config.getAsJsonObject("database").get("file").getAsString();
    }

    public JsonObject getMySQLConfig() {
        return config.getAsJsonObject("database").getAsJsonObject("mysql");
    }

    public String getPrefix() {
        return config.getAsJsonObject("general").get("prefix").getAsString();
    }

    public boolean isDebug() {
        return config.getAsJsonObject("general").get("debug").getAsBoolean();
    }

    public JsonObject getConfig() {
        return config;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}

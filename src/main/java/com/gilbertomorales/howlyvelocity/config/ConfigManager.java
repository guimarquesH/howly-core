package com.gilbertomorales.howlyvelocity.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
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
            if (!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdirs();
            }

            if (!configFile.exists()) {
                createDefaultConfig();
            }

            try (Reader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, JsonObject.class);
            }

        } catch (IOException e) {
            e.printStackTrace();
            createDefaultConfig();
        }
    }

    private void createDefaultConfig() {
        config = new JsonObject();

        // Configurações do banco de dados
        JsonObject database = new JsonObject();
        database.addProperty("type", "h2");
        database.addProperty("host", "localhost");
        database.addProperty("port", 3306);
        database.addProperty("database", "howly");
        database.addProperty("username", "root");
        database.addProperty("password", "");
        config.add("database", database);

        // Configurações gerais
        JsonObject general = new JsonObject();
        general.addProperty("debug", false);
        general.addProperty("language", "pt_BR");
        config.add("general", general);

        // Configurações de chat
        JsonObject chat = new JsonObject();
        chat.addProperty("enable_colors", true);
        chat.addProperty("enable_global_chat", false);
        config.add("chat", chat);

        saveConfig();
    }

    public void saveConfig() {
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Métodos para acessar configurações do banco de dados
    public String getDatabaseType() {
        return config.getAsJsonObject("database").get("type").getAsString();
    }

    public String getDatabaseHost() {
        return config.getAsJsonObject("database").get("host").getAsString();
    }

    public int getDatabasePort() {
        return config.getAsJsonObject("database").get("port").getAsInt();
    }

    public String getDatabaseName() {
        return config.getAsJsonObject("database").get("database").getAsString();
    }

    public String getDatabaseUsername() {
        return config.getAsJsonObject("database").get("username").getAsString();
    }

    public String getDatabasePassword() {
        return config.getAsJsonObject("database").get("password").getAsString();
    }

    // Métodos para configurações gerais
    public boolean isDebugEnabled() {
        return config.getAsJsonObject("general").get("debug").getAsBoolean();
    }

    public String getLanguage() {
        return config.getAsJsonObject("general").get("language").getAsString();
    }

    // Métodos para configurações de chat
    public boolean isChatColorsEnabled() {
        return config.getAsJsonObject("chat").get("enable_colors").getAsBoolean();
    }

    public boolean isGlobalChatEnabled() {
        return config.getAsJsonObject("chat").get("enable_global_chat").getAsBoolean();
    }

    // Método para verificar se é MySQL
    public boolean isMySQL() {
        return "mysql".equalsIgnoreCase(getDatabaseType());
    }

    // Método para verificar se é H2
    public boolean isH2() {
        return "h2".equalsIgnoreCase(getDatabaseType());
    }

    // Método para verificar se é SQLite
    public boolean isSQLite() {
        return "sqlite".equalsIgnoreCase(getDatabaseType());
    }
}

package com.gilbertomorales.howlyvelocity;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.comandos.*;
import com.gilbertomorales.howlyvelocity.config.ConfigManager;
import com.gilbertomorales.howlyvelocity.listeners.ChatListener;
import com.gilbertomorales.howlyvelocity.listeners.PlayerListener;
import com.gilbertomorales.howlyvelocity.managers.DatabaseManager;
import com.gilbertomorales.howlyvelocity.managers.MedalManager;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.placeholder.PlaceholderManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "howlyvelocity",
    name = "HowlyVelocity",
    version = "1.0.0",
    description = "Core plugin for Howly Network",
    authors = {"GilbertoMorales"}
)
public class HowlyVelocity {

    private static HowlyVelocity instance;
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private TagManager tagManager;
    private MedalManager medalManager;
    private PlaceholderManager placeholderManager;
    private HowlyAPI api;

    @Inject
    public HowlyVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info(LogColor.GREEN + "[HowlyVelocity] " + LogColor.WHITE + "Iniciando plugin..." + LogColor.RESET);
        
        try {
            // Inicializar configuração
            configManager = new ConfigManager(dataDirectory);
            configManager.loadConfig();
            
            // Inicializar banco de dados
            databaseManager = new DatabaseManager(configManager, logger);
            databaseManager.initialize();
            
            // Inicializar gerenciadores
            playerDataManager = new PlayerDataManager(databaseManager);
            tagManager = new TagManager(dataDirectory);
            tagManager.loadTags();
            medalManager = new MedalManager(dataDirectory);
            medalManager.loadMedals();
            
            // Inicializar API
            api = new HowlyAPI(this);
            
            // Inicializar gerenciador de placeholders
            placeholderManager = new PlaceholderManager(tagManager, medalManager);
            
            // Registrar listeners
            server.getEventManager().register(this, new PlayerListener(server, logger, playerDataManager, tagManager));
            server.getEventManager().register(this, new ChatListener(server, api, tagManager, medalManager));
            
            // Registrar comandos
            registerCommands();
            
            logger.info(LogColor.success("HowlyVelocity", "Plugin carregado com sucesso!"));
            logger.info(LogColor.info("HowlyVelocity", "Banco de dados: " + configManager.getDatabaseType().toUpperCase()));
            
        } catch (Exception e) {
            logger.error(LogColor.error("HowlyVelocity", "Erro ao carregar plugin: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();
        
        commandManager.register("anuncio", new AnuncioCommand(server));
        commandManager.register("online", new OnlineCommand(server));
        commandManager.register("info", new InfoCommand(server, tagManager, medalManager));
        commandManager.register("find", new FindCommand(server, tagManager));
        commandManager.register("send", new SendCommand(server, tagManager));
        commandManager.register("versoes", new VersoesCommand(server));
        commandManager.register("tag", new TagCommand(server, tagManager));
        commandManager.register("medalha", new MedalCommand(server, medalManager));
        
        // Comandos de punição
        commandManager.register("ban", new BanCommand(server, tagManager));
        commandManager.register("kick", new KickCommand(server, tagManager));
        commandManager.register("mute", new MuteCommand(server, tagManager));
        commandManager.register("unban", new UnbanCommand(server, tagManager));
        commandManager.register("unmute", new UnmuteCommand(server, tagManager));
        
        logger.info(LogColor.success("HowlyVelocity", "Comandos registrados com sucesso!"));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info(LogColor.warning("HowlyVelocity", "Desligando plugin..."));
        
        if (tagManager != null) {
            tagManager.saveTags();
        }
        
        if (medalManager != null) {
            medalManager.saveMedals();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        logger.info(LogColor.success("HowlyVelocity", "Plugin desligado com sucesso!"));
    }

    // Getters
    public static HowlyVelocity getInstance() {
        return instance;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }
    
    public MedalManager getMedalManager() {
        return medalManager;
    }
    
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public HowlyAPI getAPI() {
        return api;
    }
}

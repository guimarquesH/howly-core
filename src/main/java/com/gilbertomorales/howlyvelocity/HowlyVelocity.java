package com.gilbertomorales.howlyvelocity;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.comandos.*;
import com.gilbertomorales.howlyvelocity.config.ConfigManager;
import com.gilbertomorales.howlyvelocity.listeners.ChatListener;
import com.gilbertomorales.howlyvelocity.listeners.PlayerListener;
import com.gilbertomorales.howlyvelocity.managers.*;
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
        version = "1.1.0",
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
    private IgnoreManager ignoreManager;
    private ChatManager chatManager;
    private PlaceholderManager placeholderManager;
    private HowlyAPI api;

    @Inject
    public HowlyVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;

        // Definir codificação UTF-8 para o sistema
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
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

            // Inicializar gerenciadores básicos
            playerDataManager = new PlayerDataManager(databaseManager);

            // Inicializar TagManager e MedalManager DIRETO com banco de dados
            tagManager = new TagManager(dataDirectory);
            tagManager.loadTags();

            medalManager = new MedalManager(databaseManager);
            medalManager.loadMedals();

            ignoreManager = new IgnoreManager(dataDirectory);
            ignoreManager.loadIgnoreData();

            // Inicializar ChatManager SEM a API ainda
            chatManager = new ChatManager(server, tagManager, medalManager);

            // Inicializar API
            api = new HowlyAPI(this);

            // AGORA inicializar a PunishmentAPI no ChatManager
            chatManager.initializePunishmentAPI();

            // Inicializar gerenciador de placeholders
            placeholderManager = new PlaceholderManager(tagManager, medalManager);

            // Registrar listeners
            server.getEventManager().register(this, new PlayerListener(server, logger, playerDataManager, tagManager));
            server.getEventManager().register(this, new ChatListener(server, api, tagManager, medalManager, ignoreManager, chatManager));

            // Registrar comandos
            registerCommands();

            logger.info(LogColor.success("HowlyVelocity", "Plugin carregado com sucesso!"));
            logger.info(LogColor.info("HowlyVelocity", "Banco de dados: " + configManager.getDatabaseType().toUpperCase()));
            logger.info(LogColor.info("HowlyVelocity", "Tags e medalhas sendo salvos no banco de dados!"));

        } catch (Exception e) {
            logger.error(LogColor.error("HowlyVelocity", "Erro ao carregar plugin: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();

        // Comandos básicos
        commandManager.register("anuncio", new AnuncioCommand(server));
        commandManager.register("online", new OnlineCommand(server));
        commandManager.register("info", new InfoCommand(server, tagManager, medalManager));
        commandManager.register("find", new FindCommand(server, tagManager));
        commandManager.register("send", new SendCommand(server, tagManager));
        commandManager.register("versoes", new VersoesCommand(server));

        // Comandos de personalização
        commandManager.register("tag", new TagCommand(server, tagManager));
        commandManager.register("medalha", new MedalCommand(server, medalManager));

        // Sistema de ignorar
        commandManager.register("ignorar", new IgnorarCommand(server, ignoreManager, playerDataManager, tagManager));

        // Comandos de mensagem privada
        TellCommand tellCommand = new TellCommand(server, tagManager, ignoreManager);
        commandManager.register("tell", tellCommand);
        commandManager.register("msg", tellCommand);
        commandManager.register("w", tellCommand);
        commandManager.register("r", new ReplyCommand(server, tagManager, ignoreManager, tellCommand));

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

        if (ignoreManager != null) {
            ignoreManager.saveIgnoreData();
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

    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public HowlyAPI getAPI() {
        return api;
    }
}

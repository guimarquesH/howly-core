package com.gilbertomorales.howlyvelocity.listeners;

import com.gilbertomorales.howlyvelocity.api.HowlyAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.managers.PlayerDataManager;
import com.gilbertomorales.howlyvelocity.managers.TagManager;
import com.gilbertomorales.howlyvelocity.utils.LogColor;
import com.gilbertomorales.howlyvelocity.utils.TimeUtils;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class PlayerListener {

    private final ProxyServer server;
    private final Logger logger;
    private final PlayerDataManager playerDataManager;
    private final TagManager tagManager;
    private final PunishmentAPI punishmentAPI;

    public PlayerListener(ProxyServer server, Logger logger, PlayerDataManager playerDataManager, TagManager tagManager) {
        this.server = server;
        this.logger = logger;
        this.playerDataManager = playerDataManager;
        this.tagManager = tagManager;
        this.punishmentAPI = HowlyAPI.getInstance().getPunishmentAPI();
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        
        // Verificar se o jogador está banido de forma síncrona
        punishmentAPI.getActiveBan(player.getUniqueId()).thenAccept(ban -> {
            if (ban != null) {
                String message = "§c§lVOCÊ ESTÁ BANIDO!\n\n" +
                               "§fMotivo: §c" + ban.getReason() + "\n" +
                               "§fPunidor: §e" + ban.getPunisher() + "\n" +
                               "§fDuração: §a" + (ban.isPermanent() ? "Permanente" : TimeUtils.formatDuration(ban.getRemainingTime())) + "\n\n" +
                               "§7Apele em: §bdiscord.gg/howly";
                
                // Desconectar o jogador imediatamente
                player.disconnect(Component.text(message));
                logger.info(LogColor.warning("Login", "Jogador " + player.getUsername() + " tentou conectar, mas está banido: " + ban.getReason()));
                return;
            }
            
            // Salvar dados do jogador se não estiver banido
            playerDataManager.savePlayerData(player.getUniqueId(), player.getUsername());
            logger.info(LogColor.GREEN + "[Login] " + LogColor.WHITE + player.getUsername() + " " + LogColor.BRIGHT_BLACK + "(" + player.getUniqueId() + ") conectou-se à rede" + LogColor.RESET);
        }).exceptionally(throwable -> {
            // Em caso de erro na verificação, permitir o login mas logar o erro
            logger.error(LogColor.error("Login", "Erro ao verificar banimento de " + player.getUsername() + ": " + throwable.getMessage()));
            playerDataManager.savePlayerData(player.getUniqueId(), player.getUsername());
            return null;
        });
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        
        logger.info(LogColor.YELLOW + "[Servidor] " + LogColor.WHITE + player.getUsername() + " " + LogColor.BRIGHT_BLACK + "conectou-se ao servidor " + LogColor.GREEN + serverName + LogColor.RESET);
    }
}

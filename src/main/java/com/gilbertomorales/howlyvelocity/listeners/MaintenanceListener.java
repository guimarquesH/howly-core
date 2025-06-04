package com.gilbertomorales.howlyvelocity.listeners;

import com.gilbertomorales.howlyvelocity.managers.MOTDManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MaintenanceListener {

    private final MOTDManager motdManager;

    public MaintenanceListener(MOTDManager motdManager) {
        this.motdManager = motdManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent event) {
        if (!motdManager.isInMaintenance()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Permitir entrada apenas para jogadores com permissão
        if (!player.hasPermission("howly.coordenador")) {
            String kickMessage = motdManager.getMaintenanceKickMessage();
            event.setResult(LoginEvent.ComponentResult.denied(
                LegacyComponentSerializer.legacySection().deserialize(kickMessage)
            ));
        }
    }
}

package com.gilbertomorales.howlyvelocity.listeners;

import com.gilbertomorales.howlyvelocity.managers.MOTDManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ServerPingListener {

    private final MOTDManager motdManager;

    public ServerPingListener(MOTDManager motdManager) {
        this.motdManager = motdManager;
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder builder = event.getPing().asBuilder();

        // Definir o MOTD
        Component motd = LegacyComponentSerializer.legacySection().deserialize(motdManager.getFullMotd());
        builder.description(motd);

        // Se estiver em manutenção, mostrar 0 jogadores online
        if (motdManager.isInMaintenance()) {
            builder.onlinePlayers(0);
        }

        event.setPing(builder.build());
    }
}

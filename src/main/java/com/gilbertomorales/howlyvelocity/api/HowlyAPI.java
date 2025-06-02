package com.gilbertomorales.howlyvelocity.api;

import com.gilbertomorales.howlyvelocity.HowlyVelocity;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentAPI;
import com.gilbertomorales.howlyvelocity.api.punishment.PunishmentManager;

public class HowlyAPI {

    private static HowlyAPI instance;
    
    private final HowlyVelocity plugin;
    private final PunishmentAPI punishmentAPI;

    public HowlyAPI(HowlyVelocity plugin) {
        this.plugin = plugin;
        this.punishmentAPI = new PunishmentManager(plugin.getDatabaseManager(), plugin.getServer());
        instance = this;
    }

    public static HowlyAPI getInstance() {
        return instance;
    }

    public PunishmentAPI getPunishmentAPI() {
        return punishmentAPI;
    }

    public HowlyVelocity getPlugin() {
        return plugin;
    }
}

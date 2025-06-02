package com.gilbertomorales.howlyvelocity.api.punishment.events;

import com.gilbertomorales.howlyvelocity.api.punishment.Punishment;

public class PunishmentEvent {

    private final Punishment punishment;

    public PunishmentEvent(Punishment punishment) {
        this.punishment = punishment;
    }

    public Punishment getPunishment() {
        return punishment;
    }
}

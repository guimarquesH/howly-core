package com.gilbertomorales.howlyvelocity.api.punishment;

public enum PunishmentType {
    BAN("Banimento"),
    KICK("Expulsão"),
    MUTE("Silenciamento");

    private final String displayName;

    PunishmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PunishmentType fromString(String type) {
        try {
            return PunishmentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

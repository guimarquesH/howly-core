package com.gilbertomorales.howlyvelocity.api.punishment;

import java.util.UUID;

public class Punishment {

    private final int id;
    private final UUID playerUUID;
    private final PunishmentType type;
    private final String reason;
    private final String punisher;
    private final long createdAt;
    private final Long expiresAt;
    private boolean active;

    public Punishment(int id, UUID playerUUID, PunishmentType type, String reason, 
                     String punisher, long createdAt, Long expiresAt, boolean active) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.type = type;
        this.reason = reason;
        this.punisher = punisher;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public String getPunisher() {
        return punisher;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTime() {
        if (isPermanent()) {
            return -1;
        }
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "Punishment{" +
                "id=" + id +
                ", playerUUID=" + playerUUID +
                ", type=" + type +
                ", reason='" + reason + '\'' +
                ", punisher='" + punisher + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", active=" + active +
                '}';
    }
}

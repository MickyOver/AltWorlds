package com.altworlds;

import java.util.UUID;

public class WorldInvite {
    private final UUID ownerUuid;
    private final String worldName;
    private final long timestamp;

    public WorldInvite(UUID ownerUuid, String worldName) {
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getWorldName() { return worldName; }
    public boolean isExpired(int timeoutSeconds) {
        return (System.currentTimeMillis() - timestamp) > (timeoutSeconds * 1000L);
    }
}
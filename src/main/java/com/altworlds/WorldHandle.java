package com.altworlds;

import org.bukkit.Bukkit;
import org.bukkit.World;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles logic for a specific world instance (Locks & Lifecycle).
 */
public class WorldHandle {
    private final String worldName;
    private final long loadedAt;
    private long lastActivity;
    private final AtomicInteger locks = new AtomicInteger(0);

    public WorldHandle(String worldName) {
        this.worldName = worldName;
        this.loadedAt = System.currentTimeMillis();
        this.touch();
    }

    public String getName() { return worldName; }

    public void touch() {
        this.lastActivity = System.currentTimeMillis();
    }

    public void addLock() {
        touch();
        locks.incrementAndGet();
    }

    public void removeLock() {
        touch();
        if (locks.get() > 0) locks.decrementAndGet();
    }

    public WorldState checkState(long minUptimeMs, long cooldownMs) {
        if (locks.get() > 0) return WorldState.ACTIVE;

        World w = Bukkit.getWorld(worldName);
        if (w == null) return WorldState.READY_TO_UNLOAD;

        if (!w.getPlayers().isEmpty()) {
            touch();
            return WorldState.ACTIVE;
        }

        long now = System.currentTimeMillis();
        if ((now - loadedAt) < minUptimeMs) return WorldState.LOADING;
        if ((now - lastActivity) < cooldownMs) return WorldState.IDLE;

        return WorldState.READY_TO_UNLOAD;
    }

    /**
     * Attempts to unload the world.
     * @return true if successfully unloaded, false if Bukkit refused.
     */
    public boolean unload() {
        World w = Bukkit.getWorld(worldName);
        if (w != null) {
            w.save();
            return Bukkit.unloadWorld(w, true);
        }
        return false;
    }
}
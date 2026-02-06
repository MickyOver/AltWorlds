package com.altworlds;

import com.altworlds.AltWorldsPlugin;
import org.bukkit.Bukkit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WorldHandles and runs the cleanup task (Janitor).
 */
public class WorldManager {
    private final AltWorldsPlugin plugin;
    private final Map<String, WorldHandle> handles = new ConcurrentHashMap<>();

    private final long minUptimeMs;
    private final long cooldownMs;
    private final String lobbyName;
    private final boolean autoUnloadEnabled;

    public WorldManager(AltWorldsPlugin plugin) {
        this.plugin = plugin;
        this.lobbyName = plugin.getConfig().getString("lobby.worldName", "lobby");
        this.autoUnloadEnabled = plugin.getConfig().getBoolean("performance.unloadWorldsWhenEmpty", true);

        this.cooldownMs = plugin.getConfig().getInt("performance.unloadDelaySeconds", 45) * 1000L;
        this.minUptimeMs = plugin.getConfig().getInt("performance.minWorldUptimeSeconds", 60) * 1000L;

        if (autoUnloadEnabled) {
            // Run Janitor every 5 seconds (100 ticks)
            Bukkit.getScheduler().runTaskTimer(plugin, this::runJanitor, 100L, 100L);
        }
    }

    public WorldHandle getHandle(String worldName) {
        return handles.computeIfAbsent(worldName, WorldHandle::new);
    }

    public void removeHandle(String worldName) {
        handles.remove(worldName);
    }

    private void runJanitor() {
        for (WorldHandle handle : handles.values()) {
            String name = handle.getName();

            // Exclude critical worlds
            if (name.equals(lobbyName) || name.equals("world") || name.contains("AltWorlds/lobby")) {
                continue;
            }

            WorldState state = handle.checkState(minUptimeMs, cooldownMs);

            if (state == WorldState.READY_TO_UNLOAD) {
                boolean success = handle.unload();
                // Only log if actually unloaded (prevents spam)
                if (success) {
                    plugin.getLogger().info("[AltWorlds] Lifecycle: Unloading expired world -> " + name);
                }
            }
        }
    }

    public void shutdown() {
        handles.clear();
    }
}
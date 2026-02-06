package com.altworlds;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.SpawnChangeEvent;

public class WorldSpawnListener implements Listener {

    @EventHandler
    public void onSpawnChange(SpawnChangeEvent e) {
        World w = e.getWorld();
        // En cuanto detectamos que cambias el spawn (con /setworldspawn o plugins)
        // Forzamos el radio a 0 para que sea EXACTO y no aleatorio.
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
    }
}
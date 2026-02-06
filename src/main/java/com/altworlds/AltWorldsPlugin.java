package com.altworlds;

import com.altworlds.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AltWorldsPlugin extends JavaPlugin {
    private AltWorldsService service;
    private AltWorldsGUI gui;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        // Guardar configuraciÃ³n por defecto
        saveDefaultConfig();

        // 1. Inicializar WorldManager (Ciclo de vida de mundos)
        this.worldManager = new WorldManager(this);

        // 2. Inicializar Service (GestiÃ³n de datos, Archivos por jugador y Base64)
        this.service = new AltWorldsService(this, worldManager);
        // NOTA: service.loadData() ya no es estrictamente necesario para players.yml global,
        // pero sÃ­ para cargar templates y mundos del servidor.
        this.service.loadData();

        // 3. Inicializar GUI
        this.gui = new AltWorldsGUI(this);

        // 4. Registrar Comandos
        AltWorldsCommand cmd = new AltWorldsCommand(this, service);
        getCommand("aw").setExecutor(cmd);
        getCommand("aw").setTabCompleter(cmd);

        // 5. Registrar Listeners
        getServer().getPluginManager().registerEvents(new WorldProtectionListener(service), this);
        getServer().getPluginManager().registerEvents(new WorldRulesListener(service), this);
        getServer().getPluginManager().registerEvents(new WorldSpawnListener(), this);

        getLogger().info("AltWorlds v3.0 (Professional Edition) enabled.");
    }

    @Override
    public void onDisable() {
        // Orden importante:
        // 1. Guardar datos de jugadores (Inventarios/Location)
        if (service != null) {
            service.shutdown();
        }

        // 2. Descargar mundos si es necesario
        if (worldManager != null) {
            worldManager.shutdown();
        }
    }

    public AltWorldsService getService() { return service; }
    public AltWorldsGUI getGui() { return gui; }
    public WorldManager getWorldManager() { return worldManager; }
}
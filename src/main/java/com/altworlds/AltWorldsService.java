package com.altworlds;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AltWorldsService implements Listener {
    private final AltWorldsPlugin plugin;
    private final WorldManager worldManager;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static Component c(String legacy) {
        return legacy == null ? Component.empty() : LEGACY.deserialize(legacy);
    }

    private static String plain(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    private static String plainDisplayName(ItemMeta meta) {
        if (meta == null) return "";
        return plain(meta.displayName());
    }

    private static String stripLegacy(String legacy) {
        return legacy == null ? "" : plain(LEGACY.deserialize(legacy));
    }

    private static GameMode parseGameMode(String raw, GameMode fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return GameMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Difficulty parseDifficulty(String raw, Difficulty fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // CARPETAS Y RUTAS
    private final File playersDataFolder;
    private final File settingsDataFolder; // Nueva carpeta data/settings

    private static final String BASE_LOBBY_PATH = "plugins/AltWorlds/lobby";
    private static final String BASE_USER_WORLDS_PATH = "plugins/AltWorlds/data/player-worlds";
    private static final String BASE_ALL_WORLDS_PATH = "plugins/AltWorlds/all-worlds";

    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // CACHÃ‰S
    private final Map<UUID, File> playerFileCache = new ConcurrentHashMap<>();
    private final Map<UUID, YamlConfiguration> playerConfigs = new ConcurrentHashMap<>();
    private final Map<String, YamlConfiguration> userSettingsCache = new ConcurrentHashMap<>();

    // CONFIGURACIONES CARGADAS EN MEMORIA (Desde data/settings/)
    private TemplatesConfig templates;
    private YamlConfiguration autoWorldsSourceCfg;
    private YamlConfiguration privateWorldsSourceCfg;
    private YamlConfiguration allWorldsSourceCfg;
    private YamlConfiguration lobbySettingsCfg;

    // DATOS TEMPORALES
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, WorldInvite> pendingInvites = new HashMap<>();

    public AltWorldsService(AltWorldsPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;

        // Inicializar carpetas base
        this.playersDataFolder = new File(plugin.getDataFolder(), "data/players");
        ensureDir(playersDataFolder, "players data");

        this.settingsDataFolder = new File(plugin.getDataFolder(), "data/settings");
        ensureDir(settingsDataFolder, "settings data");

        // Asegurar carpeta de mundos de usuarios
        ensureDir(new File(BASE_USER_WORLDS_PATH), "user worlds");

        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadData();
        startActionBarTask();
    }

    public AltWorldsPlugin getPlugin() { return plugin; }

    // --- CARGA DE DATOS ---

    public void reloadTemplates() {
        this.templates = TemplatesConfig.load(plugin);
        loadSourceConfigs();
    }

    public TemplatesConfig getTemplates() { return templates; }

    private void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR && !p.hasPermission("altworlds.admin")) {
                    p.sendActionBar(c("§eType §6/aw lobby §eto exit"));
                }
            }
        }, 0L, 20L);
    }

    public void loadData() {
        // Crear estructura bÃ¡sica de carpetas fÃ­sicas
        ensureDir(plugin.getDataFolder(), "plugin data folder");
        ensureDir(new File(plugin.getDataFolder(), "data"), "data");
        ensureDir(new File(plugin.getDataFolder(), "data/players"), "players data");
        ensureDir(new File(plugin.getDataFolder(), "data/settings"), "settings data");
        ensureDir(new File(BASE_USER_WORLDS_PATH), "user worlds");

        // Carpetas fÃ­sicas para contenido (Schematics/Mundos fuente)
        ensureDir(new File(plugin.getDataFolder(), "templates"), "templates");
        ensureDir(new File(plugin.getDataFolder(), "private-worlds"), "private-worlds");
        ensureDir(new File(plugin.getDataFolder(), "auto-worlds"), "auto-worlds");
        ensureDir(new File(plugin.getDataFolder(), "all-worlds"), "all-worlds");

        // 1. Cargar configuraciones (Esto crea/lee los YML en data/settings/)
        reloadTemplates();

        // 2. Cargar lobby
        loadLobbyWorld();

        // 3. Limpiar cachÃ© de configuraciones de usuarios
        userSettingsCache.clear();
        refreshActiveWorldsSettings();
    }

    private void loadSourceConfigs() {
        // ConfiguraciÃ³n: AUTO-WORLDS
        // Escanea la carpeta fÃ­sica "auto-worlds" pero guarda la config en "data/settings/auto-worlds.yml"
        File autoDir = new File(plugin.getDataFolder(), "auto-worlds");
        ensureDir(autoDir, "auto-worlds");
        File autoYml = new File(settingsDataFolder, "auto-worlds.yml");
        this.autoWorldsSourceCfg = loadAndPopulateSource(autoDir, autoYml, "auto-worlds");

        // ConfiguraciÃ³n: PRIVATE-WORLDS
        File privDir = new File(plugin.getDataFolder(), "private-worlds");
        ensureDir(privDir, "private-worlds");
        File privYml = new File(settingsDataFolder, "private-worlds.yml");
        this.privateWorldsSourceCfg = loadAndPopulateSource(privDir, privYml, "private-worlds");

        // ConfiguraciÃ³n: ALL-WORLDS (Mundos de servidor)
        File allDir = new File(plugin.getDataFolder(), "all-worlds");
        ensureDir(allDir, "all-worlds");
        File allYml = new File(settingsDataFolder, "all-worlds.yml");
        this.allWorldsSourceCfg = loadAndPopulateSource(allDir, allYml, "all-worlds");

        // ConfiguraciÃ³n: LOBBY
        ensureLobbySettingsFile();
    }

    /**
     * Escanea 'sourceDir' en busca de carpetas. Si encuentra alguna nueva,
     * la aÃ±ade al 'ymlFile' (ubicado en data/settings).
     */
    private YamlConfiguration loadAndPopulateSource(File sourceDir, File ymlFile, String rootSection) {
        ensureFile(ymlFile, ymlFile.getName());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(ymlFile);
        File[] folders = sourceDir.listFiles(File::isDirectory);
        boolean changed = false;

        if (folders != null) {
            for (File f : folders) {
                String key = f.getName();
                String path = rootSection + "." + key;
                if (!config.contains(path)) {
                    String icon = rootSection.equals("all-worlds") ? "NETHER_STAR" : "GRASS_BLOCK";
                    config.set(path + ".name", "&b" + capitalize(key));
                    config.set(path + ".icon", icon);
                    config.createSection(path + ".settings", getDefaultSettingsMap());
                    changed = true;
                }
            }
        }
        if (changed) {
            try { config.save(ymlFile); }
            catch (IOException e) { plugin.getLogger().warning("Could not save " + ymlFile.getPath() + ": " + e.getMessage()); }
        }
        return config;
    }

    public void ensureLobbySettingsFile() {
        File lobbyYml = new File(settingsDataFolder, "lobby.yml");

        // Si no existe, lo creamos con valores por defecto
        if (!lobbyYml.exists()) {
            try {
                ensureFile(lobbyYml, "lobby.yml");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(lobbyYml);

                String configIcon = plugin.getConfig().getString("gui.main.lobby", "BEACON");

                config.set("lobby.name", "&bServer Lobby");
                config.set("lobby.icon", configIcon);

                // Cargar defaults desde config.txt si existen, o hardcoded
                ConfigurationSection defaults = plugin.getConfig().getConfigurationSection("world-defaults.lobby");
                Map<String, Object> settings = getDefaultSettingsMap();
                if (defaults != null) {
                    for (String k : defaults.getKeys(false)) {
                        if (settings.containsKey(k)) settings.put(k, defaults.get(k));
                    }
                }
                config.createSection("lobby.settings", settings);

                config.save(lobbyYml);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create lobby.yml: " + e.getMessage());
            }
        }
        // Cargar en memoria
        this.lobbySettingsCfg = YamlConfiguration.loadConfiguration(lobbyYml);
    }

    // --- GESTIÃ“N DE ARCHIVOS POR JUGADOR ---

    private File resolvePlayerFile(UUID uuid, String currentPlayerName) {
        if (playerFileCache.containsKey(uuid)) {
            return playerFileCache.get(uuid);
        }

        File[] found = playersDataFolder.listFiles((dir, name) -> name.endsWith(uuid + ".yml"));

        if (found != null && found.length > 0) {
            File existingFile = found[0];
            if (currentPlayerName != null && !existingFile.getName().equals(currentPlayerName + "-" + uuid + ".yml")) {
                File newNameFile = new File(playersDataFolder, currentPlayerName + "-" + uuid + ".yml");
                if (existingFile.renameTo(newNameFile)) {
                    existingFile = newNameFile;
                }
            }
            playerFileCache.put(uuid, existingFile);
            return existingFile;
        }

        String fileName = (currentPlayerName != null ? currentPlayerName : "Unknown") + "-" + uuid + ".yml";
        File newFile = new File(playersDataFolder, fileName);
        playerFileCache.put(uuid, newFile);
        return newFile;
    }

    public YamlConfiguration getPlayerConfig(UUID uuid) {
        return getPlayerConfig(uuid, null);
    }

    public YamlConfiguration getPlayerConfig(UUID uuid, String knownName) {
        if (playerConfigs.containsKey(uuid)) {
            return playerConfigs.get(uuid);
        }
        File file = resolvePlayerFile(uuid, knownName);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        playerConfigs.put(uuid, config);
        return config;
    }

    public void savePlayerConfig(UUID uuid) {
        if (!playerConfigs.containsKey(uuid)) return;
        try {
            YamlConfiguration config = playerConfigs.get(uuid);
            File file = resolvePlayerFile(uuid, null);
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data for " + uuid);
        }
    }

    public void unloadPlayerConfig(UUID uuid) {
        savePlayerConfig(uuid);
        playerConfigs.remove(uuid);
        playerFileCache.remove(uuid);
    }

    public void shutdown() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            saveLastLocation(p, p.getLocation());
            savePlayerInventory(p, p.getWorld());
            unloadPlayerConfig(p.getUniqueId());
        }
    }

    // --- MÃ‰TODOS PÃšBLICOS DE SETTINGS CORREGIDOS ---

    public boolean getSetting(UUID owner, String wName, String key, boolean def) {
        // 1. LOBBY (Prioridad)
        if (isLobbyWorld(wName)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            return lobbySettingsCfg.getBoolean("lobby.settings." + key, def);
        }

        // 2. MUNDOS DEL SERVIDOR (All-Worlds)
        if (owner.equals(SERVER_UUID)) {
            return allWorldsSourceCfg.getBoolean("all-worlds." + wName + ".settings." + key, def);
        }

        // 3. MUNDOS DE JUGADOR
        String internal = getPlayerConfig(owner).getString("worlds." + wName + ".internal");

        if (internal == null || !new File(internal).exists()) {
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();
            if (ownerName == null) ownerName = getPlayerConfig(owner).getString("name", "Unknown");
            String userFolder = ownerName + "_" + owner;
            internal = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + wName;
        }

        File worldFolder = new File(internal);
        if (!worldFolder.exists()) return def;

        YamlConfiguration config = getUserSettingsConfig(worldFolder.getParentFile());
        return config.getBoolean("worlds." + wName + ".settings." + key, def);
    }

    public String getSettingString(UUID owner, String wName, String key, String def) {
        // 1. LOBBY (Prioridad)
        if (isLobbyWorld(wName)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            return lobbySettingsCfg.getString("lobby.settings." + key, def);
        }

        // 2. MUNDOS DEL SERVIDOR (All-Worlds)
        if (owner.equals(SERVER_UUID)) {
            return allWorldsSourceCfg.getString("all-worlds." + wName + ".settings." + key, def);
        }

        // 3. MUNDOS DE JUGADOR
        String internal = getPlayerConfig(owner).getString("worlds." + wName + ".internal");

        if (internal == null || !new File(internal).exists()) {
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();
            if (ownerName == null) ownerName = getPlayerConfig(owner).getString("name", "Unknown");
            String userFolder = ownerName + "_" + owner;
            internal = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + wName;
        }

        File worldFolder = new File(internal);
        if (!worldFolder.exists()) return def;

        YamlConfiguration config = getUserSettingsConfig(worldFolder.getParentFile());
        return config.getString("worlds." + wName + ".settings." + key, def);
    }

    public void setWorldSetting(UUID owner, String wName, String key, boolean value) {
        // 1. LOBBY
        if (isLobbyWorld(wName)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            lobbySettingsCfg.set("lobby.settings." + key, value);
            try {
                lobbySettingsCfg.save(new File(settingsDataFolder, "lobby.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save lobby.yml settings: " + e.getMessage());
            }

            World lobby = Bukkit.getWorld(wName);
            if (lobby == null) lobby = Bukkit.getWorld(plugin.getConfig().getString("lobby.worldName", "lobby"));
            if (lobby != null) applyGameRules(lobby);
            return;
        }

        // 2. MUNDOS DEL SERVIDOR
        if (owner.equals(SERVER_UUID)) {
            allWorldsSourceCfg.set("all-worlds." + wName + ".settings." + key, value);
            try {
                allWorldsSourceCfg.save(new File(settingsDataFolder, "all-worlds.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save all-worlds.yml settings: " + e.getMessage());
            }

            World w = Bukkit.getWorld(new File(plugin.getDataFolder(), "all-worlds/" + wName).getPath().replace("\\", "/"));
            if (w != null) applyGameRules(w);
            return;
        }

        // 3. MUNDOS DE JUGADOR
        String internal = getPlayerConfig(owner).getString("worlds." + wName + ".internal");
        if (internal == null || !new File(internal).exists()) {
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();
            if (ownerName == null) ownerName = getPlayerConfig(owner).getString("name", "Unknown");
            String userFolder = ownerName + "_" + owner;
            internal = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + wName;
        }

        File worldFolder = new File(internal);
        if (!worldFolder.exists()) return;

        YamlConfiguration config = getUserSettingsConfig(worldFolder.getParentFile());
        config.set("worlds." + wName + ".settings." + key, value);
        saveUserSettingsConfig(worldFolder.getParentFile(), config);

        World w = Bukkit.getWorld(internal);
        if (w != null) applyGameRules(w);
    }

    public void setWorldSettingString(UUID owner, String wName, String key, String value) {
        // 1. LOBBY
        if (isLobbyWorld(wName)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            lobbySettingsCfg.set("lobby.settings." + key, value);
            try {
                lobbySettingsCfg.save(new File(settingsDataFolder, "lobby.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save lobby.yml settings: " + e.getMessage());
            }

            World lobby = Bukkit.getWorld(wName);
            if (lobby == null) lobby = Bukkit.getWorld(plugin.getConfig().getString("lobby.worldName", "lobby"));
            if (lobby != null) applyGameRules(lobby);
            return;
        }

        // 2. MUNDOS DEL SERVIDOR
        if (owner.equals(SERVER_UUID)) {
            allWorldsSourceCfg.set("all-worlds." + wName + ".settings." + key, value);
            try {
                allWorldsSourceCfg.save(new File(settingsDataFolder, "all-worlds.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save all-worlds.yml settings: " + e.getMessage());
            }

            World w = Bukkit.getWorld(new File(plugin.getDataFolder(), "all-worlds/" + wName).getPath().replace("\\", "/"));
            if (w != null) applyGameRules(w);
            return;
        }

        // 3. MUNDOS DE JUGADOR
        String internal = getPlayerConfig(owner).getString("worlds." + wName + ".internal");
        if (internal == null || !new File(internal).exists()) {
            String ownerName = Bukkit.getOfflinePlayer(owner).getName();
            if (ownerName == null) ownerName = getPlayerConfig(owner).getString("name", "Unknown");
            String userFolder = ownerName + "_" + owner;
            internal = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + wName;
        }

        File worldFolder = new File(internal);
        if (!worldFolder.exists()) return;

        YamlConfiguration config = getUserSettingsConfig(worldFolder.getParentFile());
        config.set("worlds." + wName + ".settings." + key, value);
        saveUserSettingsConfig(worldFolder.getParentFile(), config);

        World w = Bukkit.getWorld(internal);
        if (w != null) applyGameRules(w);
    }

    public Material getWorldIcon(UUID u, String w) {
        if (u == null) return Material.BARRIER;

        // 1. LOBBY
        if (isLobbyWorld(w)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            String iconStr = lobbySettingsCfg.getString("lobby.icon", "BEACON");
            return Material.getMaterial(iconStr) != null ? Material.getMaterial(iconStr) : Material.BEACON;
        }

        // 2. SERVER WORLDS
        if (u.equals(SERVER_UUID)) {
            String iconStr = allWorldsSourceCfg.getString("all-worlds." + w + ".icon", "NETHER_STAR");
            return Material.getMaterial(iconStr) != null ? Material.getMaterial(iconStr) : Material.NETHER_STAR;
        }

        // 3. PLAYER WORLDS
        String ownerName = Bukkit.getOfflinePlayer(u).getName();
        if (ownerName == null) ownerName = getPlayerConfig(u).getString("name", "Unknown");

        String userFolder = ownerName + "_" + u;
        File folder = new File(BASE_USER_WORLDS_PATH, userFolder + "/" + w);

        if (!folder.exists()) {
            String legacy = getPlayerConfig(u).getString("worlds." + w + ".internal");
            if (legacy != null) folder = new File(legacy);
        }

        if (!folder.exists()) return Material.GRASS_BLOCK;

        YamlConfiguration config = getUserSettingsConfig(folder.getParentFile());
        String iconStr = config.getString("worlds." + w + ".icon", "GRASS_BLOCK");
        return Material.getMaterial(iconStr) != null ? Material.getMaterial(iconStr) : Material.GRASS_BLOCK;
    }

    public void setWorldIcon(UUID owner, String wName, Material icon) {
        // 1. LOBBY
        if (isLobbyWorld(wName)) {
            if (lobbySettingsCfg == null) ensureLobbySettingsFile();
            lobbySettingsCfg.set("lobby.icon", icon.name());
            try {
                lobbySettingsCfg.save(new File(settingsDataFolder, "lobby.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save lobby.yml icon: " + e.getMessage());
            }
            return;
        }

        // 2. SERVER WORLDS
        if (owner.equals(SERVER_UUID)) {
            allWorldsSourceCfg.set("all-worlds." + wName + ".icon", icon.name());
            try {
                allWorldsSourceCfg.save(new File(settingsDataFolder, "all-worlds.yml"));
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save all-worlds.yml icon: " + e.getMessage());
            }
            return;
        }

        // 3. PLAYER WORLDS
        String ownerName = Bukkit.getOfflinePlayer(owner).getName();
        if (ownerName == null) ownerName = getPlayerConfig(owner).getString("name", "Unknown");
        String userFolder = ownerName + "_" + owner;
        File folder = new File(BASE_USER_WORLDS_PATH, userFolder + "/" + wName);

        if (folder.exists()) {
            YamlConfiguration config = getUserSettingsConfig(folder.getParentFile());
            config.set("worlds." + wName + ".icon", icon.name());
            saveUserSettingsConfig(folder.getParentFile(), config);
        }
    }

    public static Map<String, Object> getDefaultSettingsMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fly", false); m.put("fallDamage", true); m.put("public", true);
        m.put("doFireTick", true); m.put("doWeatherCycle", true); m.put("doTileDrops", true);
        m.put("hunger", true); m.put("doDaylightCycle", true); m.put("allowVisitors", true);
        m.put("randomTickSpeed", true); m.put("mobGriefing", true); m.put("pvp", false);
        m.put("keepInventory", false); m.put("potions", true); m.put("doMobSpawning", true);
        m.put("gamemode", "SURVIVAL");
        m.put("difficulty", "NORMAL");
        return m;
    }

    private void ensureDir(File dir, String label) {
        if (dir.exists()) return;
        if (!dir.mkdirs()) {
            plugin.getLogger().warning("Could not create " + label + " at " + dir.getPath());
        }
    }

    private void ensureFile(File file, String label) {
        if (file.exists()) return;
        try {
            if (!file.createNewFile()) {
                plugin.getLogger().warning("Could not create " + label + " at " + file.getPath());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create " + label + " at " + file.getPath() + ": " + e.getMessage());
        }
    }

    private void safeDelete(File file, String label) {
        if (!file.exists()) return;
        if (!file.delete()) {
            plugin.getLogger().warning("Failed to delete " + label + " at " + file.getPath());
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void refreshActiveWorldsSettings() {
        for (World w : Bukkit.getWorlds()) {
            applyGameRules(w);
        }
    }

    // --- HELPER METHODS ---

    public boolean isAllWorld(String internalName) {
        String norm = internalName.replace("\\", "/");
        return norm.startsWith(BASE_ALL_WORLDS_PATH) || norm.contains("AltWorlds/all-worlds");
    }

    public boolean isLobbyWorld(String worldName) {
        String lobbyName = plugin.getConfig().getString("lobby.worldName", "lobby");
        String norm = worldName.replace("\\", "/");
        return norm.equals(lobbyName) || norm.endsWith("/" + lobbyName) || norm.equals(BASE_LOBBY_PATH);
    }

    public List<String> getAllWorldsList() {
        if (allWorldsSourceCfg == null) return new ArrayList<>();
        ConfigurationSection sec = allWorldsSourceCfg.getConfigurationSection("all-worlds");
        return sec == null ? new ArrayList<>() : new ArrayList<>(sec.getKeys(false));
    }

    // --- TELEPORT LOGIC ---

    public void teleportToAllWorld(Player p, String wName) {
        File folder = new File(plugin.getDataFolder(), "all-worlds/" + wName);
        if (!folder.exists()) {
            p.sendMessage("§cServer world not found.");
            return;
        }

        String internalPath = folder.getPath().replace("\\", "/");
        WorldHandle handle = worldManager.getHandle(folder.getName());
        handle.touch();

        World w = Bukkit.getWorld(internalPath);
        if (w == null) {
            p.sendMessage("§eLoading server world...");
            w = new WorldCreator(internalPath).createWorld();
        }

        if (w != null) {
            handle.touch();
            applyGameRules(w);
            Location target = w.getSpawnLocation().add(0.5, 0, 0.5);
            scheduleTeleport(p, target, wName, () -> {
                String gmStr = allWorldsSourceCfg.getString("all-worlds." + wName + ".settings.gamemode", "ADVENTURE");
                p.setGameMode(parseGameMode(gmStr, GameMode.ADVENTURE));
            });
        }
    }

    public void teleportToWorld(Player p, UUID owner, String worldName) {
        YamlConfiguration ownerConfig = getPlayerConfig(owner);
        if (!ownerConfig.contains("worlds." + worldName)) {
            p.sendMessage("§cWorld not found in owner's registry.");
            return;
        }

        if (isBanned(owner, worldName, p.getUniqueId()) && !p.hasPermission("altworlds.admin")) {
            p.sendMessage("§cYou are banned from this world.");
            return;
        }

        String ownerName = Bukkit.getOfflinePlayer(owner).getName();
        if (ownerName == null) ownerName = ownerConfig.getString("name", "Unknown");

        String userFolder = ownerName + "_" + owner;
        String internalPath = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + worldName;

        if (!new File(internalPath).exists()) {
            String legacy = ownerConfig.getString("worlds." + worldName + ".internal");
            if (legacy != null && new File(legacy).exists()) internalPath = legacy;
            else {
                p.sendMessage("§cError: World folder does not exist on disk.");
                return;
            }
        }

        final String finalInternalPath = internalPath;

        WorldHandle handle = worldManager.getHandle(new File(internalPath).getName());
        handle.touch();

        World w = Bukkit.getWorld(internalPath);
        if (w == null) {
            p.sendMessage("§eLoading world...");
            w = new WorldCreator(internalPath).createWorld();
        }

        if (w != null) {
            handle.touch();
            applyGameRules(w);
            Location targetLoc = null;

            Location savedLoc = getLastLocation(p.getUniqueId(), worldName, w);
            if (savedLoc != null && savedLoc.getY() > -64) targetLoc = savedLoc.add(0, 0.1, 0);
            if (targetLoc == null) targetLoc = w.getSpawnLocation().add(0.5, 0, 0.5);

            scheduleTeleport(p, targetLoc, worldName, () -> {
                Role role = getPlayerRole(owner, worldName, p.getUniqueId());
                GameMode targetGM;

                if (role == Role.VISITOR && !p.hasPermission("altworlds.admin")) {
                    targetGM = GameMode.SPECTATOR;
                } else {
                    targetGM = getSpecificPlayerGamemode(owner, worldName, p.getUniqueId());
                    if (targetGM == null) {
                        String def = "SURVIVAL";
                        if (getSetting(owner, worldName, "gamemode", false)) {
                            YamlConfiguration cfg = getUserSettingsConfig(new File(finalInternalPath).getParentFile());
                            def = cfg.getString("worlds." + worldName + ".settings.gamemode", "SURVIVAL");
                        }
                        targetGM = parseGameMode(def, GameMode.SURVIVAL);
                    }
                }
                p.setGameMode(targetGM);
            });
        }
    }

    public void teleportToOwnedWorld(Player p, String worldName) {
        teleportToWorld(p, p.getUniqueId(), worldName);
    }

    // --- SERIALIZACIÃ“N BASE64 ---

    @SuppressWarnings("deprecation")
    public String toBase64(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error saving inventory", e);
        }
    }

    @SuppressWarnings("deprecation")
    public ItemStack[] fromBase64(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    // --- INVENTARIOS ---

    private void savePlayerInventory(Player p, World w) {
        if (w == null) return;
        String invId = w.getUID().toString();
        YamlConfiguration config = getPlayerConfig(p.getUniqueId());
        String path = "inventories." + invId;
        config.set(path + ".content", toBase64(p.getInventory().getContents()));
        config.set(path + ".armor", toBase64(p.getInventory().getArmorContents()));
        config.set(path + ".xp", p.getExp());
        config.set(path + ".level", p.getLevel());
        config.set(path + ".health", p.getHealth());
        config.set(path + ".food", p.getFoodLevel());
        savePlayerConfig(p.getUniqueId());
    }

    private void loadPlayerInventory(Player p, World w) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setExp(0); p.setLevel(0); p.setHealth(20); p.setFoodLevel(20);

        if (w == null) return;
        String invId = w.getUID().toString();
        YamlConfiguration config = getPlayerConfig(p.getUniqueId());
        String path = "inventories." + invId;

        if (config.contains(path)) {
            String content = config.getString(path + ".content");
            String armor = config.getString(path + ".armor");
            if (content != null) p.getInventory().setContents(fromBase64(content));
            if (armor != null) p.getInventory().setArmorContents(fromBase64(armor));
            p.setExp((float) config.getDouble(path + ".xp"));
            p.setLevel(config.getInt(path + ".level"));
            double hp = config.getDouble(path + ".health");
            if (hp > 0) p.setHealth(hp);
            p.setFoodLevel(config.getInt(path + ".food"));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        savePlayerInventory(e.getPlayer(), e.getFrom());
        loadPlayerInventory(e.getPlayer(), e.getPlayer().getWorld());
        updatePlayerInventory(e.getPlayer());

        World to = e.getPlayer().getWorld();
        worldManager.getHandle(to.getName()).touch();
        applyGameRules(to);

        UUID owner = getOwnerUuidByInternalWorldName(to.getName());
        if (owner != null && !owner.equals(e.getPlayer().getUniqueId())) {
            incrementVisits(owner, getWorldNameByInternal(to.getName()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (pendingTeleports.containsKey(uuid)) {
            pendingTeleports.get(uuid).cancel();
            pendingTeleports.remove(uuid);
        }
        saveLastLocation(p, p.getLocation());
        savePlayerInventory(p, p.getWorld());
        unloadPlayerConfig(uuid);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld() != e.getTo().getWorld()) {
            saveLastLocation(e.getPlayer(), e.getFrom());
            if (e.getTo().getWorld() != null) {
                worldManager.getHandle(e.getTo().getWorld().getName()).touch();
            }
        }
    }

    // --- CARPETAS DE USUARIO ---

    public void syncPlayerFolders(Player p) {
        UUID uuid = p.getUniqueId();
        String name = p.getName();
        File worldsDir = new File(BASE_USER_WORLDS_PATH);
        File correctFolder = new File(worldsDir, name + "_" + uuid);
        ensureDir(correctFolder, "player folder");
        scanAndRegisterWorlds(uuid, name);
    }

    private void scanAndRegisterWorlds(UUID uuid, String name) {
        File playerFolder = new File(BASE_USER_WORLDS_PATH, name + "_" + uuid);
        if (!playerFolder.exists() || !playerFolder.isDirectory()) return;

        File[] contents = playerFolder.listFiles(File::isDirectory);
        if (contents == null) return;

        YamlConfiguration config = getPlayerConfig(uuid);
        boolean saved = false;

        for (File worldFolder : contents) {
            String wName = worldFolder.getName();
            if (wName.equals("region") || wName.equals("data") || wName.equals("poi")) continue;

            if (!hasWorld(uuid, wName)) {
                String internalPath = worldFolder.getPath().replace("\\", "/");
                ensureWorldSettingsEntry(playerFolder, wName, "survival", null, null);

                String wPath = "worlds." + wName;
                config.set(wPath + ".internal", internalPath);
                config.set(wPath + ".ownerGamemode", "SURVIVAL");
                config.set(wPath + ".creationType", "manual_import");
                config.set(wPath + ".created", System.currentTimeMillis());
                saved = true;
            }
        }
        if (saved) savePlayerConfig(uuid);
    }

    public YamlConfiguration getUserSettingsConfig(File userFolder) {
        File settingsFile = new File(userFolder, "world-settings.yml");
        String path = settingsFile.getAbsolutePath();
        if (userSettingsCache.containsKey(path)) return userSettingsCache.get(path);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(settingsFile);
        userSettingsCache.put(path, config);
        return config;
    }

    private void saveUserSettingsConfig(File userFolder, YamlConfiguration config) {
        File settingsFile = new File(userFolder, "world-settings.yml");
        try {
            config.save(settingsFile);
            userSettingsCache.put(settingsFile.getAbsolutePath(), config);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + settingsFile.getPath());
        }
    }

    private void ensureWorldSettingsEntry(File userFolder, String worldName, String creationType, String sourceKey, YamlConfiguration sourceYml) {
        YamlConfiguration settingsCfg = getUserSettingsConfig(userFolder);
        boolean changed = false;

        String basePath = "worlds." + worldName;
        Map<String, Object> finalValues = new HashMap<>();
        Map<String, Object> defaultMap = getDefaultSettingsMap();

        String configSection = creationType;
        if (creationType.startsWith("template:")) configSection = "templates";
        else if (creationType.startsWith("autoworld:")) configSection = "auto-worlds";
        else if (creationType.startsWith("privatetemplate:")) configSection = "private-worlds";
        else if (creationType.equals("vanilla")) configSection = "survival";

        ConfigurationSection defaults = plugin.getConfig().getConfigurationSection("world-defaults." + configSection);
        if (defaults != null) {
            for (String key : defaults.getKeys(false)) {
                if (defaultMap.containsKey(key)) {
                    defaultMap.put(key, defaults.get(key));
                }
            }
        }

        finalValues.put("name", "&a" + worldName);
        finalValues.put("icon", "GRASS_BLOCK");

        if (sourceKey != null && sourceYml != null) {
            String rootSec = null;
            if (sourceYml == templates.getConfig()) rootSec = "templates";
            else if (sourceYml == autoWorldsSourceCfg) rootSec = "auto-worlds";
            else if (sourceYml == privateWorldsSourceCfg) rootSec = "private-worlds";

            if (rootSec != null) {
                String fullPath = rootSec + "." + sourceKey;
                if (sourceYml.contains(fullPath)) {
                    if (sourceYml.contains(fullPath + ".name")) finalValues.put("name", sourceYml.getString(fullPath + ".name"));
                    if (sourceYml.contains(fullPath + ".icon")) finalValues.put("icon", sourceYml.getString(fullPath + ".icon"));

                    ConfigurationSection settingsSec = sourceYml.getConfigurationSection(fullPath + ".settings");
                    if (settingsSec != null) {
                        for (String k : settingsSec.getKeys(false)) {
                            defaultMap.put(k, settingsSec.get(k));
                        }
                    }
                }
            }
        }

        if (!settingsCfg.contains(basePath)) {
            settingsCfg.set(basePath + ".name", finalValues.get("name"));
            settingsCfg.set(basePath + ".icon", finalValues.get("icon"));
            for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
                settingsCfg.set(basePath + ".settings." + entry.getKey(), entry.getValue());
            }
            changed = true;
        }

        if (changed) {
            saveUserSettingsConfig(userFolder, settingsCfg);
        }
    }

    private void checkAutoWorlds(Player p) {
        if (!plugin.getConfig().getBoolean("auto-worlds.enabled", false)) return;
        boolean copyAlways = plugin.getConfig().getBoolean("auto-worlds.copyOnEveryJoin", false);

        YamlConfiguration config = getPlayerConfig(p.getUniqueId());
        boolean alreadyChecked = config.getBoolean("autoWorldsChecked", false);

        if (alreadyChecked && !copyAlways) return;
        if (autoWorldsSourceCfg == null) return;

        ConfigurationSection sec = autoWorldsSourceCfg.getConfigurationSection("auto-worlds");
        if (sec == null) return;

        boolean countLimit = plugin.getConfig().getBoolean("auto-worlds.countTowardsLimit", false);
        int max = getMaxWorldsForPlayer(p.getUniqueId());

        for (String key : sec.getKeys(false)) {
            if (!hasWorld(p.getUniqueId(), key)) {
                File sourceFolder = new File(plugin.getDataFolder(), "auto-worlds/" + key);
                if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
                    continue;
                }
                if (countLimit && !p.hasPermission("altworlds.admin") && max != -1) {
                    int current = listPlayerWorlds(p.getUniqueId()).size();
                    if (current >= max) {
                        continue;
                    }
                }
                Map<String, String> flags = new HashMap<>();
                createWorld(p, key, "autoworld:" + key, flags, true);
            }
        }
        config.set("autoWorldsChecked", true);
        savePlayerConfig(p.getUniqueId());
    }

    private void loadLobbyWorld() {
        if (Bukkit.getWorld(BASE_LOBBY_PATH) == null) {
            File f = new File(BASE_LOBBY_PATH);
            boolean createdNow = !f.exists() && plugin.getConfig().getBoolean("lobby.autoCreate", true);
            if (f.exists() || createdNow) {
                try {
                    WorldCreator wc = new WorldCreator(BASE_LOBBY_PATH);
                    if (createdNow) wc.generator(new VoidChunkGenerator());
                    World w = wc.createWorld();
                    if (w != null) {
                        ensureLobbySettingsFile();
                        applyGameRules(w);
                        if (createdNow) {
                            if (!w.setSpawnLocation(0, 65, 0)) {
                                plugin.getLogger().warning("Failed to set lobby spawn location.");
                            }
                            createSafePlatform(w, 0, 65, 0);
                        }
                        worldManager.getHandle(w.getName()).touch();
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not load lobby: " + e.getMessage());
                }
            }
        } else {
            ensureLobbySettingsFile();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        YamlConfiguration config = getPlayerConfig(p.getUniqueId(), p.getName());
        config.set("name", p.getName());
        config.set("lastLogin", System.currentTimeMillis());

        if (!config.contains("limits.maxWorlds")) {
            int def = plugin.getConfig().getInt("defaults.maxWorldsPerPlayer", 3);
            config.set("limits.maxWorlds", def);
        }
        savePlayerConfig(p.getUniqueId());

        syncPlayerFolders(p);
        checkAutoWorlds(p);
        checkHomeWorldIntegrity(p.getUniqueId());

        boolean forceSkip = plugin.getConfig().getBoolean("login.forceSkipLobby", false);
        boolean userWantsSkip = getPlayerSetting(p.getUniqueId(), "skipLobby", false);
        String homeWorld = getPlayerSettingStr(p.getUniqueId(), "homeWorld");

        if (homeWorld != null && (forceSkip || userWantsSkip)) {
            if (hasWorld(p.getUniqueId(), homeWorld)) {
                teleportToOwnedWorld(p, homeWorld);
                return;
            }
            List<String> serverWorlds = getAllWorldsList();
            if (serverWorlds.contains(homeWorld)) {
                teleportToAllWorld(p, homeWorld);
                return;
            }
        }
        forceInstantLobby(p);
    }

    private void checkHomeWorldIntegrity(UUID uuid) {
        String home = getPlayerSettingStr(uuid, "homeWorld");
        if (home == null) return;
        if (hasWorld(uuid, home)) return;
        if (getAllWorldsList().contains(home)) return;

        List<String> worlds = listPlayerWorlds(uuid);
        if (!worlds.isEmpty()) {
            setPlayerSetting(uuid, "homeWorld", worlds.getFirst());
        } else {
            setPlayerSetting(uuid, "homeWorld", null);
        }
    }

    public boolean getPlayerSetting(UUID uuid, String key, boolean def) {
        return getPlayerConfig(uuid).getBoolean("prefs." + key, def);
    }
    public String getPlayerSettingStr(UUID uuid, String key) {
        return getPlayerConfig(uuid).getString("prefs." + key);
    }
    public void setPlayerSetting(UUID uuid, String key, Object value) {
        YamlConfiguration config = getPlayerConfig(uuid);
        config.set("prefs." + key, value);
        savePlayerConfig(uuid);
    }

    private void forceInstantLobby(Player p) {
        World w = Bukkit.getWorld(BASE_LOBBY_PATH);
        if (w == null) w = Bukkit.getWorld("world");
        if (w == null) return;

        worldManager.getHandle(w.getName()).touch();
        Location target = w.getSpawnLocation().add(0.5, 0, 0.5);
        if (isUnsafe(target.getBlock())) createSafePlatform(w, target.getBlockX(), target.getBlockY(), target.getBlockZ());
        p.teleport(target);

        String mode = "ADVENTURE";
        if (lobbySettingsCfg == null) ensureLobbySettingsFile();
        if (lobbySettingsCfg != null) {
            mode = lobbySettingsCfg.getString("lobby.settings.gamemode", "ADVENTURE");
        }
        p.setGameMode(parseGameMode(mode, GameMode.ADVENTURE));

        updatePlayerInventory(p);
    }

    public void sendInvite(Player sender, String targetName) {
        String currentWorld = sender.getWorld().getName();

        UUID worldOwner = getOwnerUuidByInternalWorldName(currentWorld);
        String realWorldName = getWorldNameByInternal(currentWorld);

        if (worldOwner == null || realWorldName == null) {
            sender.sendMessage("§cYou must be in a player world to invite.");
            return;
        }

        Role senderRole = getPlayerRole(worldOwner, realWorldName, sender.getUniqueId());
        if (!senderRole.isAtLeast(Role.ADMIN) && !sender.hasPermission("altworlds.admin")) {
            sender.sendMessage("§cYou don't have permission to invite people to this world.");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found or offline.");
            return;
        }

        if (isMember(worldOwner, realWorldName, target.getUniqueId())) {
            sender.sendMessage("§cThat player is already a member.");
            return;
        }

        if (isBanned(worldOwner, realWorldName, target.getUniqueId())) {
            sender.sendMessage("§cThat player is banned from this world. Unban them first.");
            return;
        }

        if (plugin.getConfig().getBoolean("limits.invitedWorldsCount", false)) {
            int max = getMaxWorldsForPlayer(target.getUniqueId());
            int current = listPlayerWorlds(target.getUniqueId()).size() + listJoinedWorlds(target.getUniqueId()).size();
            if (max != -1 && current >= max) {
                sender.sendMessage("§c" + target.getName() + " has reached their max world limit (Invited worlds count).");
                return;
            }
        }

        pendingInvites.put(target.getUniqueId(), new WorldInvite(worldOwner, realWorldName));

        sender.sendMessage("§aInvite sent to " + target.getName() + " for world " + realWorldName);
        target.sendMessage("§eYou have been invited to world §b" + realWorldName + "§e by §f" + sender.getName());
        target.sendMessage("§eType §a/aw accept§e to join.");
    }

    public void acceptInvite(Player p) {
        if (!pendingInvites.containsKey(p.getUniqueId())) {
            p.sendMessage("§cYou have no pending invites.");
            return;
        }

        WorldInvite invite = pendingInvites.remove(p.getUniqueId());
        int timeout = plugin.getConfig().getInt("invitations.timeout", 60);
        if (invite.isExpired(timeout)) {
            p.sendMessage("§cInvitation expired.");
            return;
        }

        addMember(invite.getOwnerUuid(), invite.getWorldName(), p.getUniqueId(), Role.MEMBER);
        addJoinedWorldReference(p.getUniqueId(), invite.getOwnerUuid(), invite.getWorldName());

        p.sendMessage("§aYou joined " + invite.getWorldName() + "!");
        teleportToWorld(p, invite.getOwnerUuid(), invite.getWorldName());

        Player owner = Bukkit.getPlayer(invite.getOwnerUuid());
        if (owner != null) owner.sendMessage("§a" + p.getName() + " accepted your invite to " + invite.getWorldName());
    }

    public void addMember(UUID owner, String wName, UUID member, Role role) {
        YamlConfiguration ownerConfig = getPlayerConfig(owner);
        String path = "worlds." + wName + ".members." + member;
        ownerConfig.set(path + ".role", role.name());
        String defGm = plugin.getConfig().getString("defaults.memberGamemode", "SURVIVAL");
        ownerConfig.set(path + ".gamemode", defGm);
        savePlayerConfig(owner);
    }

    public void addJoinedWorldReference(UUID player, UUID owner, String wName) {
        YamlConfiguration conf = getPlayerConfig(player);
        List<String> joined = conf.getStringList("joined_worlds");
        String ref = owner + ":" + wName;
        if (!joined.contains(ref)) {
            joined.add(ref);
            conf.set("joined_worlds", joined);
            savePlayerConfig(player);
        }
    }

    public void removeJoinedWorldReference(UUID player, UUID owner, String wName) {
        YamlConfiguration conf = getPlayerConfig(player);
        List<String> joined = conf.getStringList("joined_worlds");
        String ref = owner + ":" + wName;
        if (joined.remove(ref)) {
            conf.set("joined_worlds", joined);
            savePlayerConfig(player);
        }
    }

    public void removeMember(UUID owner, String wName, UUID member) {
        YamlConfiguration ownerConfig = getPlayerConfig(owner);
        String path = "worlds." + wName + ".members." + member;
        ownerConfig.set(path, null);
        savePlayerConfig(owner);
        removeJoinedWorldReference(member, owner, wName);
    }

    public void kickPlayer(Player sender, String targetName) {
        UUID ownerUuid = getOwnerUuidByInternalWorldName(sender.getWorld().getName());
        String wName = getWorldNameByInternal(sender.getWorld().getName());

        if (ownerUuid == null || wName == null) {
            sender.sendMessage("§cInvalid world.");
            return;
        }

        Role senderRole = getPlayerRole(ownerUuid, wName, sender.getUniqueId());
        if (!senderRole.isAtLeast(Role.ADMIN) && !sender.hasPermission("altworlds.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        UUID targetUuid = findPlayerUuidByName(targetName);
        if (targetUuid == null) {
            sender.sendMessage("§cPlayer not found in database.");
            return;
        }

        if (targetUuid.equals(ownerUuid)) {
            sender.sendMessage("§cCannot kick the owner.");
            return;
        }

        Role targetRole = getPlayerRole(ownerUuid, wName, targetUuid);
        if (senderRole == Role.ADMIN && targetRole == Role.ADMIN) {
            sender.sendMessage("§cAdmins cannot kick other Admins.");
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.getWorld().equals(sender.getWorld())) {
            teleportToLobby(target);
            target.sendMessage("§cYou were kicked from this world.");
        }
        sender.sendMessage("§aKicked " + targetName);

        String home = getPlayerSettingStr(targetUuid, "homeWorld");
        if (home != null && home.equals(wName)) {
            setPlayerSetting(targetUuid, "homeWorld", null);
        }
    }

    public void banPlayer(Player sender, String targetName, int minutes) {
        long seconds = (minutes == -1) ? -1 : minutes * 60L;
        banPlayer(sender, targetName, seconds);
    }

    public void banPlayer(Player sender, String targetName, long seconds) {
        UUID ownerUuid = getOwnerUuidByInternalWorldName(sender.getWorld().getName());
        String wName = getWorldNameByInternal(sender.getWorld().getName());

        if (ownerUuid == null) { sender.sendMessage("§cNot a player world."); return; }

        Role senderRole = getPlayerRole(ownerUuid, wName, sender.getUniqueId());
        if (!senderRole.isAtLeast(Role.ADMIN) && !sender.hasPermission("altworlds.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        UUID targetUuid = findPlayerUuidByName(targetName);
        if (targetUuid == null) { sender.sendMessage("§cPlayer not found."); return; }
        banPlayer(sender, ownerUuid, wName, targetUuid, seconds);
    }

    public void banPlayer(Player sender, UUID ownerUuid, String wName, UUID targetUuid, long seconds) {
        if (targetUuid.equals(ownerUuid)) { sender.sendMessage("§cCannot ban owner."); return; }

        Role senderRole = getPlayerRole(ownerUuid, wName, sender.getUniqueId());
        if (!senderRole.isAtLeast(Role.ADMIN) && !sender.hasPermission("altworlds.admin")) {
            sender.sendMessage("§cNo permission.");
            return;
        }

        if (isBanned(ownerUuid, wName, targetUuid)) {
            sender.sendMessage("§cThat player is already banned.");
            return;
        }

        long unbanTime = (seconds == -1) ? -1 : System.currentTimeMillis() + (seconds * 1000L);
        YamlConfiguration ownerConfig = getPlayerConfig(ownerUuid);
        ownerConfig.set("worlds." + wName + ".banned." + targetUuid, unbanTime);
        savePlayerConfig(ownerUuid);

        // For temporary bans, keep membership so access is restored automatically after expiry.
        if (seconds == -1) {
            removeMember(ownerUuid, wName, targetUuid);
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.getWorld().equals(sender.getWorld())) {
            teleportToLobby(target);
            target.sendMessage("§cYou have been banned from this world.");
        }

        String display = Bukkit.getOfflinePlayer(targetUuid).getName();
        if (display == null) display = targetUuid.toString();
        String msg = (seconds == -1) ? "ever" : (seconds + "s");
        sender.sendMessage("§cBanned " + display + " for " + msg);
    }

    @SuppressWarnings("unused")
    public void unbanPlayer(Player sender, UUID targetUuid) {
        UUID ownerUuid = getOwnerUuidByInternalWorldName(sender.getWorld().getName());
        String wName = getWorldNameByInternal(sender.getWorld().getName());
        if(ownerUuid == null) return;

        unbanPlayer(ownerUuid, wName, targetUuid);
        sender.sendMessage("§aUnbanned.");
    }

    public void unbanPlayer(UUID ownerUuid, String wName, UUID targetUuid) {
        YamlConfiguration config = getPlayerConfig(ownerUuid);
        config.set("worlds." + wName + ".banned." + targetUuid, null);
        savePlayerConfig(ownerUuid);
    }

    public boolean isBanned(UUID owner, String wName, UUID target) {

        YamlConfiguration config = getPlayerConfig(owner);
        String path = "worlds." + wName + ".banned." + target;
        if (!config.contains(path)) return false;
        long time = config.getLong(path);
        if (time == -1) return true;
        if (System.currentTimeMillis() > time) {
            config.set(path, null);
            savePlayerConfig(owner);
            return false;
        }
        return true;
    }

    public void leaveWorld(Player p) {
        UUID ownerUuid = getOwnerUuidByInternalWorldName(p.getWorld().getName());
        String wName = getWorldNameByInternal(p.getWorld().getName());

        if (ownerUuid == null || ownerUuid.equals(p.getUniqueId())) {
            p.sendMessage("§cYou cannot leave your own world (Delete it instead).");
            return;
        }

        if (!isMember(ownerUuid, wName, p.getUniqueId())) {
            p.sendMessage("§cYou are not a member of this world.");
            return;
        }

        removeMember(ownerUuid, wName, p.getUniqueId());
        teleportToLobby(p);
        p.sendMessage("§aYou left " + wName + ".");

        String home = getPlayerSettingStr(p.getUniqueId(), "homeWorld");
        if (home != null && home.equals(wName)) {
            setPlayerSetting(p.getUniqueId(), "homeWorld", null);
        }
    }

    public Role getPlayerRole(UUID owner, String wName, UUID player) {
        if (owner.equals(player)) return Role.OWNER;
        if (plugin.getConfig().getBoolean("auto-worlds.enabled") && owner.equals(SERVER_UUID)) {
            return Role.VISITOR;
        }
        YamlConfiguration config = getPlayerConfig(owner);
        String roleStr = config.getString("worlds." + wName + ".members." + player + ".role");
        if (roleStr != null) {
            try {
                return Role.valueOf(roleStr);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid role '" + roleStr + "' for player " + player + " in world " + wName);
            }
        }
        return Role.VISITOR;
    }

    public List<String> listJoinedWorlds(UUID uuid) {
        YamlConfiguration conf = getPlayerConfig(uuid);
        return conf.getStringList("joined_worlds");
    }

    public void toggleLike(Player p, UUID owner, String wName) {
        YamlConfiguration config = getPlayerConfig(owner);
        String path = "worlds." + wName + ".stats.likes";
        List<String> likes = config.getStringList(path);
        String uuidStr = p.getUniqueId().toString();

        if (likes.contains(uuidStr)) {
            likes.remove(uuidStr);
            p.sendMessage("§eUnliked " + wName);
        } else {
            likes.add(uuidStr);
            p.sendMessage("§aLiked " + wName + "!");
        }
        config.set(path, likes);
        savePlayerConfig(owner);
    }

    public void renameWorld(Player p, String oldName, String newName) {
        UUID uuid = p.getUniqueId();

        if (isInvalidName(newName)) {
            p.sendMessage(plugin.getConfig().getString("restrictions.names.errorMessage", "§cInvalid name."));
        }

        File allWorldsDir = new File(plugin.getDataFolder(), "all-worlds");
        File potentialServerWorld = new File(allWorldsDir, oldName);

        if (potentialServerWorld.exists()) {
            if (!p.hasPermission("altworlds.admin")) {
                p.sendMessage("§cOnly admins can rename server worlds.");
                return;
            }
            if (new File(allWorldsDir, newName).exists()) {
                p.sendMessage("§cA server world with that name already exists.");
                return;
            }
            renameServerWorld(p, oldName, newName);
            return;
        }

        if (!plugin.getConfig().getBoolean("restrictions.allowRenaming", true) && !p.hasPermission("altworlds.admin")) {
            p.sendMessage("§cRenaming worlds is disabled.");
            return;
        }

        if (hasWorld(uuid, newName)) {
            p.sendMessage("§cYou already have a world with that name.");
            return;
        }
        if (!hasWorld(uuid, oldName)) {
            p.sendMessage("§cWorld not found.");
            return;
        }

        YamlConfiguration config = getPlayerConfig(uuid);
        String internalPath = config.getString("worlds." + oldName + ".internal");
        if (internalPath == null) {
            p.sendMessage("§cWorld not found.");
            return;
        }
        World w = Bukkit.getWorld(internalPath);

        if (w != null) {
            for (Player inside : w.getPlayers()) {
                inside.sendMessage("§eWorld is being renamed. Moving to lobby...");
                teleportToLobby(inside);
            }
            Bukkit.unloadWorld(w, true);
        }

        worldManager.removeHandle(new File(internalPath).getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File oldFolder = new File(internalPath);
            File parent = oldFolder.getParentFile();
            File newFolder = new File(parent, newName);

            if (oldFolder.renameTo(newFolder)) {
                String newInternal = newFolder.getPath().replace("\\", "/");
                String parentPath = parent.getPath();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    YamlConfiguration cfg = getPlayerConfig(uuid);
                    String oldSection = "worlds." + oldName;
                    String newSection = "worlds." + newName;
                    ConfigurationSection data = cfg.getConfigurationSection(oldSection);
                    if (data != null) {
                        cfg.set(newSection, data.getValues(true));
                        cfg.set(newSection + ".internal", newInternal);
                        cfg.set(oldSection, null);
                    } else {
                        plugin.getLogger().warning("Missing config section for world rename: " + oldSection);
                    }

                    File parentFolder = new File(parentPath);
                    YamlConfiguration userSettings = getUserSettingsConfig(parentFolder);
                    if (userSettings.contains("worlds." + oldName)) {
                        ConfigurationSection oldS = userSettings.getConfigurationSection("worlds." + oldName);
                        if (oldS != null) {
                            userSettings.set("worlds." + newName, oldS.getValues(true));
                            userSettings.set("worlds." + newName + ".name", "&a" + newName);
                        }
                        userSettings.set("worlds." + oldName, null);
                        saveUserSettingsConfig(parentFolder, userSettings);
                    }

                    String home = getPlayerSettingStr(uuid, "homeWorld");
                    if (home != null && home.equals(oldName)) {
                        setPlayerSetting(uuid, "homeWorld", newName);
                    }

                    savePlayerConfig(uuid);
                    p.sendMessage("§aWorld renamed to " + newName + "!");
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage("§cFailed to rename folder. OS lock?"));
            }
        });
    }

    private void renameServerWorld(Player p, String oldName, String newName) {
        File allWorldsDir = new File(plugin.getDataFolder(), "all-worlds");
        File oldFolder = new File(allWorldsDir, oldName);
        File newFolder = new File(allWorldsDir, newName);

        String internalPath = oldFolder.getPath().replace("\\", "/");
        World w = Bukkit.getWorld(internalPath);
        if (w != null) {
            for (Player inside : w.getPlayers()) {
                inside.sendMessage("§eServer world is being renamed. Moving to lobby...");
                teleportToLobby(inside);
            }
            Bukkit.unloadWorld(w, true);
        }

        worldManager.removeHandle(oldFolder.getName());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (oldFolder.renameTo(newFolder)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (allWorldsSourceCfg.contains("all-worlds." + oldName)) {
                        ConfigurationSection oldSection = allWorldsSourceCfg.getConfigurationSection("all-worlds." + oldName);
                        ConfigurationSection newSection = allWorldsSourceCfg.createSection("all-worlds." + newName);

                        if (oldSection != null) {
                            for (String key : oldSection.getKeys(false)) {
                                newSection.set(key, oldSection.get(key));
                            }
                        }

                        newSection.set("name", "&b" + capitalize(newName));
                        allWorldsSourceCfg.set("all-worlds." + oldName, null);

                        try {
                            allWorldsSourceCfg.save(new File(settingsDataFolder, "all-worlds.yml"));
                        } catch (IOException e) {
                            plugin.getLogger().warning("Failed to save all-worlds.yml after rename: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().warning("Missing all-worlds section for rename: " + oldName);
                    }

                    p.sendMessage("§aServer World renamed to " + newName + "!");
                });
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage("§cFailed to rename server folder."));
            }
        });
    }

    private boolean isInvalidName(String name) {
        int min = plugin.getConfig().getInt("restrictions.names.minLength", 3);
        int max = plugin.getConfig().getInt("restrictions.names.maxLength", 16);
        String regex = plugin.getConfig().getString("restrictions.names.regex", "^[a-zA-Z0-9_-]+$");
        if (name.length() < min || name.length() > max) return true;
        return !Pattern.matches(regex, name);
    }

    public void setWorldBorder(Player p, double radius, Location center) {
        World w = p.getWorld();
        if (!(isOwner(p, w.getName()) || p.hasPermission("altworlds.admin"))) {
            p.sendMessage("§cYou are not the owner of this world.");
            return;
        }

        // Si no se especifica centro, usar la ubicaciÃ³n del jugador
        Location finalCenter = (center != null) ? center : p.getLocation();

        w.getWorldBorder().setCenter(finalCenter);
        w.getWorldBorder().setSize(radius * 2);
        p.sendMessage("§aWorld border set to radius: " + radius + " centered at X:" + finalCenter.getBlockX() + " Z:" + finalCenter.getBlockZ());
    }

    public void removeWorldBorder(Player p) {
        World w = p.getWorld();
        if (isOwner(p, w.getName()) || p.hasPermission("altworlds.admin")) {
            w.getWorldBorder().reset();
            p.sendMessage("§aWorld border removed.");
            return;
        }
    }

    private void scheduleTeleport(Player p, Location target, String destName, Runnable postTpAction) {
        int delay = plugin.getConfig().getInt("teleport.delay", 3);
        String targetWorldName = (target.getWorld() != null) ? target.getWorld().getName() : destName;

        WorldHandle handle = worldManager.getHandle(targetWorldName);
        handle.addLock();

        if (delay <= 0) {
            performTeleport(p, target, destName, postTpAction, handle);
            return;
        }

        if (pendingTeleports.containsKey(p.getUniqueId())) {
            p.sendMessage("§cYou already have a pending teleport!");
            handle.removeLock();
            return;
        }

        p.sendMessage("§eTeleporting to §f" + destName + "§e in " + delay + " seconds. §cDo not move.");

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(p.getUniqueId());
            performTeleport(p, target, destName, postTpAction, handle);
        }, delay * 20L);

        pendingTeleports.put(p.getUniqueId(), task);
    }

    private void performTeleport(Player p, Location target, String destName, Runnable postTpAction, WorldHandle handle) {
        try {
            String internalWorldName = handle.getName();
            World currentWorld = Bukkit.getWorld(internalWorldName);
            if (currentWorld == null) {
                currentWorld = new WorldCreator(internalWorldName).createWorld();
            }
            if (currentWorld != null) {
                target.setWorld(currentWorld);
            } else {
                p.sendMessage("§cError: Could not load destination world.");
                return;
            }

            if (!isLobbyWorld(p.getWorld().getName())) saveLastLocation(p, p.getLocation());
            if (!target.getChunk().isLoaded()) target.getChunk().load(true);

            if (isUnsafe(target.getBlock())) {
                if (target.getY() < 0) target.setY(65);
                createSafePlatform(target.getWorld(), target.getBlockX(), target.getBlockY(), target.getBlockZ());
                p.sendMessage("§e[AltWorlds] §7Unsafe destination detected. Platform created.");
            }

            p.teleport(target);
            p.sendMessage("§aTeleported to " + destName);
            handle.touch();
            if (postTpAction != null) postTpAction.run();
        } finally {
            handle.removeLock();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (pendingTeleports.containsKey(e.getPlayer().getUniqueId())) {
            if (e.getFrom().getBlockX() != e.getTo().getBlockX() ||
                    e.getFrom().getBlockY() != e.getTo().getBlockY() ||
                    e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
                if (plugin.getConfig().getBoolean("teleport.cancelOnMove", true)) {
                    BukkitTask task = pendingTeleports.remove(e.getPlayer().getUniqueId());
                    if (task != null) task.cancel();
                    e.getPlayer().sendMessage("§cTeleport cancelled because you moved.");
                }
            }
        }
    }

    public boolean teleportToLobby(Player p) {
        if (!isLobbyWorld(p.getWorld().getName())) saveLastLocation(p, p.getLocation());
        World w = Bukkit.getWorld(BASE_LOBBY_PATH);
        if (w == null) w = Bukkit.getWorld("world");

        if (w != null) {
            applyGameRules(w);
            Location target = w.getSpawnLocation().add(0.5, 0, 0.5);

            scheduleTeleport(p, target, "Lobby", () -> {
                String mode = "ADVENTURE";
                if (lobbySettingsCfg == null) ensureLobbySettingsFile();
                if (lobbySettingsCfg != null) {
                    mode = lobbySettingsCfg.getString("lobby.settings.gamemode", "ADVENTURE");
                }
                p.setGameMode(parseGameMode(mode, GameMode.ADVENTURE));
                updatePlayerInventory(p);
            });
            return true;
        }
        return false;
    }

    public Map<UUID, String> getKnownPlayers() {
        Map<UUID, String> map = new HashMap<>();
        File[] files = playersDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName().replace(".yml", "");
                // Formato esperado: Nombre-UUID
                if (fileName.contains("-")) {
                    try {
                        // Buscar el Ãºltimo guion para separar UUID (ya que el UUID tiene guiones)
                        // Estrategia: Un UUID tiene 36 caracteres.
                        if (fileName.length() > 36) {
                            String uuidPart = fileName.substring(fileName.length() - 36);
                            String namePart = fileName.substring(0, fileName.length() - 37); // -36 y el guion (-1)
                            map.put(UUID.fromString(uuidPart), namePart);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not parse player file: " + fileName);
                    }
                }
            }
        }
        return map;
    }

    public int getMaxWorldsForPlayer(UUID uuid) {
        YamlConfiguration config = getPlayerConfig(uuid);
        if (config.contains("limits.maxWorlds")) {
            return config.getInt("limits.maxWorlds");
        }
        return plugin.getConfig().getInt("defaults.maxWorldsPerPlayer", 3);
    }

    public void setMaxWorldsForPlayer(UUID uuid, int max) {
        YamlConfiguration config = getPlayerConfig(uuid);
        config.set("limits.maxWorlds", max);
        savePlayerConfig(uuid);
    }

    public void createWorld(Player owner, String worldName, String type, Map<String, String> flags) {
        createWorld(owner, worldName, type, flags, false);
    }

    public void createWorld(Player owner, String worldName, String type, Map<String, String> flags, boolean silent) {
        UUID uuid = owner.getUniqueId();
        if (!silent && !owner.hasPermission("altworlds.admin")) {
            int max = getMaxWorldsForPlayer(uuid);
            int current = listPlayerWorlds(uuid).size();

            if (max != -1 && current >= max) {
                owner.sendMessage("§cYou have reached the maximum number of worlds (" + max + ").");
                return;
            }
        }
        if (hasWorld(uuid, worldName)) {
            if(!silent) owner.sendMessage("§cYou already have a world with that name.");
            return;
        }
        if (isInvalidName(worldName)) {
            if(!silent) owner.sendMessage(plugin.getConfig().getString("restrictions.names.errorMessage", "§cInvalid name."));
            return;
        }

        String configType = "survival";
        if (type.equals("vanilla")) {
            if ("CREATIVE".equalsIgnoreCase(flags.get("gm"))) configType = "creative";
            else configType = "survival";
        } else if (type.startsWith("autoworld:")) configType = "auto-worlds";
        else if (type.startsWith("privatetemplate:")) configType = "private-worlds";
        else if (type.startsWith("template:")) configType = "templates";
        else if (type.equals("creative")) configType = "creative";

        String defGm = plugin.getConfig().getString("world-defaults." + configType + ".gamemode", "SURVIVAL");
        if (flags.containsKey("gm") && !flags.get("gm").equals("SURVIVAL")) {
            defGm = flags.get("gm");
        }

        GameMode gm = GameMode.SURVIVAL;
        try {
            gm = GameMode.valueOf(defGm);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid gamemode '" + defGm + "', defaulting to SURVIVAL.");
        }

        String genType = flags.getOrDefault("generator", "NORMAL");

        String userFolder = owner.getName() + "_" + uuid;
        String finalInternalName = BASE_USER_WORLDS_PATH + "/" + userFolder + "/" + worldName;

        File tplSource = null;
        String tplKey = null;
        YamlConfiguration sourceYml = null;

        if (type.startsWith("autoworld:")) {
            tplKey = type.substring(10);
            tplSource = new File(plugin.getDataFolder(), "auto-worlds/" + tplKey);
            sourceYml = autoWorldsSourceCfg;
        } else if (type.startsWith("privatetemplate:")) {
            tplKey = type.substring(16);
            tplSource = new File(plugin.getDataFolder(), "private-worlds/" + tplKey);
            sourceYml = privateWorldsSourceCfg;
        } else if (type.startsWith("template:")) {
            tplKey = type.substring(9);
            tplSource = templates.resolveTemplateFolder(plugin, tplKey);
            sourceYml = templates.getConfig();
        }

        final boolean isVanilla = type.equalsIgnoreCase("vanilla") || type.equals("creative");
        final GameMode finalGm = gm;
        final String finalGen = genType;
        final File finalTplSource = tplSource;
        final String finalTplKey = tplKey;
        final YamlConfiguration finalSourceYml = sourceYml;

        if(!silent) owner.sendMessage("§eGenerating world §f" + worldName + "§e... please wait.");

        WorldHandle handle = worldManager.getHandle(finalInternalName);
        handle.addLock();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File targetFolder = new File(finalInternalName);
            boolean templateCopied = false;

            if (!isVanilla && finalTplSource != null && finalTplSource.exists()) {
                try {
                    copyDirectory(finalTplSource.toPath(), targetFolder.toPath());
                    safeDelete(new File(targetFolder, "uid.dat"), "uid.dat");
                    safeDelete(new File(targetFolder, "session.lock"), "session.lock");
                    safeDelete(new File(targetFolder, "settings.yml"), "settings.yml");
                    safeDelete(new File(targetFolder, "world-settings.yml"), "world-settings.yml");
                    templateCopied = true;
                } catch (IOException ex) {
                    plugin.getLogger().warning("Failed to copy template to " + targetFolder.getPath() + ": " + ex.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if(!silent) owner.sendMessage("§cTemplate Copy Error: " + ex.getMessage());
                        handle.removeLock();
                    });
                    return;
                }
            }

            final boolean wasTemplate = templateCopied;

            Bukkit.getScheduler().runTask(plugin, () -> {
                ensureWorldSettingsEntry(targetFolder.getParentFile(), worldName, type, finalTplKey, finalSourceYml);
                try {
                    WorldCreator wc = new WorldCreator(finalInternalName);
                    if (!wasTemplate) {
                        if (finalGen.equalsIgnoreCase("FLAT")) {
                            wc.type(WorldType.FLAT);
                            wc.generateStructures(false);
                        }
                        else if (finalGen.equalsIgnoreCase("VOID")) {
                            wc.generator(new VoidChunkGenerator());
                        }
                        else {
                            wc.type(WorldType.NORMAL);
                        }
                    }

                    World w = wc.createWorld();
                    if (w != null) {
                        w.setGameRule(GameRule.SPAWN_RADIUS, 0);

                        if (!wasTemplate) {
                            if (finalGen.equalsIgnoreCase("VOID")) {
                                w.getBlockAt(0, 64, 0).setType(Material.BEDROCK);
                                if (!w.setSpawnLocation(0, 65, 0)) {
                                    plugin.getLogger().warning("Failed to set spawn location for " + w.getName());
                                }
                            } else {
                                adjustSpawnToSafeLocation(w);
                            }
                        } else {
                            if (isUnsafe(w.getSpawnLocation().getBlock())) createSafePlatform(w, w.getSpawnLocation().getBlockX(), w.getSpawnLocation().getBlockY(), w.getSpawnLocation().getBlockZ());
                        }

                        registerWorld(uuid, worldName, finalInternalName, finalGm.name(), wasTemplate, type, finalGen);

                        applyGameRules(w);

                        if (!silent) {
                            performTeleport(owner, w.getSpawnLocation().add(0.5, 0, 0.5), worldName, () -> owner.setGameMode(finalGm), handle);
                            owner.sendMessage("§aWorld created successfully!");
                        } else {
                            plugin.getLogger().info("Auto-created world " + worldName + " for " + owner.getName());
                            handle.removeLock();
                        }
                    } else {
                        if(!silent) owner.sendMessage("§cCreation failed.");
                        handle.removeLock();
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("World creation failed for " + worldName + ": " + ex.getMessage());
                    handle.removeLock();
                }
            });
        });
    }

    public void regenerateWorld(Player owner, String worldName) {
        UUID uuid = owner.getUniqueId();
        if (!hasWorld(uuid, worldName)) return;

        // 1. Obtener datos actuales antes de borrar nada
        YamlConfiguration config = getPlayerConfig(uuid);
        String path = "worlds." + worldName;
        String type = config.getString(path + ".creationType", "vanilla");
        String gen = config.getString(path + ".generator", "NORMAL");
        String internalPath = config.getString(path + ".internal");
        if (internalPath == null) {
            owner.sendMessage("§cWorld not found.");
            return;
        }

        // Validar permisos si es auto-world...
        if (type.startsWith("autoworld:") && !plugin.getConfig().getBoolean("auto-worlds.permissions.allowRegen", false) && !owner.hasPermission("altworlds.admin")) {
            owner.sendMessage("§cYou cannot regenerate this auto-world.");
            return;
        }

        // 2. Sacar jugadores del mundo y descargar
        World w = Bukkit.getWorld(internalPath);
        if (w != null) {
            for (Player p : w.getPlayers()) forceInstantLobby(p);
            Bukkit.unloadWorld(w, false);
        }

        // 3. Borrar solo los ARCHIVOS del mundo (regiones, datos), NO la config del jugador
        worldManager.removeHandle(new File(internalPath).getName());
        File worldFolder = new File(internalPath);

        // Borramos el contenido de la carpeta, excepto backups si hubiese (borramos todo para regenerar limpio)
        try {
            deleteDirectory(worldFolder.toPath());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete world folder " + worldFolder.getPath() + ": " + e.getMessage());
        }

        owner.sendMessage("§eRegenerating files... please wait.");

        // 4. Volver a crear usando la lÃ³gica de createWorld pero SIN registrar de nuevo en config (ya existe)
        // Usamos un pequeÃ±o truco: llamamos a createWorld? NO, porque createWorld falla si existe en config.
        // Tenemos que duplicar la lÃ³gica de copia/generaciÃ³n aquÃ­ o borrar temporalmente la key "worlds.X"
        // Para mantener a los miembros, es mejor NO tocar la config y regenerar a mano.

        // OPCIÃ“N SEGURA: Reutilizar lÃ³gica de creaciÃ³n
        // Resolver template source en el hilo principal para evitar acceso async a configs
        File tplSource = null;
        if (!type.equals("vanilla") && !type.equals("creative")) {
            if (type.startsWith("template:")) tplSource = templates.resolveTemplateFolder(plugin, type.substring(9));
            else if (type.startsWith("autoworld:")) tplSource = new File(plugin.getDataFolder(), "auto-worlds/" + type.substring(10));
            else if (type.startsWith("privatetemplate:")) tplSource = new File(plugin.getDataFolder(), "private-worlds/" + type.substring(16));
        }
        final File finalTplSource = tplSource;

        // Hacemos el proceso de generaciÃ³n asÃ­ncrono manual
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            // Re-copiar template si es necesario
            if (!type.equals("vanilla") && !type.equals("creative")) {
                if (finalTplSource != null && finalTplSource.exists()) {
                    try {
                        copyDirectory(finalTplSource.toPath(), worldFolder.toPath());
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to copy template to " + worldFolder.getPath() + ": " + e.getMessage());
                    }
                    // Borrar uid.dat para evitar conflictos
                    safeDelete(new File(worldFolder, "uid.dat"), "uid.dat");
                }
            }

            // Volver al hilo principal para cargar
            Bukkit.getScheduler().runTask(plugin, () -> {
                WorldCreator wc = new WorldCreator(internalPath.replace("\\", "/"));
                if (gen.equalsIgnoreCase("FLAT")) wc.type(WorldType.FLAT);
                else if (gen.equalsIgnoreCase("VOID")) wc.generator(new VoidChunkGenerator());

                World newWorld = wc.createWorld();
                if (newWorld != null) {
                    if (gen.equalsIgnoreCase("VOID")) {
                        newWorld.getBlockAt(0, 64, 0).setType(Material.BEDROCK);
                        if (!newWorld.setSpawnLocation(0, 65, 0)) {
                            plugin.getLogger().warning("Failed to set spawn location for " + newWorld.getName());
                        }
                    } else if (type.equals("vanilla") || type.equals("creative")) {
                        adjustSpawnToSafeLocation(newWorld);
                    }
                    applyGameRules(newWorld);
                    owner.sendMessage("§aWorld regenerated!");
                    teleportToOwnedWorld(owner, worldName);
                } else {
                    owner.sendMessage("§cError regenerating world.");
                }
            });
        });
    }

    public void cloneWorld(Player actor, UUID ownerUuid, String sourceWorldName, String newWorldName) {
        if (!actor.hasPermission("altworlds.admin")) { actor.sendMessage("Â§cNo permission."); return; }
        if (ownerUuid == null || sourceWorldName == null) { actor.sendMessage("Â§cInvalid world."); return; }
        if (isLobbyWorld(sourceWorldName)) { actor.sendMessage("Â§cLobby world cannot be cloned."); return; }

        if (isInvalidName(newWorldName)) {
            actor.sendMessage(plugin.getConfig().getString("restrictions.names.errorMessage", "§cInvalid name."));
            return;
        }

        // --- SERVER WORLDS ---
        if (ownerUuid.equals(SERVER_UUID)) {
            if (allWorldsSourceCfg == null) { actor.sendMessage("Â§cServer worlds config not loaded."); return; }
            if (!allWorldsSourceCfg.contains("all-worlds." + sourceWorldName)) {
                actor.sendMessage("Â§cServer world not found.");
                return;
            }
            if (allWorldsSourceCfg.contains("all-worlds." + newWorldName)) {
                actor.sendMessage("Â§cA server world with that name already exists.");
                return;
            }

            File sourceFolder = new File(plugin.getDataFolder(), "all-worlds/" + sourceWorldName);
            File targetFolder = new File(plugin.getDataFolder(), "all-worlds/" + newWorldName);
            if (!sourceFolder.exists()) { actor.sendMessage("Â§cWorld folder not found."); return; }
            if (targetFolder.exists()) { actor.sendMessage("Â§cTarget folder already exists."); return; }

            World w = Bukkit.getWorld(sourceFolder.getPath().replace("\\", "/"));
            if (w != null) w.save();

            actor.sendMessage("Â§eCloning server world... please wait.");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    copyDirectory(sourceFolder.toPath(), targetFolder.toPath());
                    safeDelete(new File(targetFolder, "uid.dat"), "uid.dat");
                    safeDelete(new File(targetFolder, "session.lock"), "session.lock");
                    safeDelete(new File(targetFolder, "settings.yml"), "settings.yml");
                    safeDelete(new File(targetFolder, "world-settings.yml"), "world-settings.yml");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to clone server world folder " + sourceFolder.getPath() + " to " + targetFolder.getPath() + ": " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> actor.sendMessage("Â§cClone failed: " + e.getMessage()));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    ConfigurationSection oldSection = allWorldsSourceCfg.getConfigurationSection("all-worlds." + sourceWorldName);
                    if (oldSection != null) {
                        ConfigurationSection newSection = allWorldsSourceCfg.createSection("all-worlds." + newWorldName);
                        for (String key : oldSection.getKeys(false)) {
                            newSection.set(key, oldSection.get(key));
                        }
                        if (newSection.contains("name")) newSection.set("name", "&a" + newWorldName);
                        try {
                            allWorldsSourceCfg.save(new File(settingsDataFolder, "all-worlds.yml"));
                        } catch (IOException e) {
                            plugin.getLogger().warning("Failed to save all-worlds.yml after clone: " + e.getMessage());
                        }
                    } else {
                        plugin.getLogger().warning("Missing all-worlds section for clone: " + sourceWorldName);
                    }

                    actor.sendMessage("Â§aServer world cloned as " + newWorldName + "!");
                });
            });
            return;
        }

        // --- PLAYER WORLDS ---
        YamlConfiguration ownerConfig = getPlayerConfig(ownerUuid);
        if (!ownerConfig.contains("worlds." + sourceWorldName)) { actor.sendMessage("Â§cWorld not found."); return; }
        if (ownerConfig.contains("worlds." + newWorldName)) { actor.sendMessage("Â§cYou already have a world with that name."); return; }

        String sourceInternal = ownerConfig.getString("worlds." + sourceWorldName + ".internal");
        if (sourceInternal == null) { actor.sendMessage("Â§cWorld not found."); return; }

        File sourceFolder = new File(sourceInternal);
        if (!sourceFolder.exists()) { actor.sendMessage("Â§cWorld folder not found."); return; }

        File parent = sourceFolder.getParentFile();
        File targetFolder = new File(parent, newWorldName);
        if (targetFolder.exists()) { actor.sendMessage("Â§cTarget folder already exists."); return; }

        World w = Bukkit.getWorld(sourceInternal);
        if (w != null) w.save();

        actor.sendMessage("Â§eCloning world... please wait.");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                copyDirectory(sourceFolder.toPath(), targetFolder.toPath());
                safeDelete(new File(targetFolder, "uid.dat"), "uid.dat");
                safeDelete(new File(targetFolder, "session.lock"), "session.lock");
                safeDelete(new File(targetFolder, "settings.yml"), "settings.yml");
                safeDelete(new File(targetFolder, "world-settings.yml"), "world-settings.yml");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clone world folder " + sourceFolder.getPath() + " to " + targetFolder.getPath() + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> actor.sendMessage("Â§cClone failed: " + e.getMessage()));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                YamlConfiguration cfg = getPlayerConfig(ownerUuid);
                String oldSection = "worlds." + sourceWorldName;
                String newSection = "worlds." + newWorldName;

                ConfigurationSection data = cfg.getConfigurationSection(oldSection);
                if (data != null) {
                    cfg.set(newSection, data.getValues(true));
                    cfg.set(newSection + ".internal", targetFolder.getPath().replace("\\", "/"));
                    cfg.set(newSection + ".created", System.currentTimeMillis());
                } else {
                    plugin.getLogger().warning("Missing config section for world clone: " + oldSection);
                }
                savePlayerConfig(ownerUuid);

                YamlConfiguration userSettings = getUserSettingsConfig(parent);
                if (userSettings.contains(oldSection)) {
                    ConfigurationSection oldS = userSettings.getConfigurationSection(oldSection);
                    if (oldS != null) {
                        userSettings.set(newSection, oldS.getValues(true));
                        userSettings.set(newSection + ".name", "&a" + newWorldName);
                    }
                    saveUserSettingsConfig(parent, userSettings);
                }

                Set<UUID> members = getWorldMembers(ownerUuid, sourceWorldName);
                for (UUID memberUuid : members) {
                    addJoinedWorldReference(memberUuid, ownerUuid, newWorldName);
                }

                actor.sendMessage("Â§aWorld cloned as " + newWorldName + "!");
            });
        });
    }

    public void deleteWorld(Player actor, String targetWorldName) {
        UUID ownerUuid = actor.getUniqueId();

        String currentInternal = actor.getWorld().getName();
        if (isAllWorld(currentInternal)) {
            if (allWorldsSourceCfg.contains("all-worlds." + targetWorldName)) {
                actor.sendMessage("§cServer worlds cannot be deleted via command.");
                return;
            }
        }

        if (actor.hasPermission("altworlds.admin")) {
            String currentReal = getWorldNameByInternal(currentInternal);
            if (currentReal != null && currentReal.equals(targetWorldName)) {
                ownerUuid = getOwnerUuidByInternalWorldName(currentInternal);
            }
        }
        if (!hasWorld(ownerUuid, targetWorldName)) return;

        Set<UUID> members = getWorldMembers(ownerUuid, targetWorldName);
        for (UUID memberUuid : members) {
            removeJoinedWorldReference(memberUuid, ownerUuid, targetWorldName);
        }

        YamlConfiguration config = getPlayerConfig(ownerUuid);
        String path = "worlds." + targetWorldName;
        String type = config.getString(path + ".creationType", "vanilla");

        if (type.startsWith("autoworld:") || type.startsWith("auto-worlds")) {
            if (!plugin.getConfig().getBoolean("auto-worlds.permissions.allowDelete", false) && !actor.hasPermission("altworlds.admin")) {
                actor.sendMessage("§cYou cannot delete this auto-world.");
                return;
            }
        }

        String currentHome = getPlayerSettingStr(ownerUuid, "homeWorld");
        if (currentHome != null && currentHome.equals(targetWorldName)) {
            List<String> remaining = listPlayerWorlds(ownerUuid);
            remaining.remove(targetWorldName);
            if (!remaining.isEmpty()) {
                setPlayerSetting(ownerUuid, "homeWorld", remaining.getFirst());
            } else {
                setPlayerSetting(ownerUuid, "homeWorld", null);
            }
        }

        String internal = config.getString(path + ".internal");
        if (internal == null) {
            actor.sendMessage("§cWorld not found.");
            return;
        }
        World w = Bukkit.getWorld(internal);
        if (w != null) {
            for (Player p : w.getPlayers()) forceInstantLobby(p);
            Bukkit.unloadWorld(w, false);
        }

        File folder = new File(internal);
        YamlConfiguration settings = getUserSettingsConfig(folder.getParentFile());
        settings.set("worlds." + targetWorldName, null);
        saveUserSettingsConfig(folder.getParentFile(), settings);

        try {
            deleteDirectory(folder.toPath());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete world folder " + folder.getPath() + ": " + e.getMessage());
        }
        config.set("worlds." + targetWorldName, null);
        savePlayerConfig(ownerUuid);
    }

    private void registerWorld(UUID uuid, String wName, String internal, String gm, boolean isTemplate, String type, String gen) {
        YamlConfiguration config = getPlayerConfig(uuid);
        String wPath = "worlds." + wName;
        config.set(wPath + ".internal", internal.replace("\\", "/"));
        config.set(wPath + ".ownerGamemode", gm);
        config.set(wPath + ".isTemplate", isTemplate);
        config.set(wPath + ".creationType", type);
        config.set(wPath + ".generator", gen);
        config.set(wPath + ".stats.visits", 0);
        config.set(wPath + ".stats.likes", new ArrayList<String>());
        config.set(wPath + ".created", System.currentTimeMillis());
        savePlayerConfig(uuid);
    }

    public void applyGameRules(World world) {
        UUID owner = getOwnerUuidByInternalWorldName(world.getName());
        String realName;

        if (isLobbyWorld(world.getName())) {
            owner = SERVER_UUID;
            realName = world.getName();
        } else if (owner == null && isAllWorld(world.getName())) {
            owner = SERVER_UUID;
            File f = new File(world.getName());
            realName = f.getName();
        } else {
            realName = getWorldNameByInternal(world.getName());
        }

        if (owner == null || realName == null) return;

        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        for (WorldSettings s : WorldSettings.values()) {
            if (s.getCategory() == WorldSettings.Category.GAMERULE && s.getGameRule() != null) {
                boolean val = getSetting(owner, realName, s.getKey(), true);
                world.setGameRule(s.getGameRule(), val);
            }
            if (s == WorldSettings.CROP_GROWTH) {
                boolean grow = getSetting(owner, realName, s.getKey(), true);
                world.setGameRule(GameRule.RANDOM_TICK_SPEED, grow ? 3 : 0);
            }
        }
        world.setGameRule(GameRule.DO_TILE_DROPS, true);

        String diffStr = getSettingString(owner, realName, "difficulty", "NORMAL");
        Difficulty diff = parseDifficulty(diffStr, Difficulty.NORMAL);
        world.setDifficulty(diff);
    }

    @SuppressWarnings("unused")
    public void addLike(Player p, UUID owner, String wName) {
        YamlConfiguration config = getPlayerConfig(owner);
        String path = "worlds." + wName + ".stats.likes";
        List<String> likes = config.getStringList(path);
        if (likes.contains(p.getUniqueId().toString())) { p.sendMessage("§cYou already liked this world!"); return; }
        likes.add(p.getUniqueId().toString()); config.set(path, likes); savePlayerConfig(owner); p.sendMessage("§aYou liked " + wName + "!");
    }
    @SuppressWarnings("unused")
    public int getLikes(UUID owner, String wName) { return getPlayerConfig(owner).getStringList("worlds." + wName + ".stats.likes").size(); }
    public void incrementVisits(UUID owner, String wName) { YamlConfiguration c = getPlayerConfig(owner); String path = "worlds." + wName + ".stats.visits"; int v = c.getInt(path, 0); c.set(path, v + 1); savePlayerConfig(owner); }
    public int getVisits(UUID owner, String wName) { return getPlayerConfig(owner).getInt("worlds." + wName + ".stats.visits", 0); }

    public static class WorldInfo {
        public String wName;
        public String ownerName;
        public UUID ownerUuid;
        public int likes;
        public int visits;
        public int currentPlayers;
        public WorldInfo(String w, String o, UUID u, int l, int v, int cp) {
            this.wName=w; this.ownerName=o; this.ownerUuid=u; this.likes=l; this.visits=v; this.currentPlayers=cp;
        }
    }


    private void saveLastLocation(Player p, Location loc) {
        World w = loc.getWorld();
        if (w == null || isLobbyWorld(w.getName())) return;
        if (p.isDead() || p.getHealth() <= 0) return;
        if (loc.getY() <= -60) return;

        String internalName = w.getName();
        if (isAllWorld(internalName)) return;

        YamlConfiguration config = getPlayerConfig(p.getUniqueId());
        String worldName = getWorldNameByInternal(internalName);
        if (worldName != null) {
            String path = "locations." + worldName;
            config.set(path + ".x", loc.getX());
            config.set(path + ".y", loc.getY());
            config.set(path + ".z", loc.getZ());
            config.set(path + ".yaw", loc.getYaw());
            config.set(path + ".pitch", loc.getPitch());
            config.set(path + ".world_owner", getOwnerUuidByInternalWorldName(internalName).toString());
            savePlayerConfig(p.getUniqueId());
        }
    }

    private Location getLastLocation(UUID playerUuid, String worldName, World w) {
        YamlConfiguration config = getPlayerConfig(playerUuid);
        String path = "locations." + worldName;
        if (!config.contains(path) && config.contains("worlds." + worldName + ".lastLoc")) {
            path = "worlds." + worldName + ".lastLoc";
        }
        if (config.contains(path + ".x")) {
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw");
            float pitch = (float) config.getDouble(path + ".pitch");
            return new Location(w, x, y, z, yaw, pitch);
        }
        return null;
    }

    public Set<UUID> getWorldMembers(UUID owner, String wName) {
        if (owner.equals(SERVER_UUID)) return new HashSet<>();
        Set<UUID> members = new HashSet<>();
        YamlConfiguration config = getPlayerConfig(owner);
        ConfigurationSection sec = config.getConfigurationSection("worlds." + wName + ".members");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                UUID memberUuid = parseUuid(key);
                if (memberUuid != null) members.add(memberUuid);
            }
        }
        return members;
    }

    public void setSpecificPlayerGamode(UUID owner, String wName, UUID target, GameMode gm) {
        if (owner.equals(SERVER_UUID)) return;
        YamlConfiguration config = getPlayerConfig(owner);
        String path = "worlds." + wName + ".members." + target + ".gamemode";
        if (gm == null) config.set(path, null);
        else config.set(path, gm.name());
        savePlayerConfig(owner);
    }

    public GameMode getSpecificPlayerGamemode(UUID owner, String wName, UUID target) {
        if (owner.equals(SERVER_UUID)) return null;
        YamlConfiguration config = getPlayerConfig(owner);
        String path = "worlds." + wName + ".members." + target + ".gamemode";
        String gmStr = config.getString(path);
        if (gmStr != null) {
            return parseGameMode(gmStr, null);
        }
        return null;
    }

    public Material getGuiIcon(String key, Material defaultMat) {
        String s = plugin.getConfig().getString("gui." + key);
        if (s == null) return defaultMat;
        try { return Material.valueOf(s); } catch(Exception e) { return defaultMat; }
    }

    public void setGuiIcon(String key, Material mat) {
        plugin.getConfig().set("gui." + key, mat.name());
        plugin.saveConfig();
        if (key.equals("main.lobby")) {
            String lobbyName = plugin.getConfig().getString("lobby.worldName", "lobby");
            World lobby = Bukkit.getWorld(lobbyName);
            if (lobby != null) {
                setWorldIcon(SERVER_UUID, lobby.getName(), mat);
            }
        }
    }

    public Material getLobbyItemMaterial() {
        String s = plugin.getConfig().getString("lobby.item.material", "CLOCK");
        try { return Material.valueOf(s); } catch (Exception e) { return Material.CLOCK; }
    }

    @SuppressWarnings("unused")
    public void setLobbyItemMaterial(Material mat) {
        plugin.getConfig().set("lobby.item.material", mat.name());
        plugin.saveConfig();
    }

    @SuppressWarnings("unused")
    public void adminTeleport(Player p, String targetName, String worldName) {
        UUID targetUuid = findPlayerUuidByName(targetName);
        if (targetUuid != null) teleportToWorld(p, targetUuid, worldName);
        else p.sendMessage("§cPlayer not found.");
    }

    public List<String> getPrivateTemplates() {
        if (privateWorldsSourceCfg == null) return Collections.emptyList();
        ConfigurationSection sec = privateWorldsSourceCfg.getConfigurationSection("private-worlds");
        return sec != null ? new ArrayList<>(sec.getKeys(false)) : Collections.emptyList();
    }

    public boolean isTemplateWorld(UUID owner, String wName) {
        if (owner.equals(SERVER_UUID)) return false;
        return getPlayerConfig(owner).getBoolean("worlds." + wName + ".isTemplate", false);
    }
    public String getCreationType(UUID owner, String wName) {
        if (owner.equals(SERVER_UUID)) return "all-worlds";
        return getPlayerConfig(owner).getString("worlds." + wName + ".creationType", "vanilla");
    }

    public boolean isCreativeWorld(UUID owner, String wName) {
        String type = getCreationType(owner, wName);
        if (type == null) return false;
        return type.equalsIgnoreCase("creative") || type.equalsIgnoreCase("flat") || type.equalsIgnoreCase("void");
    }

    public boolean worldFolderExists(UUID owner, String wName) {
        if (owner.equals(SERVER_UUID)) return new File(plugin.getDataFolder(), "all-worlds/" + wName).exists();
        String name = Bukkit.getOfflinePlayer(owner).getName();
        if(name==null) name = getPlayerConfig(owner).getString("name", "Unknown");
        return new File(BASE_USER_WORLDS_PATH, name+"_"+owner+"/"+wName).exists();
    }
    public boolean isOwner(Player p, String internalWorldName) {
        if (isAllWorld(internalWorldName)) return false;
        UUID owner = getOwnerUuidByInternalWorldName(internalWorldName);
        return owner != null && owner.equals(p.getUniqueId());
    }
    public boolean hasWorld(UUID uuid, String wName) { return getPlayerConfig(uuid).contains("worlds." + wName); }

    public boolean isMember(UUID ownerUuid, String worldName, UUID targetUuid) {
        if (ownerUuid.equals(SERVER_UUID)) return false;
        String role = getPlayerConfig(ownerUuid).getString("worlds." + worldName + ".members." + targetUuid + ".role", "VISITOR");
        return role.equalsIgnoreCase("MEMBER") || role.equalsIgnoreCase("OWNER");
    }

    public UUID getOwnerUuidByInternalWorldName(String internalName) {
        if (isAllWorld(internalName) || isLobbyWorld(internalName)) return null;
        File f = new File(internalName);
        File p = f.getParentFile();
        if(p!=null && p.getName().contains("_")) {
            String[] parts = p.getName().split("_");
            return parseUuid(parts[parts.length-1]);
        }
        return null;
    }

    public String getWorldNameByInternal(String internalName) {
        if (isAllWorld(internalName)) return new File(internalName).getName();
        if (isLobbyWorld(internalName)) return internalName;
        return new File(internalName).getName();
    }

    public UUID findWorldOwnerIfMember(String wName, UUID myUuid) {
        File[] files = playersDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                if(!name.contains("-")) continue;
                try {
                    String ownerUuidStr = name.substring(name.lastIndexOf("-")+1, name.lastIndexOf("."));
                    if(ownerUuidStr.equals(myUuid.toString())) continue;
                    YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
                    if(conf.contains("worlds." + wName + ".members." + myUuid)) {
                        return UUID.fromString(ownerUuidStr);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse owner UUID from filename " + name + ": " + e.getMessage());
                }
            }
        }
        return null;
    }


    public UUID findPlayerUuidByName(String name) {
        name = stripLegacy(name);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p.getUniqueId();
            }
        }

        File[] files = playersDataFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith(name + "-")) {
                    return UUID.fromString(
                            f.getName().split("-")[1].replace(".yml", "")
                    );
                }
            }
        }
        return null;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                try {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // Best-effort copy; caller logs when needed
                }
            });
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
    public List<String> listPlayerWorlds(UUID uuid) { ConfigurationSection s = getPlayerConfig(uuid).getConfigurationSection("worlds"); return s == null ? new ArrayList<>() : new ArrayList<>(s.getKeys(false)); }

    public List<String> getAllWorlds() {
        List<String> all = new ArrayList<>();
        File[] files = playersDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if(files != null) {
            for(File f : files) {
                String name = f.getName();
                if(!name.contains("-")) continue;
                String ownerName = name.substring(0, name.lastIndexOf("-"));
                String uuidStr = name.substring(name.lastIndexOf("-")+1, name.lastIndexOf("."));

                YamlConfiguration c = YamlConfiguration.loadConfiguration(f);
                ConfigurationSection wSec = c.getConfigurationSection("worlds");
                if(wSec != null) {
                    for(String wName : wSec.getKeys(false)) {
                        all.add(ownerName + ":" + wName + ":" + uuidStr);
                    }
                }
            }
        }
        return all;
    }

    private void createSafePlatform(World w, int x, int y, int z) {
        w.getBlockAt(x, y-1, z).setType(Material.GLASS);
        w.getBlockAt(x, y+1, z).setType(Material.TORCH);
    }

    private void adjustSpawnToSafeLocation(World w) {
        Location spawn = w.getSpawnLocation();
        int highestY = w.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ());
        if (highestY < -60 || isUnsafe(w.getBlockAt(spawn.getBlockX(), highestY, spawn.getBlockZ()))) {
            if (!w.setSpawnLocation(spawn.getBlockX(), 65, spawn.getBlockZ())) {
                plugin.getLogger().warning("Failed to set safe spawn location for " + w.getName());
            }
            createSafePlatform(w, spawn.getBlockX(), 65, spawn.getBlockZ());
        } else {
            if (!w.setSpawnLocation(spawn.getBlockX(), highestY + 1, spawn.getBlockZ())) {
                plugin.getLogger().warning("Failed to set spawn location for " + w.getName());
            }
        }
    }

    private boolean isUnsafe(Block feet) {
        Block below = feet.getRelative(BlockFace.DOWN);
        return feet.isLiquid() || below.isLiquid() || below.getType() == Material.MAGMA_BLOCK || below.getType() == Material.CACTUS || below.getType() == Material.AIR;
    }

    public void updatePlayerInventory(Player p) {
        if (!plugin.getConfig().getBoolean("lobby.item.giveOnJoin", true)) {
            removeMenuItems(p);
            return;
        }
        Material mat = getLobbyItemMaterial();
        String scope = plugin.getConfig().getString("lobby.item.scope", "LOBBY_ONLY");
        int slot = plugin.getConfig().getInt("lobby.item.slot", 8);
        if (slot < 0) return;

        if (scope.equals("LOBBY_ONLY")) {
            if (isLobbyWorld(p.getWorld().getName())) {
                removeMenuItems(p);
                giveItem(p, mat, slot);
            } else {
                removeMenuItems(p);
            }
        } else {
            removeMenuItems(p);
            giveItem(p, mat, slot);
        }
    }

    private void removeMenuItems(Player p) {
        Material mat = getLobbyItemMaterial();
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() == mat) {
                if (item.hasItemMeta() && plainDisplayName(item.getItemMeta()).contains("Main Menu")) {
                    p.getInventory().setItem(i, null);
                }
            }
        }
    }

    public List<WorldInfo> getPublicWorlds(String sortBy) {
        List<WorldInfo> list = new ArrayList<>();

        // 1. Listar archivos de jugadores
        File[] files = playersDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File f : files) {
                // Parsear Nombre-UUID.yml
                String fileName = f.getName().replace(".yml", "");
                if(!fileName.contains("-")) continue;

                UUID u;
                try {
                    // Extraer UUID (Ãºltimos 36 caracteres)
                    if (fileName.length() <= 36) continue;
                    String uuidStr = fileName.substring(fileName.length() - 36);
                    u = UUID.fromString(uuidStr);
                } catch (Exception e) { continue; }

                // Cargar config del jugador
                YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
                ConfigurationSection wSec = conf.getConfigurationSection("worlds");
                if(wSec == null) continue;

                String ownerName = conf.getString("name", "Unknown");

                for(String wName : wSec.getKeys(false)) {
                    // --- PASO 1: ENCONTRAR LA CARPETA FÃSICA ---
                    File worldFolder = null;

                    // A) Probar ruta guardada en config
                    String internalPath = conf.getString("worlds." + wName + ".internal");
                    if (internalPath != null) {
                        File t = new File(internalPath);
                        if (t.exists()) worldFolder = t;
                    }

                    // B) Probar ruta construida estÃ¡ndar (Fallback)
                    if (worldFolder == null) {
                        String userFolder = ownerName + "_" + u;
                        File t = new File(BASE_USER_WORLDS_PATH, userFolder + "/" + wName);
                        if (t.exists()) worldFolder = t;
                    }

                    // Si no encontramos la carpeta fÃ­sica, el mundo no existe -> saltar
                    if (worldFolder == null) continue;

                    // --- PASO 2: LEER SETTING 'PUBLIC' DIRECTAMENTE ---
                    // Leemos el world-settings.yml dentro de la carpeta encontrada
                    boolean isPublic = true; // Por defecto true si no existe archivo (visible)
                    File settingsFile = new File(worldFolder, "world-settings.yml");

                    if (settingsFile.exists()) {
                        YamlConfiguration wConfig = YamlConfiguration.loadConfiguration(settingsFile);
                        // Ruta dentro del yml: worlds.<NombreMundo>.settings.public
                        if (wConfig.contains("worlds." + wName + ".settings.public")) {
                            isPublic = wConfig.getBoolean("worlds." + wName + ".settings.public");
                        }
                    }

                    // Si no es pÃºblico, lo saltamos
                    if (!isPublic) continue;

                    // --- PASO 3: RECOPILAR DATOS ---
                    int l = conf.getStringList("worlds." + wName + ".stats.likes").size();
                    int v = conf.getInt("worlds." + wName + ".stats.visits", 0);

                    // Calcular jugadores online de forma segura
                    int cp = 0;
                    // Intentar obtener el mundo si ya estÃ¡ cargado en Bukkit
                    World loadedWorld = Bukkit.getWorld(worldFolder.getPath().replace("\\", "/")); // Por ruta
                    if (loadedWorld == null) loadedWorld = Bukkit.getWorld(wName); // Por nombre simple

                    if (loadedWorld != null) {
                        cp = loadedWorld.getPlayers().size();
                    }

                    list.add(new WorldInfo(wName, ownerName, u, l, v, cp));
                }
            }
        }

        // Ordenar lista
        list.sort((a, b) -> {
            switch (sortBy.toLowerCase()) {
                case "likes": return Integer.compare(b.likes, a.likes);
                case "visits": return Integer.compare(b.visits, a.visits);
                case "type":
                    String typeA = getCreationType(a.ownerUuid, a.wName);
                    String typeB = getCreationType(b.ownerUuid, b.wName);
                    return (typeA == null ? "" : typeA).compareTo(typeB == null ? "" : typeB);
                default: return Integer.compare(b.currentPlayers, a.currentPlayers);
            }
        });
        return list;
    }

    private void giveItem(Player p, Material mat, int slot) {
        ItemStack clock = new ItemStack(mat);
        ItemMeta meta = clock.getItemMeta();
        meta.displayName(c("§a§lMain Menu §7(Right Click)"));
        meta.lore(List.of(c("§7Open AltWorlds Menu")));
        clock.setItemMeta(meta);
        p.getInventory().setItem(slot, clock);
    }

    public void setTemplateIcon(String templateKey, Material icon) {
        // Accedemos a la config de templates cargada en memoria
        YamlConfiguration tplConfig = templates.getConfig();
        tplConfig.set("templates." + templateKey + ".icon", icon.name());

        // Guardar en disco (data/settings/templates.yml)
        try {
            tplConfig.save(new File(settingsDataFolder, "templates.yml"));
            // Recargar para actualizar cachÃ©s
            reloadTemplates();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save templates.yml: " + e.getMessage());
        }
    }

    // MÃ©todo nuevo para Admin: All Worlds
    public List<WorldInfo> getAllWorldsInfo(String sortBy) {
        List<WorldInfo> list = new ArrayList<>();

        // 1. Escanear mundos de jugadores
        File[] files = playersDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                String fileName = f.getName();
                if(!fileName.contains("-")) continue;

                try {
                    String uuidStr = fileName.substring(fileName.lastIndexOf("-")+1, fileName.lastIndexOf("."));
                    UUID u = UUID.fromString(uuidStr);

                    YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
                    ConfigurationSection wSec = conf.getConfigurationSection("worlds");
                    if(wSec == null) continue;

                    String ownerName = conf.getString("name", "Unknown");

                    for(String wName : wSec.getKeys(false)) {
                        // AQUÃ ESTÃ LA DIFERENCIA: NO filtramos por "public".
                        // Lo mostramos TODO porque es para admins.

                        String internalPath = conf.getString("worlds." + wName + ".internal");
                        boolean exists = false;

                        if (internalPath != null && new File(internalPath).exists()) {
                            exists = true;
                        } else {
                            String userFolder = ownerName + "_" + u;
                            if (new File(BASE_USER_WORLDS_PATH, userFolder + "/" + wName).exists()) exists = true;
                        }

                        if (!exists) continue;

                        int l = conf.getStringList("worlds." + wName + ".stats.likes").size();
                        int v = conf.getInt("worlds." + wName + ".stats.visits", 0);

                        World w = Bukkit.getWorld(wName);
                        if (w == null && internalPath != null) w = Bukkit.getWorld(internalPath.replace("\\", "/"));
                        int cp = (w != null) ? w.getPlayers().size() : 0;

                        list.add(new WorldInfo(wName, ownerName, u, l, v, cp));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse world data for " + f.getName() + ": " + e.getMessage());
                }
            }
        }

        // 2. Ordenar
        list.sort((a, b) -> {
            switch (sortBy.toLowerCase()) {
                case "likes": return Integer.compare(b.likes, a.likes);
                case "visits": return Integer.compare(b.visits, a.visits);
                case "type":
                    String typeA = getCreationType(a.ownerUuid, a.wName);
                    String typeB = getCreationType(b.ownerUuid, b.wName);
                    return (typeA == null ? "" : typeA).compareTo(typeB == null ? "" : typeB);
                default: return Integer.compare(b.currentPlayers, a.currentPlayers);
            }
        });
        return list;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Material mat = getLobbyItemMaterial();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() != null && e.getItem().getType() == mat) {
            if (e.getItem().hasItemMeta() && plainDisplayName(e.getItem().getItemMeta()).contains("Main Menu")) {
                e.setCancelled(true);
                plugin.getGui().openMain(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Material mat = getLobbyItemMaterial();
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (dropped.getType() == mat && dropped.hasItemMeta() && plainDisplayName(dropped.getItemMeta()).contains("Main Menu")) {
            String scope = plugin.getConfig().getString("lobby.item.scope", "LOBBY_ONLY");
            e.setCancelled(scope.equals("EVERYWHERE") || isLobbyWorld(e.getPlayer().getWorld().getName()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (Boolean.TRUE.equals(e.getBlock().getWorld().getGameRuleValue(GameRule.DO_TILE_DROPS))) {
            e.setDropItems(true);
        }
    }


    @EventHandler public void onInventoryClick(InventoryClickEvent e) { if (isLobbyWorld(e.getWhoClicked().getWorld().getName()) && e.getWhoClicked().getGameMode() != GameMode.CREATIVE) e.setCancelled(true); }
}



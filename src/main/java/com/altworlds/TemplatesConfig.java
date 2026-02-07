package com.altworlds;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TemplatesConfig {
    private final Map<String, TemplateData> templatesData = new HashMap<>();
    private final List<String> validFolders = new ArrayList<>();
    private YamlConfiguration config;

    public static class TemplateData {
        String displayName;
        Material icon;
    }

    public static TemplatesConfig load(AltWorldsPlugin plugin) {
        TemplatesConfig tc = new TemplatesConfig();

        // 1. Carpetas fÃ­sicas de plantillas (schematics/mundos) siguen en /plugins/AltWorlds/templates
        File templatesDir = new File(plugin.getDataFolder(), "templates");
        if (!templatesDir.exists() && !templatesDir.mkdirs()) {
            plugin.getLogger().warning("Could not create templates folder at " + templatesDir.getPath());
        }

        // 2. Archivo de configuraciÃ³n visual (YML) ahora en /plugins/AltWorlds/data/settings
        File settingsDir = new File(plugin.getDataFolder(), "data/settings");
        if (!settingsDir.exists() && !settingsDir.mkdirs()) {
            plugin.getLogger().warning("Could not create settings folder at " + settingsDir.getPath());
        }

        File ymlFile = new File(settingsDir, "templates.yml");

        if (!ymlFile.exists()) {
            try {
                if (!ymlFile.createNewFile()) {
                    plugin.getLogger().warning("Could not create data/settings/templates.yml at " + ymlFile.getPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data/settings/templates.yml");
            }
        }

        tc.config = YamlConfiguration.loadConfiguration(ymlFile);

        // 3. Detectar carpetas fÃ­sicas reales
        File[] files = templatesDir.listFiles(File::isDirectory);
        Set<String> physicalFolders = new HashSet<>();
        if (files != null) {
            for (File f : files) {
                physicalFolders.add(f.getName());
            }
        }

        boolean changesMade = false;

        // 4. Sincronizar con YML
        for (String folderName : physicalFolders) {
            tc.validFolders.add(folderName);

            // Si es una carpeta nueva que no estaba en el YML, la registramos
            if (!tc.config.contains("templates." + folderName)) {
                String path = "templates." + folderName;
                tc.config.set(path + ".name", "&a" + capitalize(folderName));
                tc.config.set(path + ".icon", getRandomIcon().name());

                // Inyectar settings por defecto
                tc.config.createSection(path + ".settings", AltWorldsService.getDefaultSettingsMap());

                changesMade = true;

                TemplateData data = new TemplateData();
                data.displayName = "§a" + capitalize(folderName);
                data.icon = Material.PAPER;
                tc.templatesData.put(folderName, data);
            }
        }

        // 5. Cargar datos a memoria
        ConfigurationSection templatesSection = tc.config.getConfigurationSection("templates");
        if (templatesSection != null) {
            for (String key : templatesSection.getKeys(false)) {
                if (!physicalFolders.contains(key)) continue;

                TemplateData data = new TemplateData();
                data.displayName = tc.config.getString("templates." + key + ".name", key).replace("&", "§");
                String iconName = tc.config.getString("templates." + key + ".icon", "PAPER");
                try {
                    data.icon = Material.valueOf(iconName);
                } catch (Exception e) {
                    data.icon = Material.PAPER;
                }
                tc.templatesData.put(key, data);
            }
        }

        if (changesMade) {
            try {
                tc.config.save(ymlFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save templates.yml!");
            }
        }

        return tc;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static Material getRandomIcon() {
        Material[] opts = {Material.GRASS_BLOCK, Material.DIRT, Material.STONE, Material.SAND, Material.OAK_LOG, Material.BRICKS, Material.MOSSY_COBBLESTONE};
        return opts[new Random().nextInt(opts.length)];
    }

    public List<String> getTemplateFolders() { return validFolders; }
    public YamlConfiguration getConfig() { return config; }

    public String getDisplayName(String folder) {
        return templatesData.containsKey(folder) ? templatesData.get(folder).displayName : folder;
    }

    public Material getIcon(String folder) {
        return templatesData.containsKey(folder) ? templatesData.get(folder).icon : Material.PAPER;
    }

    public File resolveTemplateFolder(AltWorldsPlugin plugin, String folderName) {
        return new File(plugin.getDataFolder(), "templates/" + folderName);
    }
}
